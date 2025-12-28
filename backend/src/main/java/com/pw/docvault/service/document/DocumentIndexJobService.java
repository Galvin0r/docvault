package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DocumentIndexJobService {

    private final DocumentIndexJobRepository documentIndexJobRepository;

    public DocumentIndexJob create(Document document) {
        var docIndexJob = new DocumentIndexJob();
        docIndexJob.setDocument(document);
        docIndexJob.setStatus(DocumentIndexJobStatus.PENDING);
        return documentIndexJobRepository.save(docIndexJob);
    }
}
