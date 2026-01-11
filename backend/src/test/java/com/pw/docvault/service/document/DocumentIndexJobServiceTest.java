package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIndexJobServiceTest {

    @Mock
    private DocumentIndexJobRepository documentIndexJobRepository;

    @InjectMocks
    private DocumentIndexJobService documentIndexJobService;

    @Test
    void createCorrectlyInitializesJob() {
        Document document = new Document();
        document.setId(123L);

        when(documentIndexJobRepository.save(any(DocumentIndexJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentIndexJob result = documentIndexJobService.create(document);

        ArgumentCaptor<DocumentIndexJob> captor = ArgumentCaptor.forClass(DocumentIndexJob.class);
        verify(documentIndexJobRepository).save(captor.capture());

        DocumentIndexJob savedJob = captor.getValue();
        assertThat(savedJob.getDocument()).isEqualTo(document);
        assertThat(savedJob.getAttempts()).isEqualTo((short) 0);
        assertThat(savedJob.getStatus()).isEqualTo(DocumentIndexJobStatus.PENDING);
        assertThat(result).isEqualTo(savedJob);
    }
}
