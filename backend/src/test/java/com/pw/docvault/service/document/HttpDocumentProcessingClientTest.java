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
        httpServer.createContext("/process", exchange -> respond(
                exchange,
                200,
                """
                {"fragmentOrder":0,"content":"chunk one","embedding":[0.1,0.2]}
                {"fragmentOrder":1,"content":"chunk two","embedding":[0.3,0.4]}
                """,
                "application/x-ndjson"
        ));

        var fragments = new ArrayList<DocumentFragment>();

        client.processFromSignedUrl("https://signed", "application/pdf", fragments::add);

        assertThat(fragments).hasSize(2);
        assertThat(fragments.getFirst().getFragmentOrder()).isZero();
        assertThat(fragments.getFirst().getContent()).isEqualTo("chunk one");
        assertThat(fragments.getFirst().getEmbedding()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void embedTextPostsQueryTextAndReturnsEmbedding() {
        var requestBody = new AtomicReference<String>();
        var requestAccept = new AtomicReference<String>();
        httpServer.createContext("/embed", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
            respond(exchange, 200, "{\"embedding\":[0.4,0.5,0.6]}", "application/json");
        });

        float[] embedding = client.embedText("semantic query");

        assertThat(embedding).containsExactly(0.4f, 0.5f, 0.6f);
        assertThat(requestAccept.get()).isEqualTo("application/json");
        assertThat(requestBody.get()).isEqualTo("{\"text\":\"semantic query\"}");
    }

    @Test
    void embedTextThrowsWhenProcessorReturnsError() {
        httpServer.createContext("/embed", exchange -> respond(exchange, 502, "processor failed", "text/plain"));

        assertThrows(DocumentException.class, () -> client.embedText("semantic query"));
    }

    @Test
    void embedTextRejectsBlankTextBeforeCallingProcessor() {
        assertThrows(DocumentException.class, () -> client.embedText(" "));
    }

    private void respond(HttpExchange exchange, int statusCode, String body, String responseContentType)
            throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", responseContentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}