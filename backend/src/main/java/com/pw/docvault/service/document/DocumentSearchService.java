package com.pw.docvault.service.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.model.document.DocumentSearchResultDto;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DocumentSearchService {

    private static final int SNIPPET_LENGTH = 220;
    private static final String HIGHLIGHT_PRE_TAG = "<mark>";
    private static final String HIGHLIGHT_POST_TAG = "</mark>";

    private final ElasticsearchOperations elasticsearchOperations;
    private final CurrentUserProvider currentUserProvider;
    private final GroupMembershipRepository groupMembershipRepository;

    public Page<DocumentSearchResultDto> search(DocumentSearchMode mode, String content, String title, String author,
                                                Instant uploadedFrom, Instant uploadedTo, DocumentSearchScope scope,
                                                Pageable pageable) {
        if (mode == DocumentSearchMode.VECTOR) {
            throw new BadRequestException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Vector search endpoint is reserved, but semantic search is not implemented yet."
            );
        }

        Optional<User> user = currentUserProvider.getOptional();
        List<Long> groupIds = user.map(value -> groupMembershipRepository.findAllByUserId(value.getId()).stream()
                                                                         .map(membership ->
                                                                                      membership.getGroup().getId())
                                                                         .toList())
                                  .orElseGet(List::of);

        var query = NativeQuery.builder()
                .withQuery(keywordQuery(content, title, author, uploadedFrom, uploadedTo,
                        scope == null ? DocumentSearchScope.ACCESSIBLE : scope, user, groupIds))
                .withPageable(pageable)
                .withTrackTotalHits(true)
                .withHighlightQuery(highlightQuery())
                .build();

        var hits = elasticsearchOperations.search(query, DocumentFragment.class);
        List<DocumentSearchResultDto> results = hits.getSearchHits().stream().map(this::toDto).toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private Query keywordQuery(String content, String title, String author, Instant uploadedFrom, Instant uploadedTo,
                               DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        if (hasText(content)) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                    .query(content.trim())
                    .fields("content^3", "title^2")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
            )));
        } else {
            must.add(Query.of(q -> q.matchAll(ma -> ma)));
        }

        if (hasText(title)) {
            filter.add(Query.of(q -> q.match(m -> m.field("title").query(title.trim()))));
        }
        if (hasText(author)) {
            filter.add(termQuery("ownerLogin", author.trim().toLowerCase()));
        }
        if (uploadedFrom != null || uploadedTo != null) {
            filter.add(Query.of(q -> q.range(r -> r.date(d -> {
                d.field("createdAt");
                if (uploadedFrom != null) {
                    d.gte(uploadedFrom.toString());
                }
                if (uploadedTo != null) {
                    d.lte(uploadedTo.toString());
                }
                return d;
            }))));
        }

        filter.add(accessQuery(scope, user, groupIds));

        return Query.of(q -> q.bool(b -> b.must(must).filter(filter)));
    }

    private Query accessQuery(DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        if (user.isEmpty()) {
            return publicQuery();
        }

        Long userId = user.get().getId();
        return switch (scope) {
            case PUBLIC -> publicQuery();
            case OWNED_BY_ME -> termQuery("ownerId", userId);
            case SHARED_WITH_ME -> sharedWithMeQuery(userId, groupIds);
            case ACCESSIBLE -> accessibleQuery(userId, groupIds);
        };
    }

    private Query accessibleQuery(Long userId, List<Long> groupIds) {
        List<Query> should = new ArrayList<>();
        should.add(publicQuery());
        should.add(termQuery("ownerId", userId));
        should.add(termQuery("permittedUserIds", userId));
        if (!groupIds.isEmpty()) {
            should.add(termsQuery("permittedGroupIds", groupIds));
        }

        return Query.of(q -> q.bool(b -> b.should(should).minimumShouldMatch("1")));
    }

    private Query sharedWithMeQuery(Long userId, List<Long> groupIds) {
        List<Query> should = new ArrayList<>();
        should.add(termQuery("permittedUserIds", userId));
        if (!groupIds.isEmpty()) {
            should.add(termsQuery("permittedGroupIds", groupIds));
        }

        return Query.of(q -> q.bool(b -> b
                .should(should)
                .minimumShouldMatch("1")
                .mustNot(termQuery("ownerId", userId))
        ));
    }

    private Query publicQuery() {
        return Query.of(q -> q.term(t -> t
                .field("visibility")
                .value(DocumentVisibility.PUBLIC.name())
        ));
    }

    private Query termQuery(String field, Long value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    private Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    private Query termsQuery(String field, List<Long> values) {
        return Query.of(q -> q.terms(t -> t
                .field(field)
                .terms(tf -> tf.value(values.stream().map(FieldValue::of).toList()))
        ));
    }

    private HighlightQuery highlightQuery() {
        var params = HighlightParameters.builder()
                .withPreTags(HIGHLIGHT_PRE_TAG)
                .withPostTags(HIGHLIGHT_POST_TAG)
                .withFragmentSize(SNIPPET_LENGTH)
                .withNumberOfFragments(1)
                .withRequireFieldMatch(false)
                .build();

        return new HighlightQuery(
                new Highlight(params, List.of(new HighlightField("content"), new HighlightField("title"))),
                DocumentFragment.class
        );
    }

    private DocumentSearchResultDto toDto(SearchHit<DocumentFragment> hit) {
        DocumentFragment fragment = hit.getContent();
        String highlightedTitle = highlightSnippet(hit, "title").orElse(fragment.getTitle());
        String highlightedContent = highlightSnippet(hit, "content").orElse(snippet(fragment.getContent()));

        return new DocumentSearchResultDto(
                fragment.getDocumentId(),
                fragment.getFragmentOrder(),
                fragment.getTitle(),
                highlightedTitle,
                snippet(fragment.getContent()),
                highlightedContent,
                fragment.getCreatedAt(),
                fragment.getOwnerId(),
                fragment.getOwnerLogin(),
                fragment.getVisibility(),
                hit.getScore()
        );
    }

    private Optional<String> highlightSnippet(SearchHit<DocumentFragment> hit, String field) {
        List<String> values = hit.getHighlightField(field);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
    }

    private String snippet(String value) {
        if (value == null || value.length() <= SNIPPET_LENGTH) {
            return value;
        }
        return value.substring(0, SNIPPET_LENGTH).stripTrailing() + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}