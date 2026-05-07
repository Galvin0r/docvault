package com.pw.docvault.service.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.DocumentException;
import com.pw.docvault.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "app.processing.mode", havingValue = "http")
public class HttpDocumentProcessingClient implements DocumentProcessingClient {

    private static final String NDJSON_MEDIA_TYPE = "application/x-ndjson";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.processing.http.base-url}")
    private String baseUrl;

    @Value("${app.processing.http.read-timeout-seconds}")
    private int readTimeoutSeconds;

    @Override
    public void processFromSignedUrl(String signedUrl, String mimeType, Consumer<DocumentFragment> fragmentConsumer) {
        HttpRequest request = buildRequest(signedUrl, mimeType);

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new DocumentException(
                            ErrorCode.DOCUMENT_PROCESSING_FAILED,
                            "Processing service returned status %d: %s".formatted(
                                    response.statusCode(),
                                    readRemainingBody(reader)));
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    fragmentConsumer.accept(objectMapper.readValue(line, DocumentFragment.class));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Processing request interrupted", ex);
        } catch (IOException ex) {
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Processing request failed", ex);
        }
    }

    @Override
    public float[] embedText(String text) {
        if (text == null || text.isBlank()) {
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Embedding text must not be blank");
        }

        HttpRequest request = buildEmbeddingRequest(text);
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String responseBody = new String(body.readAllBytes(), StandardCharsets.UTF_8).trim();
                    throw new DocumentException(
                            ErrorCode.DOCUMENT_PROCESSING_FAILED,
                            "Processing service returned status %d while embedding query: %s".formatted(
                                    response.statusCode(),
                                    responseBody));
                }
                return objectMapper.readValue(body, EmbeddingResponse.class).embedding();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Embedding request interrupted", ex);
        } catch (IOException ex) {
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Embedding request failed", ex);
        }
    }

    private HttpRequest buildRequest(String signedUrl, String mimeType) {
        try {
            String payload = objectMapper.writeValueAsString(new ProcessingRequest(signedUrl, mimeType));
            return HttpRequest.newBuilder(processUri())
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(readTimeoutSeconds))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", NDJSON_MEDIA_TYPE)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
        } catch (IOException ex) {
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Failed to build processing request", ex);
        }
    }

    private HttpRequest buildEmbeddingRequest(String text) {
        try {
            String payload = objectMapper.writeValueAsString(new EmbeddingRequest(text));
            return HttpRequest.newBuilder(embedUri())
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(readTimeoutSeconds))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
        } catch (IOException ex) {
            throw new DocumentException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "Failed to build embedding request", ex);
        }
    }

    private URI processUri() {
        return URI.create(normalizedBaseUrl() + "/process");
    }

    private URI embedUri() {
        return URI.create(normalizedBaseUrl() + "/embed");
    }

    private String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String readRemainingBody(BufferedReader reader) {
        try {
            var sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (IOException ex) {
            return "Unable to read error response";
        }
    }

    private record ProcessingRequest( @JsonProperty("signedUrl") String signedUrl,
                                      @JsonProperty("mimeType") String mimeType) {
    }

    private record EmbeddingRequest( @JsonProperty("text") String text) {
    }

    private record EmbeddingResponse( @JsonProperty("embedding") float[] embedding) {
    }
}