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
import java.util.Arrays;
import java.util.Map;
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

        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption[].class)))
                .thenReturn(url);

        String result = gcsService.generatePutSignedUrl(objectName, contentType);

        assertThat(result).isEqualTo(url.toString());
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<Storage.SignUrlOption[]> optionsCaptor = ArgumentCaptor.forClass(Storage.SignUrlOption[].class);

        verify(storage).signUrl(blobInfoCaptor.capture(), eq(15L), eq(TimeUnit.MINUTES), optionsCaptor.capture());

        BlobInfo blobInfo = blobInfoCaptor.getValue();
        assertThat(blobInfo.getBlobId().getName()).isEqualTo(objectName);
        assertThat(blobInfo.getContentType()).isEqualTo(contentType);
        assertThat(optionsCaptor.getValue()).hasSize(3);
    }

    @Test
    void generateGetSignedUrlCallsStorage() throws Exception {
        String objectName = "download.txt";
        URL url = new URL("https://get-url.com");

        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption[].class)))
                .thenReturn(url);

        String result = gcsService.generateGetSignedUrl(objectName);

        assertThat(result).isEqualTo(url.toString());
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<Storage.SignUrlOption[]> optionsCaptor = ArgumentCaptor.forClass(Storage.SignUrlOption[].class);

        verify(storage).signUrl(blobInfoCaptor.capture(), eq(15L), eq(TimeUnit.MINUTES), optionsCaptor.capture());

        assertThat(blobInfoCaptor.getValue().getBlobId().getName()).isEqualTo(objectName);
        assertThat(optionsCaptor.getValue()).hasSize(2);
        assertThat(extractQueryParams(optionsCaptor.getValue())).isNull();
    }

    @Test
    void generateGetSignedUrlWithDownloadFilenameAddsContentDisposition() throws Exception {
        String objectName = "user_1/uuid-object";
        String downloadFilename = "report final.pdf";
        URL url = new URL("https://get-url.com");

        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption[].class)))
                .thenReturn(url);

        String result = gcsService.generateGetSignedUrl(objectName, downloadFilename);

        assertThat(result).isEqualTo(url.toString());

        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<Storage.SignUrlOption[]> optionsCaptor = ArgumentCaptor.forClass(Storage.SignUrlOption[].class);

        verify(storage).signUrl(blobInfoCaptor.capture(), eq(15L), eq(TimeUnit.MINUTES), optionsCaptor.capture());

        assertThat(blobInfoCaptor.getValue().getBlobId().getName()).isEqualTo(objectName);
        Map<String, String> queryParams = extractQueryParams(optionsCaptor.getValue());
        assertThat(queryParams).isNotNull();
        assertThat(queryParams).containsKey("response-content-disposition");
        assertThat(queryParams.get("response-content-disposition")).contains("attachment");
        assertThat(queryParams.get("response-content-disposition")).contains("filename*=");
        assertThat(queryParams.get("response-content-disposition")).contains("report%20final.pdf");
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

    @SuppressWarnings("unchecked")
    private Map<String, String> extractQueryParams(Storage.SignUrlOption[] options) {
        return Arrays.stream(options)
                .map(option -> ReflectionTestUtils.invokeMethod(option, "getValue"))
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, String>) value)
                .findFirst()
                .orElse(null);
    }
}