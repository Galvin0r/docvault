package com.pw.docvault.model.document;

public record DocumentContentFragmentDto(
        Integer fragmentOrder,
        Integer pageNumber,
        String content
) {}