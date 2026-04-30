package com.pw.docvault.integration;

import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentListingIT extends AbstractDocumentIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void listDocumentsIncludesOnlyUserReadableNonDraftNonDeletingDocuments() throws Exception {
        var alice = documents.createUser("alice");

        documents.createDocument(alice, "Visible Uploaded", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADED, Instant.parse("2026-07-01T10:00:00Z"));
        documents.createDocument(alice, "Visible Indexing", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXING, Instant.parse("2026-07-02T10:00:00Z"));
        documents.createDocument(alice, "Visible Indexed", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-07-03T10:00:00Z"));
        documents.createDocument(alice, "Visible Index Failed", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEX_FAILED, Instant.parse("2026-07-04T10:00:00Z"));
        documents.createDocument(alice, "Hidden Uploading", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADING, Instant.parse("2026-07-05T10:00:00Z"));
        documents.createDocument(alice, "Hidden Deleting", DocumentVisibility.PRIVATE,
                DocumentStatus.DELETING, Instant.parse("2026-07-06T10:00:00Z"));
        documents.createDocument(alice, "Hidden Delete Failed", DocumentVisibility.PRIVATE,
                DocumentStatus.DELETE_FAILED, Instant.parse("2026-07-07T10:00:00Z"));

        documents.mockMvc().perform(get("/document")
                        .with(user(alice))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder(
                        "Visible Uploaded",
                        "Visible Indexing",
                        "Visible Indexed",
                        "Visible Index Failed"
                )))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Hidden Uploading"))))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Hidden Deleting"))))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Hidden Delete Failed"))));
    }

    @Test
    void listDocumentsDoesNotDuplicateDocumentsReachedThroughSeveralAccessPaths() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var team = documents.createGroup("Team");
        documents.createMembership(alice, team);
        documents.createMembership(bob, team);

        var document = documents.createDocument(bob, "Shared Through Every Path", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2026-07-10T10:00:00Z"));
        documents.grantUser(document, alice);
        documents.grantGroup(document, team);

        documents.mockMvc().perform(get("/document")
                        .with(user(alice))
                        .param("titleSearch", "Shared Through Every Path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(document.getId()));
    }

    @Test
    void listDocumentsReturnsActiveJobInfoForOwnedDocuments() throws Exception {
        var alice = documents.createUser("alice");

        var owned = documents.createDocument(alice, "Owned With Pending Job", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADED, Instant.parse("2026-07-11T10:00:00Z"));
        documents.createJob(owned, com.pw.docvault.model.enums.DocumentSyncOperation.INDEX_CONTENT);

        documents.mockMvc().perform(get("/document")
                        .with(user(alice))
                        .param("titleSearch", "Owned With Pending Job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(owned.getId()))
                .andExpect(jsonPath("$.content[0].attempts").value(0));
    }
}
