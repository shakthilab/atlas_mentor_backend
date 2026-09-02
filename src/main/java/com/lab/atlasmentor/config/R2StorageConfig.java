package com.lab.atlasmentor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Cloudflare R2 is S3-compatible, so we point the plain AWS SDK v2 S3 client at R2's
 * S3-compatible endpoint (https://&lt;account-id&gt;.r2.cloudflarestorage.com) instead of
 * pulling in a separate R2-specific SDK. Backs R2StorageService, the shared upload
 * target for both the proof-section and comment-section attachment flows.
 *
 * r2.account-id / r2.access-key-id / r2.secret-access-key are expected to be set via
 * environment variables (R2_ACCOUNT_ID etc. - see application.properties) in any real
 * environment; left blank here only means "not configured yet," not "safe to run
 * without" - {@code @Lazy} defers actually building the client (which validates the
 * credentials aren't blank) until the first real upload attempt, rather than crashing
 * application startup entirely when R2 isn't configured yet.
 */
@Configuration
public class R2StorageConfig {

    @Bean
    @Lazy
    public S3Client r2S3Client(
            @Value("${r2.account-id}") String accountId,
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey) {

        return S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                // R2 doesn't use AWS regions; "auto" is Cloudflare's documented placeholder -
                // the SDK still requires some Region value to be set.
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
