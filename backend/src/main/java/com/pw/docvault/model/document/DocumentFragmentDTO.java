package com.pw.docvault.model.document;

import com.pw.docvault.model.enums.DocumentVisibility;

import java.time.Instant;

public record DocumentFragmentDTO(
        Long documentId,
        String title,
        String highlightedTitle,
        String highlightedContentSnippet,
        Instant createdAt,
        String ownerName,
        DocumentVisibility visibility
) {}
