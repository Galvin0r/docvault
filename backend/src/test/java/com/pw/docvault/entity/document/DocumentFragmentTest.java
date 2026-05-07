package com.pw.docvault.entity.document;

import com.pw.docvault.model.enums.DocumentVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFragmentTest {

    @Test
    void readsMicrosecondInstantFromElasticsearchDocument() {
        var mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.afterPropertiesSet();
        var converter = new MappingElasticsearchConverter(mappingContext);
        converter.afterPropertiesSet();

        Document source = Document.create();
        source.setId("72830069-f404-4543-ab8a-d602bfeaf6d2");
        source.put("documentId", 42L);
        source.put("fragmentOrder", 0);
        source.put("title", "Policy");
        source.put("originalFilename", "policy.pdf");
        source.put("mimeType", "application/pdf");
        source.put("sizeBytes", 2048L);
        source.put("ownerId", 5L);
        source.put("ownerLogin", "alice");
        source.put("createdAt", "2026-04-19T11:11:41.539888Z");
        source.put("visibility", DocumentVisibility.PUBLIC.name());
        source.put("content", "A document fragment.");

        DocumentFragment fragment = converter.read(DocumentFragment.class, source);

        assertThat(fragment.getCreatedAt()).isEqualTo(Instant.parse("2026-04-19T11:11:41.539888Z"));
        assertThat(fragment.getOriginalFilename()).isEqualTo("policy.pdf");
        assertThat(fragment.getMimeType()).isEqualTo("application/pdf");
        assertThat(fragment.getSizeBytes()).isEqualTo(2048L);
    }
}