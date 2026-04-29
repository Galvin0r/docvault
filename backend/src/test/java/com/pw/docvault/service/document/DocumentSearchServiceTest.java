package com.pw.docvault.service.document;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.never;
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
        assertThat(page.getContent().getFirst().highlightedContentSnippet()).isEqualTo("hello <mark>world</mark>");

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(DocumentFragment.class));
        assertThat(captor.getValue().getQuery().toString()).contains("PUBLIC");
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
    void vectorSearchIsExplicitlyReserved() {
        assertThrows(BadRequestException.class, () -> documentSearchService.search(
                DocumentSearchMode.VECTOR,
                "policy",
                null,
                null,
                null,
                null,
                DocumentSearchScope.ACCESSIBLE,
                PageRequest.of(0, 10)
        ));

        verify(elasticsearchOperations, never()).search(org.mockito.ArgumentMatchers.any(NativeQuery.class), eq(DocumentFragment.class));
    }

    private DocumentFragment fragment() {
        DocumentFragment fragment = new DocumentFragment();
        fragment.setDocumentId(42L);
        fragment.setFragmentOrder(3);
        fragment.setTitle("Policy");
        fragment.setContent("A long policy document about world access.");
        fragment.setCreatedAt(Instant.parse("2026-04-11T10:15:30Z"));
        fragment.setOwnerId(5L);
        fragment.setOwnerLogin("alice");
        fragment.setVisibility(DocumentVisibility.PUBLIC);
        return fragment;
    }
}
