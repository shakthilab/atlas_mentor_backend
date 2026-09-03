package com.lab.atlasmentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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

    /**
     * Deletes the object backing a previously-uploaded {@code file_url} (as returned by
     * {@link #upload}). Used when an attachment's owning row (comment or task) is
     * deleted, so the recording/photo/etc. doesn't linger in the bucket forever.
     *
     * Best-effort: an attachment registered via the older "just record a URL a client
     * already uploaded elsewhere" path (see TaskService#addAttachment) may point outside
     * this bucket entirely, so a URL that doesn't match either of our own key-encoding
     * schemes is left alone rather than risking deleting the wrong thing - it was never
     * ours to delete.
     */
    public void delete(String fileUrl) {
        String key = keyFromUrl(fileUrl);
        if (key == null) {
            log.warn("Could not resolve an R2 key from file URL '{}' - skipping delete (attachment may live outside this bucket).", fileUrl);
            return;
        }
        log.info("Deleting R2 object at key '{}' from bucket '{}'", key, bucketName);
        s3ClientProvider.getObject().deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
    }

    /** Inverse of {@link #resolveUrl} - null if fileUrl doesn't match either scheme it produces. */
    private String keyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            String prefix = base + "/";
            return fileUrl.startsWith(prefix) ? fileUrl.substring(prefix.length()) : null;
        }
        String rawPrefix = "https://" + bucketName + "." + accountId + ".r2.cloudflarestorage.com/";
        return fileUrl.startsWith(rawPrefix) ? fileUrl.substring(rawPrefix.length()) : null;
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
