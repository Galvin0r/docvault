package com.pw.docvault.integration;

import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentSearchIT extends AbstractDocumentIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void keywordSearchMatchesRealDocumentTextAndHighlightsTitleAndContent() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");

        var retentionGuide = documents.createDocument(bob, "Quarterly Security Retention Guide",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-02-05T09:00:00Z"));
        var auditChecklist = documents.createDocument(bob, "Audit Checklist",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-02-06T09:00:00Z"));
        var recipe = documents.createDocument(bob, "Kitchen Notes",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-02-07T09:00:00Z"));
        var privateMatch = documents.createDocument(bob, "Private Retention Memo",
                DocumentVisibility.PRIVATE, DocumentStatus.INDEXED, Instant.parse("2026-02-08T09:00:00Z"));

        documents.indexFragment(retentionGuide, """
                The quarterly security retention policy requires encrypted backups, audit evidence,
                and a written deletion schedule for expired contracts.
                """);
        documents.indexFragment(auditChecklist, """
                Before the audit, confirm that every security report links to the retention schedule
                and names the responsible document owner.
                """);
        documents.indexFragment(recipe, """
                This note talks about sourdough starter hydration and oven temperature.
                """);
        documents.indexFragment(privateMatch, """
                The private retention policy draft is not shared with Alice yet.
                """);
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "retention policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].contentSnippet", not(containsString("sourdough"))))
                .andExpect(jsonPath("$.content[0].highlightedContentSnippet", containsString("<mark>")));

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "Quarterly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].documentId").value(retentionGuide.getId()))
                .andExpect(jsonPath("$.content[0].highlightedTitle", containsString("<mark>")));
    }

    @Test
    void anonymousKeywordSearchOnlyReturnsPublicDocumentsEvenWhenFiltersMatchPrivateOnes() throws Exception {
        var bob = documents.createUser("bob");

        var publicDoc = documents.createDocument(bob, "Public Incident Response",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-03-01T10:00:00Z"));
        var privateDoc = documents.createDocument(bob, "Private Incident Response",
                DocumentVisibility.PRIVATE, DocumentStatus.INDEXED, Instant.parse("2026-03-02T10:00:00Z"));

        documents.indexFragment(publicDoc, "incident response playbook with public communication contacts");
        documents.indexFragment(privateDoc, "incident response playbook with attorney privilege notes");
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .param("content", "incident response")
                        .param("author", "BOB")
                        .param("uploadedFrom", "2026-03-01T00:00:00Z")
                        .param("uploadedTo", "2026-03-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].documentId").value(publicDoc.getId()));

        documents.mockMvc().perform(get("/document/search")
                        .param("content", "attorney privilege")
                        .param("scope", "ACCESSIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void keywordSearchHonorsAclScopesAndCombinedMetadataFilters() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var charlie = documents.createUser("charlie");
        var research = documents.createGroup("Research");
        documents.createMembership(alice, research);

        var own = documents.createDocument(alice, "Alpha Owner Notes", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-01-10T10:00:00Z"));
        var publicDoc = documents.createDocument(bob, "Alpha Public Report", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2026-01-20T10:00:00Z"));
        var direct = documents.createDocument(bob, "Alpha Direct Memo", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-02-10T10:00:00Z"));
        var group = documents.createDocument(bob, "Alpha Group Plan", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-03-10T10:00:00Z"));
        var hidden = documents.createDocument(bob, "Alpha Hidden Draft", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-04-10T10:00:00Z"));

        documents.grantUser(direct, alice);
        documents.grantGroup(group, research);
        documents.indexFragment(own, "alpha owner content");
        documents.indexFragment(publicDoc, "alpha public content");
        documents.indexFragment(direct, "alpha direct content", List.of(alice.getId()), List.of());
        documents.indexFragment(group, "alpha group content", List.of(), List.of(research.getId()));
        documents.indexFragment(hidden, "alpha hidden content");
        documents.refreshIndex();

        assertSearchTotal(alice, "alpha", "ACCESSIBLE", 4);
        assertSearchTotal(alice, "alpha", "PUBLIC", 1);
        assertSearchTotal(alice, "alpha", "OWNED_BY_ME", 1);
        assertSearchTotal(alice, "alpha", "SHARED_WITH_ME", 2);
        assertSearchTotal(charlie, "alpha", "ACCESSIBLE", 1);

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "alpha")
                        .param("author", "bob")
                        .param("title", "Direct")
                        .param("uploadedFrom", "2026-02-01T00:00:00Z")
                        .param("uploadedTo", "2026-02-28T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].documentId").value(direct.getId()))
                .andExpect(jsonPath("$.content[0].highlightedContentSnippet", containsString("<mark>")));
    }

    @Test
    void keywordSearchTreatsContentTitleAuthorAndDatesAsIntersectionFilters() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");
        var carol = documents.createUser("carol");

        var target = documents.createDocument(bob, "Migration Runbook Final",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-10T12:00:00Z"));
        var wrongTitle = documents.createDocument(bob, "Migration Draft Notes",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-11T12:00:00Z"));
        var wrongAuthor = documents.createDocument(carol, "Migration Runbook Final",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-12T12:00:00Z"));
        var wrongDate = documents.createDocument(bob, "Migration Runbook Final",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-05-01T12:00:00Z"));

        documents.indexFragment(target, "blue green deployment checklist with rollback validation");
        documents.indexFragment(wrongTitle, "blue green deployment checklist with rollback validation");
        documents.indexFragment(wrongAuthor, "blue green deployment checklist with rollback validation");
        documents.indexFragment(wrongDate, "blue green deployment checklist with rollback validation");
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "rollback validation")
                        .param("title", "Final")
                        .param("author", "BOB")
                        .param("uploadedFrom", "2026-04-01T00:00:00Z")
                        .param("uploadedTo", "2026-04-30T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].documentId").value(target.getId()));

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "rollback validation")
                        .param("title", "Final")
                        .param("author", "BOB")
                        .param("uploadedFrom", "2026-04-01T00:00:00Z")
                        .param("uploadedTo", "2026-04-09T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void keywordSearchSupportsIndependentMetadataFiltersAndPagination() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");

        var first = documents.createDocument(bob, "Zeta Architecture", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2026-01-10T10:00:00Z"));
        var second = documents.createDocument(bob, "Zeta Budget", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2026-02-10T10:00:00Z"));
        var third = documents.createDocument(alice, "Zeta Roadmap", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-03-10T10:00:00Z"));
        var outsideRange = documents.createDocument(bob, "Zeta Old Notes", DocumentVisibility.PUBLIC,
                DocumentStatus.INDEXED, Instant.parse("2025-12-10T10:00:00Z"));
        documents.indexFragment(first, "zeta architecture content");
        documents.indexFragment(second, "zeta budget content");
        documents.indexFragment(third, "zeta roadmap content");
        documents.indexFragment(outsideRange, "zeta old content");
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("title", "Budget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].documentId").value(second.getId()));

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("author", "bob")
                        .param("uploadedFrom", "2026-01-01T00:00:00Z")
                        .param("uploadedTo", "2026-02-28T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "zeta")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void keywordSearchReturnsMatchingFragmentsForMultiFragmentDocument() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createDocument(alice, "Multi Fragment Manual", DocumentVisibility.PRIVATE,
                DocumentStatus.INDEXED, Instant.parse("2026-02-15T10:00:00Z"));
        documents.indexFragment(document, 0, "alpha fragment has the shared keyword", List.of(), List.of());
        documents.indexFragment(document, 1, "beta fragment has the shared keyword", List.of(), List.of());
        documents.indexFragment(document, 2, "gamma fragment has unrelated text", List.of(), List.of());
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "shared keyword")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].documentId", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(document.getId().intValue()))))
                .andExpect(jsonPath("$.content[*].fragmentOrder", org.hamcrest.Matchers.containsInAnyOrder(0, 1)));
    }

    @Test
    void vectorSearchRanksByEmbeddingSimilarityAndHonorsFiltersAndAcl() throws Exception {
        var alice = documents.createUser("alice");
        var bob = documents.createUser("bob");

        var matching = documents.createDocument(bob, "Security Controls Manual",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-01T10:00:00Z"));
        var weakerMatch = documents.createDocument(bob, "Security Controls Reference",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-02T10:00:00Z"));
        var hidden = documents.createDocument(bob, "Security Controls Private",
                DocumentVisibility.PRIVATE, DocumentStatus.INDEXED, Instant.parse("2026-04-03T10:00:00Z"));
        var wrongTitle = documents.createDocument(bob, "Kitchen Controls Manual",
                DocumentVisibility.PUBLIC, DocumentStatus.INDEXED, Instant.parse("2026-04-04T10:00:00Z"));

        when(documentProcessingClient.embedText("semantic controls")).thenReturn(unitVector(0));

        documents.indexFragment(matching, 0, "encryption key rotation and incident containment",
                List.of(), List.of(), unitVector(0));
        documents.indexFragment(weakerMatch, 0, "security policy summary",
                List.of(), List.of(), mixedVector(0, 1));
        documents.indexFragment(hidden, 0, "private security controls",
                List.of(), List.of(), unitVector(0));
        documents.indexFragment(wrongTitle, 0, "security controls with unrelated title",
                List.of(), List.of(), unitVector(0));
        documents.refreshIndex();

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("mode", "VECTOR")
                        .param("content", "semantic controls")
                        .param("title", "Security")
                        .param("author", "BOB")
                        .param("uploadedFrom", "2026-04-01T00:00:00Z")
                        .param("uploadedTo", "2026-04-30T23:59:59Z")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].documentId").value(matching.getId()))
                .andExpect(jsonPath("$.content[1].documentId").value(weakerMatch.getId()))
                .andExpect(jsonPath("$.content[*].documentId", not(org.hamcrest.Matchers.hasItem(hidden.getId().intValue()))))
                .andExpect(jsonPath("$.content[*].documentId", not(org.hamcrest.Matchers.hasItem(wrongTitle.getId().intValue()))));
    }

    private void assertSearchTotal(User user, String content, String scope, int total) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> documents.mockMvc().perform(get("/document/search")
                        .with(user(user))
                        .param("content", content)
                        .param("scope", scope))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(total)));
    }

    private float[] unitVector(int dimension) {
        float[] vector = new float[384];
        vector[dimension] = 1.0f;
        return vector;
    }

    private float[] mixedVector(int firstDimension, int secondDimension) {
        float[] vector = new float[384];
        vector[firstDimension] = 0.7f;
        vector[secondDimension] = 0.7f;
        return vector;
    }
}