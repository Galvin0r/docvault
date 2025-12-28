package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.exception.*;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.service.GoogleCloudStorageService;
import com.pw.docvault.service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final GoogleCloudStorageService googleCloudStorageService;
    private final CurrentUserProvider currentUser;
    private final DocumentIndexJobService documentIndexJobService;

    @Value(value = "${app.gsc.buffer.storage.space}")
    private Integer bufferStorageSpace;

    public Long createDocumentDraft(String title, String description, DocumentVisibility visibility) {
        var user = currentUser.get();

        Document document = new Document();
        document.setTitle(title);
        document.setDescription((description == null || description.isBlank()) ? null : description);
        document.setVisibility(visibility);
        document.setOwner(user);
        document.setStatus(DocumentStatus.UPLOADING);

        return documentRepository.save(document).getId();
    }

    public void upload(Long id, MultipartFile file) {
        var user = currentUser.get();
        var draft = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id: " + id + " not found"));
        if (draft.getStatus() != DocumentStatus.UPLOADING) {
            throw new ConflictException(ErrorCode.DOCUMENT_INVALID_STATE,
                                        "Document for id " + id + " is already loaded. Please, try again.");
        }
        if (!Objects.equals(draft.getOwner().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_FORBIDDEN,
                                         "Uploading document for other user is forbidden");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.DOCUMENT_EMPTY, "Empty file");
        }

        String key = null;
        try (InputStream in = file.getInputStream()) {
            String safeName = safeFilename(file.getOriginalFilename());
            String contentType = safeContentType(file.getContentType());

            key = googleCloudStorageService.upload(in, safeName, user.getId(), contentType);

            draft.setMimeType(contentType);
            draft.setPath(key);
            draft.setOriginalFilename(safeName);
            draft.setStatus(DocumentStatus.UPLOADED);
            draft.setSizeBytes(file.getSize());

            documentRepository.save(draft);
            documentIndexJobService.create(draft);
        } catch (Exception e) {
            if (key != null) {
                try {
                    googleCloudStorageService.delete(key);
                } catch (Exception ignored) {}
            }
            documentRepository.deleteById(id);
            throw new DocumentException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "Document upload failed");
        }
    }

    private String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private String safeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }

    public void delete(Long id) {
        Document document = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id " + id + " not found"));
        documentRepository.delete(document);
        googleCloudStorageService.delete(document.getPath());
    }

    public StreamingResponseBody download(Document document) {
        InputStream inputStream = googleCloudStorageService.download(document.getPath());;

        return outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[bufferStorageSpace];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };
    }
}
