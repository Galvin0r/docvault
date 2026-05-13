package com.pw.docvault.service.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.json.JsonData;
import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.model.document.DocumentSearchResultDto;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DocumentSearchService {

    private static final int SNIPPET_LENGTH = 220;
    private static final int DOCUMENT_COUNT_PRECISION_THRESHOLD = 40_000;
    private static final String DOCUMENT_COUNT_AGGREGATION = "documentCount";
    private static final String HIGHLIGHT_PRE_TAG = "<mark>";
    private static final String HIGHLIGHT_POST_TAG = "</mark>";

    private final ElasticsearchOperations elasticsearchOperations;
    private final CurrentUserProvider currentUserProvider;
    private final GroupMembershipRepository groupMembershipRepository;
    private final DocumentProcessingClient documentProcessingClient;
    private final DocumentRepository documentRepository;

    public Page<DocumentSearchResultDto> search(DocumentSearchMode mode, String content, String title, String author,
                                                Instant uploadedFrom, Instant uploadedTo, DocumentSearchScope scope,
                                                Pageable pageable) {
        Optional<User> user = currentUserProvider.getOptional();
        DocumentSearchMode effectiveMode = mode == null ? DocumentSearchMode.KEYWORD : mode;
        DocumentSearchScope effectiveScope = scope == null ? DocumentSearchScope.ACCESSIBLE : scope;
        List<Long> groupIds = groupIdsForSearch(user, effectiveScope);
        Query searchQuery = switch (effectiveMode) {
            case KEYWORD -> keywordQuery(content, title, author, uploadedFrom, uploadedTo, effectiveScope, user, groupIds);
            case VECTOR -> vectorQuery(content, title, author, uploadedFrom, uploadedTo, effectiveScope, user, groupIds);
        };

        var queryBuilder = NativeQuery.builder()
                .withQuery(searchQuery)
                .withPageable(pageable)
                .withFieldCollapse(FieldCollapse.of(c -> c.field("documentId")))
                .withAggregation(DOCUMENT_COUNT_AGGREGATION, documentCountAggregation());
        if (effectiveMode == DocumentSearchMode.KEYWORD && shouldHighlight(content, title)) {
            queryBuilder.withHighlightQuery(highlightQuery(hasText(content), hasText(title)));
        }

        var hits = elasticsearchOperations.search(queryBuilder.build(), DocumentFragment.class);
        var resultHits = distinctDocumentHits(hits.getSearchHits());
        Map<Long, Document> documentsById = findDocumentMetadata(resultHits);
        List<DocumentSearchResultDto> results = resultHits.stream()
                .map(hit -> toDto(hit, documentsById.get(hit.getContent().getDocumentId()), title))
                .toList();

        return new PageImpl<>(results, pageable, totalHits(hits, resultHits));
    }

    private List<Long> groupIdsForSearch(Optional<User> user, DocumentSearchScope scope) {
        if (user.isEmpty() || (scope != DocumentSearchScope.ACCESSIBLE && scope != DocumentSearchScope.SHARED_WITH_ME)) {
            return List.of();
        }

        return groupMembershipRepository.findAllByUserId(user.get().getId()).stream()
                .map(membership -> membership.getGroup().getId())
                .toList();
    }

    private Query keywordQuery(String content, String title, String author, Instant uploadedFrom, Instant uploadedTo,
                               DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        List<Query> must = new ArrayList<>();

        if (hasText(content)) {
            must.add(contentQuery(content.trim()));
        } else {
            must.add(Query.of(q -> q.matchAll(ma -> ma)));
        }

        return Query.of(q -> q.bool(b -> b
                .must(must)
                .filter(metadataFilters(title, author, uploadedFrom, uploadedTo, scope, user, groupIds))
        ));
    }

    private Query contentQuery(String content) {
        List<Query> contentQueries = List.of(
                Query.of(q -> q.multiMatch(mm -> mm
                        .query(content)
                        .fields("content^3", "title^2")
                        .type(TextQueryType.BestFields)
                )),
                Query.of(q -> q.multiMatch(mm -> mm
                        .query(content)
                        .fields("content^3", "title^2")
                        .type(TextQueryType.BoolPrefix)
                )),
                Query.of(q -> q.multiMatch(mm -> mm
                        .query(content)
                        .fields("content^3", "title^2")
                        .type(TextQueryType.PhrasePrefix)
                ))
        );

        return Query.of(q -> q.bool(b -> b
                .should(contentQueries)
                .minimumShouldMatch("1")
        ));
    }

    private Query vectorQuery(String content, String title, String author, Instant uploadedFrom, Instant uploadedTo,
                              DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        if (!hasText(content)) {
            throw new BadRequestException(
                    ErrorCode.DOCUMENT_INVALID_STATE, "Vector search requires a non-blank content query.");
        }

        List<Query> filter = metadataFilters(title, author, uploadedFrom, uploadedTo, scope, user, groupIds);
        filter.add(Query.of(q -> q.exists(e -> e.field("embedding"))));
        float[] queryVector = documentProcessingClient.embedText(content.trim());

        return Query.of(q -> q.scriptScore(ss -> ss
                .query(Query.of(inner -> inner.bool(b -> b.filter(filter))))
                .script(s -> s
                        .source(source -> source.scriptString("cosineSimilarity(params.query_vector, 'embedding') + 1.0"))
                        .params("query_vector", JsonData.of(queryVector))
                )
        ));
    }

    private List<Query> metadataFilters(String title, String author, Instant uploadedFrom, Instant uploadedTo,
                                        DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        List<Query> filter = new ArrayList<>();

        if (hasText(title)) {
            filter.add(titleQuery(title.trim()));
        }
        if (hasText(author)) {
            filter.add(authorQuery(author.trim()));
        }
        if (uploadedFrom != null || uploadedTo != null) {
            filter.add(uploadedDateRangeQuery(uploadedFrom, uploadedTo));
        }

        filter.add(accessQuery(scope, user, groupIds));
        return filter;
    }

    private Query uploadedDateRangeQuery(Instant uploadedFrom, Instant uploadedTo) {
        return Query.of(q -> q.range(r -> r.untyped(d -> {
            d.field("createdAt");
            if (uploadedFrom != null) {
                d.gte(JsonData.of(uploadedFrom.toEpochMilli()));
            }
            if (uploadedTo != null) {
                d.lte(JsonData.of(uploadedTo.toEpochMilli()));
            }
            return d;
        })));
    }

    private Query authorQuery(String author) {
        String normalizedAuthor = author.toLowerCase(Locale.ROOT);
        List<Query> authorQueries = List.of(
                termQuery("ownerLogin", normalizedAuthor),
                Query.of(q -> q.prefix(p -> p
                        .field("ownerLogin")
                        .value(normalizedAuthor)
                        .caseInsensitive(true)
                )),
                Query.of(q -> q.wildcard(w -> w
                        .field("ownerLogin")
                        .value(wildcardValue(author))
                        .caseInsensitive(true)
                ))
        );

        return Query.of(q -> q.bool(b -> b
                .should(authorQueries)
                .minimumShouldMatch("1")
        ));
    }

    private Query titleQuery(String title) {
        List<Query> titleQueries = List.of(
                Query.of(q -> q.match(m -> m
                        .field("title")
                        .query(title)
                        .fuzziness("AUTO")
                )),
                Query.of(q -> q.matchBoolPrefix(m -> m
                        .field("title")
                        .query(title)
                        .fuzziness("AUTO")
                )),
                Query.of(q -> q.matchPhrasePrefix(m -> m
                        .field("title")
                        .query(title)
                )),
                Query.of(q -> q.wildcard(w -> w
                        .field("title")
                        .value(wildcardValue(title))
                        .caseInsensitive(true)
                ))
        );

        return Query.of(q -> q.bool(b -> b
                .should(titleQueries)
                .minimumShouldMatch("1")
        ));
    }

    private Query accessQuery(DocumentSearchScope scope, Optional<User> user, List<Long> groupIds) {
        if (user.isEmpty()) {
            return switch (scope) {
                case PUBLIC, ACCESSIBLE -> publicQuery();
                case OWNED_BY_ME, SHARED_WITH_ME -> matchNoneQuery();
            };
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

    private Query matchNoneQuery() {
        return Query.of(q -> q.matchNone(mn -> mn));
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

    private boolean shouldHighlight(String content, String title) {
        return hasText(content) || hasText(title);
    }

    private HighlightQuery highlightQuery(boolean contentSearch, boolean titleSearch) {
        var params = HighlightParameters.builder()
                .withPreTags(HIGHLIGHT_PRE_TAG)
                .withPostTags(HIGHLIGHT_POST_TAG)
                .withFragmentSize(SNIPPET_LENGTH)
                .withNumberOfFragments(1)
                .withRequireFieldMatch(true)
                .build();
        List<HighlightField> fields = new ArrayList<>();
        if (contentSearch) {
            fields.add(new HighlightField("content"));
            fields.add(new HighlightField("title"));
        } else if (titleSearch) {
            fields.add(new HighlightField("title"));
        }

        return new HighlightQuery(
                new Highlight(params, fields),
                DocumentFragment.class
        );
    }

    private Aggregation documentCountAggregation() {
        return Aggregation.of(a -> a.cardinality(c -> c
                .field("documentId")
                .precisionThreshold(DOCUMENT_COUNT_PRECISION_THRESHOLD)
        ));
    }

    private List<SearchHit<DocumentFragment>> distinctDocumentHits(List<SearchHit<DocumentFragment>> hits) {
        Map<Long, SearchHit<DocumentFragment>> hitsByDocumentId = new LinkedHashMap<>();
        for (var hit : hits) {
            hitsByDocumentId.putIfAbsent(hit.getContent().getDocumentId(), hit);
        }
        return List.copyOf(hitsByDocumentId.values());
    }

    private long totalHits(SearchHits<DocumentFragment> hits, List<SearchHit<DocumentFragment>> resultHits) {
        if (hits.getAggregations() instanceof ElasticsearchAggregations aggregations) {
            var aggregation = aggregations.get(DOCUMENT_COUNT_AGGREGATION);
            if (aggregation != null && aggregation.aggregation().getAggregate().isCardinality()) {
                return aggregation.aggregation().getAggregate().cardinality().value();
            }
        }
        return resultHits.size();
    }

    private Map<Long, Document> findDocumentMetadata(List<SearchHit<DocumentFragment>> hits) {
        List<Long> documentIds = hits.stream()
                .map(hit -> hit.getContent().getDocumentId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (documentIds.isEmpty()) {
            return Map.of();
        }

        return documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
    }

    private DocumentSearchResultDto toDto(SearchHit<DocumentFragment> hit, Document document, String titleSearch) {
        DocumentFragment fragment = hit.getContent();
        String highlightedTitle = highlightedTitle(hit, fragment, titleSearch);
        String highlightedContent = highlightSnippet(hit, "content").orElse(snippet(fragment.getContent()));

        return new DocumentSearchResultDto(
                fragment.getDocumentId(),
                fragment.getFragmentOrder(),
                fragment.getTitle(),
                document != null ? document.getOriginalFilename() : fragment.getOriginalFilename(),
                document != null ? document.getMimeType() : fragment.getMimeType(),
                document != null ? document.getSizeBytes() : fragment.getSizeBytes(),
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

    private String highlightedTitle(SearchHit<DocumentFragment> hit, DocumentFragment fragment, String titleSearch) {
        if (hasText(titleSearch)) {
            return highlightExactSubstring(fragment.getTitle(), titleSearch.trim()).orElse(fragment.getTitle());
        }
        return highlightSnippet(hit, "title").orElse(fragment.getTitle());
    }

    private Optional<String> highlightExactSubstring(String value, String search) {
        if (!hasText(value) || !hasText(search)) {
            return Optional.empty();
        }

        String normalizedValue = value.toLowerCase(Locale.ROOT);
        String normalizedSearch = search.toLowerCase(Locale.ROOT);
        int start = normalizedValue.indexOf(normalizedSearch);
        if (start < 0) {
            return Optional.empty();
        }

        int end = start + search.length();
        return Optional.of(value.substring(0, start)
                + HIGHLIGHT_PRE_TAG
                + value.substring(start, end)
                + HIGHLIGHT_POST_TAG
                + value.substring(end));
    }

    private Optional<String> highlightSnippet(SearchHit<DocumentFragment> hit, String field) {
        List<String> values = hit.getHighlightField(field);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
    }

    private String snippet(String value) {
        if (value == null || value.length() <= SNIPPET_LENGTH) {
            return value;
        }

        String prefix = value.substring(0, SNIPPET_LENGTH).stripTrailing();
        int breakIndex = lastWhitespaceIndex(prefix);
        if (breakIndex > SNIPPET_LENGTH / 2) {
            prefix = prefix.substring(0, breakIndex).stripTrailing();
        }
        return prefix + "...";
    }

    private int lastWhitespaceIndex(String value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String wildcardValue(String value) {
        return "*" + value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("?", "\\?") + "*";
    }
}