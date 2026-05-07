package com.pw.docvault.integration;

import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentSecurityIT extends AbstractDocumentIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void anonymousCanDownloadPublicDocumentsButNotPrivateDocuments() throws Exception {
        var alice = documents.createUser("alice");
        var publicDocument = documents.createIndexedDocument(alice, "Public Manual", DocumentVisibility.PUBLIC,
                Instant.parse("2026-05-01T10:00:00Z"));
        var privateDocument = documents.createIndexedDocument(alice, "Private Manual", DocumentVisibility.PRIVATE,
                Instant.parse("2026-05-02T10:00:00Z"));

        when(googleCloudStorageService.generateGetSignedUrl(anyString(), anyString()))
                .thenReturn("https://storage.example/public-download");

        documents.mockMvc().perform(get("/document/download/%d".formatted(publicDocument.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("https://storage.example/public-download"));

        documents.mockMvc().perform(get("/document/download/%d".formatted(privateDocument.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUsersCannotUseProtectedDocumentEndpoints() throws Exception {
        documents.mockMvc().perform(post("/document/draft")
                        .param("title", "Draft")
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isUnauthorized());

        documents.mockMvc().perform(get("/document"))
                .andExpect(status().isUnauthorized());

        documents.mockMvc().perform(delete("/document/delete/1"))
                .andExpect(status().isUnauthorized());

        documents.mockMvc().perform(put("/document/1/access/users/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonOwnersCannotManageDocumentsOrAccessLists() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var team = documents.createGroup("Team");
        documents.createMembership(bob, team);

        var uploadingDocument = documents.createDocument(alice, "Uploading Secret", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADING, Instant.parse("2026-05-03T10:00:00Z"));
        var indexedDocument = documents.createIndexedDocument(alice, "Indexed Secret", DocumentVisibility.PRIVATE,
                Instant.parse("2026-05-04T10:00:00Z"));

        documents.mockMvc().perform(post("/document/%d/sign-upload".formatted(uploadingDocument.getId()))
                        .with(user(bob))
                        .param("contentType", "text/plain")
                        .param("originalFilename", "secret.txt"))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(post("/document/%d/complete-upload".formatted(uploadingDocument.getId()))
                        .with(user(bob)))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(patch("/document/%d/visibility".formatted(indexedDocument.getId()))
                        .with(user(bob))
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(get("/document/%d/access".formatted(indexedDocument.getId()))
                        .with(user(bob)))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(put("/document/%d/access/users/%d".formatted(indexedDocument.getId(), bob.getId()))
                        .with(user(bob)))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(put("/document/%d/access/groups/%d".formatted(indexedDocument.getId(), team.getId()))
                        .with(user(bob)))
                .andExpect(status().isForbidden());

        documents.mockMvc().perform(delete("/document/delete/%d".formatted(indexedDocument.getId()))
                        .with(user(bob)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotShareDocumentWithGroupTheyDoNotBelongTo() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var team = documents.createGroup("Team");
        documents.createMembership(bob, team);
        var document = documents.createIndexedDocument(alice, "Owner Outside Team", DocumentVisibility.PRIVATE,
                Instant.parse("2026-05-05T10:00:00Z"));

        documents.mockMvc().perform(put("/document/%d/access/groups/%d".formatted(document.getId(), team.getId()))
                        .with(user(alice)))
                .andExpect(status().isForbidden());
    }
}