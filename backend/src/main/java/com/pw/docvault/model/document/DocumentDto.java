package com.pw.docvault.model.document;

import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String title,
        String description,
        String originalFilename,
        String mimeType,
        Instant uploadedAt,
        DocumentVisibility visibility,
        Long ownerId,
        String ownerLogin,
        Long size,
        DocumentStatus status
//        Short attempts,
//        String lastError
) {}
