package com.pw.docvault.model.document;

import com.pw.docvault.model.enums.DocumentVisibility;

import java.time.Instant;

public record DocumentSearchResultDto(
        Long documentId,
        Integer fragmentOrder,
        String title,
        String highlightedTitle,
        String contentSnippet,
        String highlightedContentSnippet,
        Instant uploadedAt,
        Long ownerId,
        String ownerLogin,
        DocumentVisibility visibility,
        float score
) {}
