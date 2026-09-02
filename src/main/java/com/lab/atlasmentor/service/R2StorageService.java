package com.lab.atlasmentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Thin wrapper around the R2-pointed S3Client (see R2StorageConfig) - the one place
 * that actually talks to Cloudflare R2. Shared by both attachment contexts via
 * TaskAttachmentUploadService; nothing here knows or cares whether the caller is the
 * proof section or a comment.
 *
 * S3Client is injected via ObjectProvider rather than directly: R2StorageConfig's
 * bean construction validates the R2 credentials aren't blank, and this service is
 * itself an eager singleton (constructor-injected into TaskAttachmentUploadService,
 * which TaskController needs at startup) - resolving S3Client eagerly here would
 * crash application startup whenever R2 isn't configured yet, rather than failing
 * only on the first real upload attempt as intended.
 */
@Slf4j
@Service
public class R2StorageService {

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final String bucketName;
    private final String accountId;
    private final String publicBaseUrl;

    public R2StorageService(
            ObjectProvider<S3Client> s3ClientProvider,
            @Value("${r2.bucket-name}") String bucketName,
            @Value("${r2.account-id}") String accountId,
            @Value("${r2.public-base-url:}") String publicBaseUrl) {
        this.s3ClientProvider = s3ClientProvider;
        this.bucketName = bucketName;
        this.accountId = accountId;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * Uploads content to R2 under {@code key} and returns the URL to store in
     * task_attachments.file_url.
     */
    public String upload(String key, byte[] content, String contentType) {
        log.info("Uploading {} bytes to R2 bucket '{}' at key '{}'", content.length, bucketName, key);
        s3ClientProvider.getObject().putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
        return resolveUrl(key);
    }

    private String resolveUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/" + key;
        }
        // Fallback: the raw R2 S3 endpoint URL. This only resolves publicly if the
        // bucket has public access (or a custom domain) turned on in the Cloudflare
        // dashboard - r2.public-base-url should be set to a real public/custom domain
        // in any environment where task_attachments.file_url needs to be fetchable by
        // the frontend directly.
        log.warn("r2.public-base-url is not configured - falling back to the raw R2 endpoint URL for key '{}'. "
                + "This only resolves publicly if the bucket has public access or a custom domain enabled.", key);
        return "https://" + bucketName + "." + accountId + ".r2.cloudflarestorage.com/" + key;
    }
}
