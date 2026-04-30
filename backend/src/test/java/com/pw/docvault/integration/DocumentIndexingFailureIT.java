package com.pw.docvault.integration;

import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.DocumentException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class DocumentIndexingFailureIT extends AbstractDocumentIT {

    private final DocumentIntegrationSupport documents;

    @BeforeEach
    void resetState() throws InterruptedException {
        documents.resetState();
    }

    @Test
    void indexContentFailureRetriesAndLaterIndexesMultipleFragments() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createDocument(alice, "Retryable Index", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADED, Instant.parse("2026-06-01T10:00:00Z"));
        documents.createJob(document, DocumentSyncOperation.INDEX_CONTENT);
        when(googleCloudStorageService.generateGetSignedUrl(anyString())).thenReturn("https://storage.example/source");

        doThrow(new DocumentException(ErrorCode.DOCUMENT_INVALID_STATE, "processor unavailable"))
                .when(documentProcessingClient)
                .processFromSignedUrl(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        documents.documentIndexWorker().poll();

        var retryJob = documents.jobsForDocument(document).getFirst();
        assertThat(documents.findDocument(document.getId()).getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(retryJob.getStatus()).isEqualTo(DocumentIndexJobStatus.RETRY);
        assertThat(retryJob.getAttempts()).isEqualTo((short) 1);
        assertThat(retryJob.getLastError()).contains("processor unavailable");

        reset(documentProcessingClient);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var consumer = (java.util.function.Consumer<DocumentFragment>) invocation.getArgument(2);
            var first = new DocumentFragment();
            first.setContent("orchidtoken retry fragment");
            consumer.accept(first);
            var second = new DocumentFragment();
            second.setContent("basalttoken retry fragment");
            consumer.accept(second);
            return null;
        }).when(documentProcessingClient).processFromSignedUrl(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        var doneJob = documents.jobsForDocument(document).getFirst();
        assertThat(documents.findDocument(document.getId()).getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(doneJob.getStatus()).isEqualTo(DocumentIndexJobStatus.DONE);
        assertThat(doneJob.getAttempts()).isEqualTo((short) 1);

        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "orchidtoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "basalttoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void indexContentFailureMarksDocumentFailedAfterMaxAttempts() {
        var alice = documents.createUser("alice");
        var document = documents.createDocument(alice, "Permanent Index Failure", DocumentVisibility.PRIVATE,
                DocumentStatus.UPLOADED, Instant.parse("2026-06-02T10:00:00Z"));
        documents.createJob(document, DocumentSyncOperation.INDEX_CONTENT);
        when(googleCloudStorageService.generateGetSignedUrl(anyString())).thenReturn("https://storage.example/source");
        doThrow(new DocumentException(ErrorCode.DOCUMENT_INVALID_STATE, "processor permanently down"))
                .when(documentProcessingClient)
                .processFromSignedUrl(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        documents.documentIndexWorker().poll();
        documents.documentIndexWorker().poll();
        documents.documentIndexWorker().poll();

        var failedJob = documents.jobsForDocument(document).getFirst();
        assertThat(documents.findDocument(document.getId()).getStatus()).isEqualTo(DocumentStatus.INDEX_FAILED);
        assertThat(failedJob.getStatus()).isEqualTo(DocumentIndexJobStatus.FAILED);
        assertThat(failedJob.getAttempts()).isEqualTo((short) 3);
        assertThat(failedJob.getLastError()).contains("processor permanently down");
    }

    @Test
    void deleteFailureRetriesAndLaterRemovesPostgresAndSearchMetadata() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createIndexedDocument(alice, "Retryable Delete", DocumentVisibility.PRIVATE,
                Instant.parse("2026-06-03T10:00:00Z"));
        documents.indexFragment(document, "delete retry keyword");
        documents.refreshIndex();

        doThrow(new RuntimeException("gcs unavailable"))
                .doNothing()
                .when(googleCloudStorageService).delete(anyString());

        documents.mockMvc().perform(delete("/document/delete/%d".formatted(document.getId()))
                        .with(user(alice)))
                .andExpect(status().isInternalServerError());

        var retryJob = documents.jobsForDocument(document).getFirst();
        assertThat(documents.findDocument(document.getId()).getStatus()).isEqualTo(DocumentStatus.DELETING);
        assertThat(retryJob.getOperation()).isEqualTo(DocumentSyncOperation.DELETE);
        assertThat(retryJob.getStatus()).isEqualTo(DocumentIndexJobStatus.RETRY);

        documents.documentIndexWorker().poll();
        documents.refreshIndex();

        assertThat(documents.documentExists(document.getId())).isFalse();
        assertThat(documents.jobsForDocument(document)).isEmpty();
        documents.mockMvc().perform(get("/document/search")
                        .with(user(alice))
                        .param("content", "delete retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteFailureMarksDocumentFailedAfterMaxAttempts() throws Exception {
        var alice = documents.createUser("alice");
        var document = documents.createIndexedDocument(alice, "Permanent Delete Failure", DocumentVisibility.PRIVATE,
                Instant.parse("2026-06-04T10:00:00Z"));

        doThrow(new RuntimeException("gcs permanently unavailable"))
                .when(googleCloudStorageService).delete(anyString());

        documents.mockMvc().perform(delete("/document/delete/%d".formatted(document.getId()))
                        .with(user(alice)))
                .andExpect(status().isInternalServerError());
        documents.documentIndexWorker().poll();
        documents.documentIndexWorker().poll();

        var failedJob = documents.jobsForDocument(document).getFirst();
        assertThat(documents.findDocument(document.getId()).getStatus()).isEqualTo(DocumentStatus.DELETE_FAILED);
        assertThat(failedJob.getOperation()).isEqualTo(DocumentSyncOperation.DELETE);
        assertThat(failedJob.getStatus()).isEqualTo(DocumentIndexJobStatus.FAILED);
        assertThat(failedJob.getAttempts()).isEqualTo((short) 3);
    }
}
