package com.pw.docvault.integration;

import com.google.cloud.storage.Storage;
import com.pw.docvault.service.EmailService;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import com.pw.docvault.service.document.DocumentProcessingClient;
import com.pw.docvault.service.document.GoogleCloudStorageService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractWebIntegrationIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @MockitoBean
    protected Storage storage;

    @MockitoBean
    protected GoogleCloudStorageService googleCloudStorageService;

    @MockitoBean
    protected DocumentProcessingClient documentProcessingClient;

    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected DocumentFragmentRepository documentFragmentRepository;

    @MockitoBean
    protected ElasticsearchOperations elasticsearchOperations;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
