package com.pw.docvault.service.document;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private SearchHits<DocumentFragment> searchHits;

    @Mock
    private DocumentProcessingClient documentProcessingClient;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentSearchService documentSearchService;

    @Test
    void keywordSearchMapsHighlightedResults() {
        DocumentFragment fragment = fragment();
        var hit = new SearchHit<>(
                "doc_fragment",
                "fragment-1",
                null,
                7.5f,
                null,
                Map.of("content", List.of("hello <mark>world</mark>")),
                Map.of(),
                null,
                null,
                Map.of(),
                fragment
        );

        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(documentRepository.findAllById(List.of(42L))).thenReturn(List.of(documentMetadata()));

        var page = documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                "world",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().documentId()).isEqualTo(42L);
        assertThat(page.getContent().getFirst().originalFilename()).isEqualTo("policy.pdf");
        assertThat(page.getContent().getFirst().mimeType()).isEqualTo("application/pdf");
        assertThat(page.getContent().getFirst().size()).isEqualTo(8192L);
        assertThat(page.getContent().getFirst().highlightedContentSnippet()).isEqualTo("hello <mark>world</mark>");

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString()).contains("PUBLIC");
        assertThat(captor.getValue().getFieldCollapse().field()).isEqualTo("documentId");
        assertThat(captor.getValue().getAggregations()).containsKey("documentCount");
    }

    @Test
    void searchReturnsOneResultPerDocument() {
        DocumentFragment firstFragment = fragment();
        firstFragment.setFragmentOrder(1);
        firstFragment.setContent("First matching fragment.");

        DocumentFragment secondFragment = fragment();
        secondFragment.setFragmentOrder(2);
        secondFragment.setContent("Second matching fragment from the same document.");

        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(
                searchHit(firstFragment, 7.5f, Map.of("content", List.of("First <mark>matching</mark> fragment."))),
                searchHit(secondFragment, 6.0f, Map.of("content", List.of("Second <mark>matching</mark> fragment.")))
        ));
        doReturn(documentCountAggregation(1L)).when(searchHits).getAggregations();
        when(documentRepository.findAllById(List.of(42L))).thenReturn(List.of(documentMetadata()));

        var page = documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                "matching",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().fragmentOrder()).isEqualTo(1);
        assertThat(page.getContent().getFirst().highlightedContentSnippet())
                .isEqualTo("First <mark>matching</mark> fragment.");
    }

    @Test
    void authenticatedSearchIncludesGroupMembershipForAcl() {
        User user = new User();
        user.setId(5L);

        Group group = new Group();
        group.setId(9L);

        GroupMembership membership = new GroupMembership();
        membership.setGroup(group);

        when(currentUserProvider.getOptional()).thenReturn(Optional.of(user));
        when(groupMembershipRepository.findAllByUserId(5L)).thenReturn(List.of(membership));
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                "policy",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString()).contains("permittedGroupIds", "9");
    }

    @Test
    void titleFilterSupportsPartialPrefixes() {
        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                "fragme",
                "LL",
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString())
                .contains("match_bool_prefix", "match_phrase_prefix", "wildcard", "*ll*");
    }

    @Test
    void contentSearchSupportsPrefixesWithoutFuzzyMatches() {
        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                "intro",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString())
                .contains("best_fields", "bool_prefix", "phrase_prefix")
                .doesNotContain("fuzziness");
    }

    @Test
    void titleFilterHighlightsOnlyMatchedTitleSubstring() {
        DocumentFragment fragment = fragment();
        fragment.setTitle("D1_solar_clinic_backup_power");

        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(
                searchHit(fragment, 7.5f, Map.of("title", List.of("<mark>D1_solar_clinic_backup_power</mark>")))
        ));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(documentRepository.findAllById(List.of(42L))).thenReturn(List.of(documentMetadata()));

        var page = documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                null,
                "solar",
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent().getFirst().highlightedTitle())
                .isEqualTo("D1_<mark>solar</mark>_clinic_backup_power");
    }

    @Test
    void authorFilterSupportsPartialLogins() {
        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                null,
                null,
                "Rom",
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString())
                .contains("ownerLogin", "prefix", "wildcard", "rom", "*rom*");
        assertThat(captor.getValue().getHighlightQuery()).isEmpty();
    }

    @Test
    void dateFilterUsesIndexedFragmentDateRange() {
        Instant uploadedFrom = Instant.parse("2026-04-06T22:00:00Z");
        Instant uploadedTo = Instant.parse("2026-04-07T21:59:59.999Z");

        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.KEYWORD,
                null,
                null,
                null,
                uploadedFrom,
                uploadedTo,
                DocumentSearchScope.PUBLIC,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString())
                .contains("createdAt", String.valueOf(uploadedFrom.toEpochMilli()), String.valueOf(uploadedTo.toEpochMilli()))
                .doesNotContain("documentId");
    }

    @Test
    void vectorSearchBuildsScriptScoreQueryFromProcessorEmbedding() {
        when(currentUserProvider.getOptional()).thenReturn(Optional.empty());
        when(documentProcessingClient.embedText("policy")).thenReturn(unitVector(0));
        when(elasticsearchOperations.search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);

        documentSearchService.search(
                DocumentSearchMode.VECTOR,
                "policy",
                "Policy",
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        );

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString())
                .contains("script_score", "cosineSimilarity", "query_vector", "embedding", "PUBLIC", "Policy");
    }

    @Test
    void vectorSearchRequiresContentQuery() {
        assertThrows(BadRequestException.class, () -> documentSearchService.search(
                DocumentSearchMode.VECTOR,
                " ",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        ));
    }

    private DocumentFragment fragment() {
        DocumentFragment fragment = new DocumentFragment();
        fragment.setDocumentId(42L);
        fragment.setFragmentOrder(3);
        fragment.setTitle("Policy");
        fragment.setOriginalFilename("policy-from-index.txt");
        fragment.setMimeType("text/plain");
        fragment.setSizeBytes(1024L);
        fragment.setContent("A long policy document about world access.");
        fragment.setCreatedAt(Instant.parse("2026-04-11T10:15:30Z"));
        fragment.setOwnerId(5L);
        fragment.setOwnerLogin("alice");
        fragment.setVisibility(DocumentVisibility.PUBLIC);
        return fragment;
    }

    private Document documentMetadata() {
        Document document = new Document();
        document.setId(42L);
        document.setOriginalFilename("policy.pdf");
        document.setMimeType("application/pdf");
        document.setSizeBytes(8192L);
        return document;
    }

    private SearchHit<DocumentFragment> searchHit(DocumentFragment fragment, float score, Map<String, List<String>> highlights) {
        return new SearchHit<>(
                "doc_fragment",
                "fragment-" + fragment.getFragmentOrder(),
                null,
                score,
                null,
                highlights,
                Map.of(),
                null,
                null,
                Map.of(),
                fragment
        );
    }

    private ElasticsearchAggregations documentCountAggregation(long count) {
        return new ElasticsearchAggregations(Map.of(
                "documentCount",
                Aggregate.of(a -> a.cardinality(c -> c.value(count)))
        ));
    }

    private float[] unitVector(int dimension) {
        float[] vector = new float[384];
        vector[dimension] = 1.0f;
        return vector;
    }
}