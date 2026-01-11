package com.pw.docvault.service.document;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleCloudStorageService {

    private final Storage storage;

    @Value(value = "${app.gcs.bucket.name}")
    private String bucketName;

    @Value(value = "${app.gsc.buffer.storage.space}")
    public Integer bufferSpace;

    public GoogleCloudStorageService(@Value(value = "${app.gcs.credentials.location}")
                                     Resource credentials) throws IOException {
        this.storage = StorageOptions.newBuilder()
                .setCredentials(
                        ServiceAccountCredentials.fromStream(credentials.getInputStream())
                )
                .build()
                .getService();
    }

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

    public void delete(String objectName) {
        storage.delete(BlobId.of(bucketName, objectName));
    }

    public InputStream download(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);

        Blob blob = storage.get(blobId);
        if (blob == null) {
            throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "GCS object not found: " + objectName);
        }

        ReadChannel reader = blob.reader();
        return Channels.newInputStream(reader);
    }
}
