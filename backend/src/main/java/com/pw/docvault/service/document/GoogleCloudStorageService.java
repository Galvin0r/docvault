package com.pw.docvault.service.document;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class GoogleCloudStorageService {

    private final Storage storage;

    @Value(value = "${app.gcs.bucket.name}")
    private String bucketName;

    public Blob getMetadata(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        return storage.get(blobId);
    }

    public String generatePutSignedUrl(String objectName, String contentType) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                .setContentType(contentType)
                .build();

        return storage.signUrl(
                blobInfo,
                15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.withContentType()
        ).toString();
    }

    public String generateGetSignedUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

        return storage.signUrl(
                blobInfo,
                15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
        ).toString();
    }

    public String generateGetSignedUrl(String objectName, String downloadFilename) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();
        var contentDisposition = ContentDisposition.attachment()
                .filename(resolveDownloadFilename(objectName, downloadFilename), StandardCharsets.UTF_8)
                .build()
                .toString();

        return storage.signUrl(
                blobInfo,
                15, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.withQueryParams(Map.of(
                        "response-content-disposition",
                        contentDisposition
                ))
        ).toString();
    }

    private String resolveDownloadFilename(String objectName, String downloadFilename) {
        if (downloadFilename != null && !downloadFilename.isBlank()) {
            return downloadFilename;
        }
        int lastSlashIndex = objectName.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < objectName.length() - 1) {
            return objectName.substring(lastSlashIndex + 1);
        }
        if (!objectName.isBlank()) {
            return objectName;
        }
        return "download";
    }

    public void delete(String objectName) {
        storage.delete(BlobId.of(bucketName, objectName));
    }

    public InputStream download(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);

        Blob blob = storage.get(blobId);
        if (blob == null) {
            throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "GCS object not found: " + objectName);
        }

        ReadChannel reader = blob.reader();
        return Channels.newInputStream(reader);
    }
}