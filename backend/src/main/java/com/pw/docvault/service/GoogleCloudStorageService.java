package com.pw.docvault.service;

import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.UUID;

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

    public String upload(InputStream inputStream, String fileName, Long userId, String contentType)
            throws IOException {
        String uniqueId = UUID.randomUUID().toString();
        String objectName = String.format("user_%d/%s_%s", userId, fileName, uniqueId);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[bufferSpace];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int limit;
            while ((limit = inputStream.read(buffer)) >= 0) {
                byteBuffer.clear();
                byteBuffer.limit(limit);
                while (byteBuffer.hasRemaining()) {
                    writer.write(byteBuffer);
                }
            }
        }

        return objectName;
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
