package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.LeadImportRawRow;
import com.lab.atlasmentor.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Part 2 Path B — the "smart link" endpoint's detection + fetch logic. The employee pastes
 * a Google Sheets share URL; this class converts it into the CSV export fetch and hands off
 * to {@link LeadImportParseService} — same CSV parser direct .csv uploads use.
 *
 * Google Sheets only. Every non-Sheets link — Google Drive-hosted Excel files, personal
 * OneDrive, SharePoint/OneDrive-for-Business — was tried and dropped: none of them expose a
 * stable, anonymous raw-bytes URL a backend HTTP client can rely on (OneDrive/SharePoint in
 * particular route through session/JS-driven viewers with no durable direct-download form,
 * and reverse-engineering that isn't worth the fragility). An employee with a file from any
 * of those sources should download it and use "Import via file" instead.
 *
 * SSRF note: the only outbound fetch target here is a fixed literal host we construct
 * ourselves (docs.google.com/spreadsheets/.../export) — the raw user-supplied URL is only
 * ever used to extract the sheet id/gid via regex, never fetched as-is, so this never turns
 * into an arbitrary-host fetch on attacker-supplied input.
 */
@Service
public class LeadLinkResolverService {

    private static final String UNRECOGNIZED_LINK_ERROR =
            "This link isn't publicly accessible, or isn't a recognized Google Sheets link. " +
            "Please set sharing to 'anyone with the link' or upload the file directly instead.";

    // Generous for a lead spreadsheet, bounded so one request can't exhaust memory.
    private static final long MAX_RESPONSE_BYTES = 15L * 1024 * 1024;

    private static final Pattern SPREADSHEET_ID_PATTERN = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID_PATTERN = Pattern.compile("[#?&]gid=(\\d+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final LeadImportParseService parseService;

    public LeadLinkResolverService(LeadImportParseService parseService) {
        this.parseService = parseService;
    }

    public List<LeadImportRawRow> resolveAndParse(String urlString) {
        URI uri = parseUri(urlString);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (host.equals("docs.google.com") && uri.getPath() != null && uri.getPath().contains("/spreadsheets/")) {
            return fetchGoogleSheet(urlString);
        }
        throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
    }

    private URI parseUri(String urlString) {
        try {
            URI uri = new URI(urlString);
            if (uri.getScheme() == null || !uri.getScheme().startsWith("http") || uri.getHost() == null) {
                throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
            }
            return uri;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
        }
    }

    private List<LeadImportRawRow> fetchGoogleSheet(String urlString) {
        Matcher idMatcher = SPREADSHEET_ID_PATTERN.matcher(urlString);
        if (!idMatcher.find()) {
            throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
        }
        String spreadsheetId = idMatcher.group(1);
        Matcher gidMatcher = GID_PATTERN.matcher(urlString);
        String gid = gidMatcher.find() ? gidMatcher.group(1) : null;

        String exportUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/export?format=csv"
                + (gid != null ? "&gid=" + gid : "");

        FetchResult result = fetch(exportUrl);
        assertNotHtml(result);
        return parseService.parseCsv(new InputStreamReader(new ByteArrayInputStream(result.body()), StandardCharsets.UTF_8));
    }

    private FetchResult fetch(String urlString) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(urlString))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (compatible; AtlasMentorLeadImport/1.0)")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
            }
            if (body.length > MAX_RESPONSE_BYTES) {
                throw new BusinessException("The linked file is too large to import (max 15 MB). Please upload a smaller file directly.");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            return new FetchResult(body, contentType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
        }
    }

    /**
     * A private/inaccessible sheet typically comes back as a 200 with an HTML login/sharing
     * page instead of the real content — catch that here regardless of what Content-Type the
     * server claims.
     */
    private void assertNotHtml(FetchResult result) {
        String contentType = result.contentType().toLowerCase();
        if (contentType.contains("text/html")) {
            throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
        }
        byte[] body = result.body();
        int i = 0;
        while (i < body.length && Character.isWhitespace(body[i])) {
            i++;
        }
        if (i < body.length && body[i] == '<') {
            throw new BusinessException(UNRECOGNIZED_LINK_ERROR);
        }
    }

    private record FetchResult(byte[] body, String contentType) {
    }
}
