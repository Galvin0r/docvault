package com.pw.docvault.integration;

import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentAccessSyncIT extends AbstractDocumentIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void directUserAccessGrantAndRevokeSynchronizesSearchAcl() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");

        var document = documents.createDocument(alice, "Confidential Direct Roadmap", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-01T10:00:00Z"));
        documents.indexFragment(document, "direct confidential keyword");
        documents.refreshIndex();

        assertSearchTotal(bob, "direct", "ACCESSIBLE", 0);

        documents.mockMvc().perform(put("/document/%d/access/users/%d".formatted(document.getId(), bob.getId()))
                        .with(user(alice)))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertSearchTotal(bob, "direct", "ACCESSIBLE", 1);

        documents.mockMvc().perform(get("/document/%d/access".formatted(document.getId())).with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(bob.getId()));

        documents.mockMvc().perform(delete("/document/%d/access/users/%d".formatted(document.getId(), bob.getId()))
                        .with(user(alice)))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertSearchTotal(bob, "direct", "ACCESSIBLE", 0);
    }

    @Test
    void groupAccessGrantAndRevokeSynchronizesSearchAcl() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var team = documents.createGroup("Team");
        documents.createMembership(alice, team);
        documents.createMembership(bob, team);

        var document = documents.createDocument(alice, "Confidential Team Roadmap", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-01T10:00:00Z"));
        documents.indexFragment(document, "confidential roadmap only for the team");
        documents.refreshIndex();

        assertSearchTotal(bob, "confidential", "ACCESSIBLE", 0);

        documents.mockMvc().perform(put("/document/%d/access/groups/%d".formatted(document.getId(), team.getId()))
                        .with(user(alice)))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertSearchTotal(bob, "confidential", "ACCESSIBLE", 1);

        documents.mockMvc().perform(delete("/document/%d/access/groups/%d".formatted(document.getId(), team.getId()))
                        .with(user(alice)))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertSearchTotal(bob, "confidential", "ACCESSIBLE", 0);
    }

    @Test
    void visibilityUpdateSynchronizesAnonymousSearchAccessInBothDirections() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createDocument(alice, "Visibility Switch", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-15T10:00:00Z"));
        documents.indexFragment(document, "visibility keyword");
        documents.refreshIndex();

        assertAnonymousSearchTotal("visibility", 0);

        documents.mockMvc().perform(patch("/document/%d/visibility".formatted(document.getId()))
                        .with(user(alice))
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertAnonymousSearchTotal("visibility", 1);

        documents.mockMvc().perform(patch("/document/%d/visibility".formatted(document.getId()))
                        .with(user(alice))
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isNoContent());
        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertAnonymousSearchTotal("visibility", 0);
    }

    @Test
    void ownerCannotGrantAccessToSelf() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createDocument(alice, "Self Share", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-20T10:00:00Z"));

        documents.mockMvc().perform(put("/document/%d/access/users/%d".formatted(document.getId(), alice.getId()))
                        .with(user(alice)))
                .andExpect(status().isBadRequest());
    }

    private void assertSearchTotal(org.springframework.security.core.userdetails.UserDetails user,
                                   String content, String scope, int total) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> documents.mockMvc().perform(get("/document/search")
                        .with(user(user))
                        .param("content", content)
                        .param("scope", scope))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(total)));
    }

    private void assertAnonymousSearchTotal(String content, int total) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> documents.mockMvc().perform(get("/document/search")
                        .param("content", content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(total)));
    }
}