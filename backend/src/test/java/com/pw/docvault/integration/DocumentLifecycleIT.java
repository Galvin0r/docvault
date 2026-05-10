package com.pw.docvault.integration;

import com.google.cloud.storage.Blob;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentLifecycleIT extends AbstractSearchIntegrationIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void documentLifecycleIndexesSearchesDownloadsAndDeletes() throws Exception {
        var alice = documents.createUser("alice");
        var uploadedBlob = mock(Blob.class);
        when(uploadedBlob.exists()).thenReturn(true);
        when(uploadedBlob.getSize()).thenReturn(128L);
        when(googleCloudStorageService.generatePutSignedUrl(anyString(), anyString()))
                .thenReturn("https://storage.example/upload");
        when(googleCloudStorageService.getMetadata(anyString())).thenReturn(uploadedBlob);
        when(googleCloudStorageService.generateGetSignedUrl(anyString()))
                .thenReturn("https://storage.example/source");
        when(googleCloudStorageService.generateGetSignedUrl(anyString(), anyString()))
                .thenReturn("https://storage.example/download");
        documents.processorReturns("Quarterly roadmap contains the integration keyword.");

        var documentId = documents.mockMvc().perform(post("/document/draft")
                        .with(user(alice))
                        .param("title", "Quarterly Roadmap")
                        .param("description", "planning")
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        documents.mockMvc().perform(post("/document/%s/sign-upload".formatted(documentId))
                        .with(user(alice))
                        .param("contentType", "text/plain")
                        .param("originalFilename", "roadmap.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://storage.example/upload"));

        documents.mockMvc().perform(post("/document/%s/complete-upload".formatted(documentId)).with(user(alice)))
                .andExpect(status().isCreated());

        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document").with(user(alice)).param("ownedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("INDEXED"));

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "roadmap")
                        .param("scope", "ACCESSIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        documents.mockMvc().perform(get("/document/search").param("content", "roadmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        documents.mockMvc().perform(get("/document/download/%s".formatted(documentId)).with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string("https://storage.example/download"));

        documents.mockMvc().perform(delete("/document/delete/%s".formatted(documentId)).with(user(alice)))
                .andExpect(status().isNoContent());
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document").with(user(alice)).param("ownedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "roadmap")
                        .param("scope", "ACCESSIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(googleCloudStorageService).delete(anyString());
    }

    @Test
    void downloadHonorsOwnerPublicDirectGroupAndPrivateAccess() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var charlie = documents.createUser("charlie");
        var team = documents.createGroup("Team");
        documents.createMembership(bob, team);

        var privateDocument = documents.createIndexedDocument(alice, "Private Download", DocumentVisibility.PRIVATE,
                Instant.parse("2026-04-01T10:00:00Z"));
        var publicDocument = documents.createIndexedDocument(alice, "Public Download", DocumentVisibility.PUBLIC,
                Instant.parse("2026-04-02T10:00:00Z"));
        var directDocument = documents.createIndexedDocument(alice, "Direct Download", DocumentVisibility.PRIVATE,
                Instant.parse("2026-04-03T10:00:00Z"));
        var groupDocument = documents.createIndexedDocument(alice, "Group Download", DocumentVisibility.PRIVATE,
                Instant.parse("2026-04-04T10:00:00Z"));
        documents.grantUser(directDocument, bob);
        documents.grantGroup(groupDocument, team);

        when(googleCloudStorageService.generateGetSignedUrl(anyString(), anyString()))
                .thenReturn("https://storage.example/download");

        documents.mockMvc().perform(get("/document/download/%d".formatted(privateDocument.getId())).with(user(alice)))
                .andExpect(status().isOk());
        documents.mockMvc().perform(get("/document/download/%d".formatted(publicDocument.getId())).with(user(charlie)))
                .andExpect(status().isOk());
        documents.mockMvc().perform(get("/document/download/%d".formatted(directDocument.getId())).with(user(bob)))
                .andExpect(status().isOk());
        documents.mockMvc().perform(get("/document/download/%d".formatted(groupDocument.getId())).with(user(bob)))
                .andExpect(status().isOk());

        documents.mockMvc().perform(get("/document/download/%d".formatted(privateDocument.getId())).with(user(charlie)))
                .andExpect(status().isForbidden());
        documents.mockMvc().perform(get("/document/download/%d".formatted(directDocument.getId())).with(user(charlie)))
                .andExpect(status().isForbidden());
        documents.mockMvc().perform(get("/document/download/%d".formatted(groupDocument.getId())).with(user(charlie)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDocumentsHonorsVisibilityAccessFiltersAndPagination() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var team = documents.createGroup("Research");
        documents.createMembership(alice, team);

        documents.createDocument(alice, "Alpha Owned", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-01-10T10:00:00Z"));
        documents.createDocument(alice, "Beta Owned", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-02-10T10:00:00Z"));
        var publicDoc = documents.createDocument(bob, "Gamma Public", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2026-03-10T10:00:00Z"));
        var sharedDoc = documents.createDocument(bob, "Delta Shared", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-10T10:00:00Z"));
        var groupDoc = documents.createDocument(bob, "Epsilon Group", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-05-10T10:00:00Z"));
        documents.createDocument(bob, "Hidden Private", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-06-10T10:00:00Z"));
        documents.grantUser(sharedDoc, alice);
        documents.grantGroup(groupDoc, team);

        documents.mockMvc().perform(get("/document")
                        .with(user(alice))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(groupDoc.getId()))
                .andExpect(jsonPath("$.content[1].id").value(sharedDoc.getId()));

        documents.mockMvc().perform(get("/document").with(user(alice)).param("ownedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        documents.mockMvc().perform(get("/document")
                        .with(user(alice))
                        .param("ownerName", "bob")
                        .param("titleSearch", "Gamma")
                        .param("dateFrom", "2026-03-01T00:00:00Z")
                        .param("dateTo", "2026-03-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(publicDoc.getId()));

        documents.mockMvc().perform(get("/document").with(user(alice)).param("groupId", team.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(groupDoc.getId()));
    }
}
