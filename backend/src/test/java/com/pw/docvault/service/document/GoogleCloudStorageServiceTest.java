package com.pw.docvault.service.document;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.pw.docvault.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCloudStorageServiceTest {

    @Mock
    private Storage storage;

    @InjectMocks
    private GoogleCloudStorageService gcsService;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(gcsService, "bucketName", BUCKET_NAME);
    }

    @Test
    void getMetadataReturnsBlob() {
        String objectName = "file.txt";
        Blob blob = mock(Blob.class);
        when(storage.get(BlobId.of(BUCKET_NAME, objectName))).thenReturn(blob);

        Blob result = gcsService.getMetadata(objectName);

        assertThat(result).isEqualTo(blob);
    }

    @Test
    void generatePutSignedUrlCallsStorage() throws Exception {
        String objectName = "upload.txt";
        String contentType = "text/plain";
        URL url = new URL("https://signed-url.com");

        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(), any(), any()))
                .thenReturn(url);

        String result = gcsService.generatePutSignedUrl(objectName, contentType);

        assertThat(result).isEqualTo(url.toString());
        verify(storage).signUrl(
                argThat(info -> info.getBlobId().getName().equals(objectName) && info.getContentType().equals(contentType)),
                eq(15L), eq(TimeUnit.MINUTES), any(), any(), any()
        );
    }

    @Test
    void generateGetSignedUrlCallsStorage() throws Exception {
        String objectName = "download.txt";
        URL url = new URL("https://get-url.com");

        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(), any()))
                .thenReturn(url);

        String result = gcsService.generateGetSignedUrl(objectName);

        assertThat(result).isEqualTo(url.toString());
        verify(storage).signUrl(
                argThat(info -> info.getBlobId().getName().equals(objectName)),
                eq(15L), eq(TimeUnit.MINUTES), any(), any()
        );
    }

    @Test
    void deleteCallsStorage() {
        String objectName = "to-delete.txt";
        gcsService.delete(objectName);
        verify(storage).delete(BlobId.of(BUCKET_NAME, objectName));
    }

    @Test
    void downloadReturnsInputStream() throws Exception {
        String objectName = "data.bin";
        Blob blob = mock(Blob.class);
        ReadChannel reader = mock(ReadChannel.class);
        
        when(storage.get(BlobId.of(BUCKET_NAME, objectName))).thenReturn(blob);
        when(blob.reader()).thenReturn(reader);

        InputStream result = gcsService.download(objectName);

        assertThat(result).isNotNull();
        verify(blob).reader();
    }

    @Test
    void downloadThrowsNotFoundIfBlobIsNull() {
        String objectName = "missing.bin";
        when(storage.get(BlobId.of(BUCKET_NAME, objectName))).thenReturn(null);

        assertThrows(NotFoundException.class, () -> gcsService.download(objectName));
    }
}
