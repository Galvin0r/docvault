package com.pw.docvault.integration;

import com.google.cloud.storage.Storage;
import com.pw.docvault.service.document.DocumentProcessingClient;
import com.pw.docvault.service.document.GoogleCloudStorageService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.testcontainers.containers.wait.strategy.Wait.forHttp;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractDocumentIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> ELASTICSEARCH = new GenericContainer<>(DockerImageName.parse("elasticsearch:9.2.3"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withExposedPorts(9200)
            .waitingFor(forHttp("/").forPort(9200).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));

    @MockitoBean
    protected Storage storage;

    @MockitoBean
    protected GoogleCloudStorageService googleCloudStorageService;

    @MockitoBean
    protected DocumentProcessingClient documentProcessingClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.elasticsearch.uris", AbstractDocumentIT::elasticsearchUrl);
    }

    private static String elasticsearchUrl() {
        return "http://%s:%d".formatted(ELASTICSEARCH.getHost(), ELASTICSEARCH.getMappedPort(9200));
    }
}
