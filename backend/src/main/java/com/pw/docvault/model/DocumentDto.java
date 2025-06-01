package com.pw.docvault.model;

import java.time.LocalDateTime;

public record DocumentDto(
        Long id,
        String title,
        String description,
        String originalFilename,
        String mimeType,
        String path,
        LocalDateTime uploadedAt,
        String visibility,
        Long ownerId,
        String ownerLogin
) {}
