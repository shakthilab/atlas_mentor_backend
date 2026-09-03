package com.lab.atlasmentor.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * delete()/keyFromUrl() round-trip the same key-encoding upload()/resolveUrl() use -
 * exercised here with both public-base-url and raw-R2-endpoint URLs, since deleting a
 * comment (see TaskService#deleteComment) needs to turn a stored file_url back into
 * the R2 key that was actually uploaded under.
 */
class R2StorageServiceTest {

    private S3Client s3Client;
    private ObjectProvider<S3Client> s3ClientProvider;

    private R2StorageService newService(String publicBaseUrl) {
        s3Client = mock(S3Client.class);
        s3ClientProvider = mock(ObjectProvider.class);
        when(s3ClientProvider.getObject()).thenReturn(s3Client);
        return new R2StorageService(s3ClientProvider, "my-bucket", "acct123", publicBaseUrl);
    }

    @Test
    void deletesByKeyDerivedFromAPublicBaseUrl() {
        R2StorageService service = newService("https://cdn.example.com");

        service.delete("https://cdn.example.com/tasks/1/abc-note.m4a");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEqualsKeyAndBucket(captor.getValue(), "tasks/1/abc-note.m4a", "my-bucket");
    }

    @Test
    void deletesByKeyDerivedFromTheRawR2EndpointUrl() {
        R2StorageService service = newService("");

        service.delete("https://my-bucket.acct123.r2.cloudflarestorage.com/tasks/1/abc-note.m4a");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEqualsKeyAndBucket(captor.getValue(), "tasks/1/abc-note.m4a", "my-bucket");
    }

    @Test
    void aUrlFromOutsideThisBucketIsLeftAlone() {
        // e.g. an attachment registered via the older "just record a URL a client
        // already uploaded elsewhere" path (TaskService#addAttachment) - never ours to
        // delete.
        R2StorageService service = newService("https://cdn.example.com");

        service.delete("https://some-other-host.example.net/file.jpg");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void aNullUrlIsLeftAlone() {
        R2StorageService service = newService("https://cdn.example.com");

        service.delete(null);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    private void assertEqualsKeyAndBucket(DeleteObjectRequest request, String expectedKey, String expectedBucket) {
        org.junit.jupiter.api.Assertions.assertEquals(expectedKey, request.key());
        org.junit.jupiter.api.Assertions.assertEquals(expectedBucket, request.bucket());
    }
}
