package com.pw.docvault.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.DocumentException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpDocumentProcessingClientTest {

    private HttpServer httpServer;
    private HttpDocumentProcessingClient client;

    @BeforeEach
    void setup() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.start();

        client = new HttpDocumentProcessingClient(HttpClient.newHttpClient(), new ObjectMapper());
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:" + httpServer.getAddress().getPort());
        ReflectionTestUtils.setField(client, "readTimeoutSeconds", 30);
    }

    @AfterEach
    void tearDown() {
        httpServer.stop(0);
    }

    @Test
    void processFromSignedUrlStreamsNdjsonFragments() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestContentType = new AtomicReference<>();
        AtomicReference<String> requestAccept = new AtomicReference<>();
        AtomicReference<String> requestProtocol = new AtomicReference<>();

        httpServer.createContext("/process", exchange -> respond(
                exchange,
                requestBody,
                requestContentType,
                requestAccept,
                requestProtocol,
                200,
                """
                {"fragmentOrder":0,"content":"chunk one","embedding":[0.1,0.2]}
                {"fragmentOrder":1,"content":"chunk two","embedding":[0.3,0.4]}
                """
        ));

        List<DocumentFragment> fragments = new ArrayList<>();

        client.processFromSignedUrl("https://signed", "application/pdf", fragments::add);

        assertThat(fragments).hasSize(2);
        assertThat(fragments.get(0).getFragmentOrder()).isEqualTo(0);
        assertThat(fragments.get(0).getContent()).isEqualTo("chunk one");
        assertThat(fragments.get(0).getEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(fragments.get(1).getFragmentOrder()).isEqualTo(1);
        assertThat(requestProtocol.get()).isEqualTo("HTTP/1.1");
        assertThat(requestContentType.get()).isEqualTo("application/json; charset=UTF-8");
        assertThat(requestAccept.get()).isEqualTo("application/x-ndjson");
        assertThat(requestBody.get()).isEqualTo("{\"signedUrl\":\"https://signed\",\"mimeType\":\"application/pdf\"}");
    }

    @Test
    void processFromSignedUrlThrowsWhenProcessorReturnsError() {
        httpServer.createContext("/process", exchange -> respond(
                exchange,
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                502,
                "processor failed"
        ));

        assertThrows(DocumentException.class,
                () -> client.processFromSignedUrl("https://signed", "application/pdf", fragment -> {
                }));
    }

    private void respond(HttpExchange exchange,
                         AtomicReference<String> requestBody,
                         AtomicReference<String> requestContentType,
                         AtomicReference<String> requestAccept,
                         AtomicReference<String> requestProtocol,
                         int statusCode,
                         String body) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        requestAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
        requestProtocol.set(exchange.getProtocol());

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
