package com.pw.docvault.service;

import com.pw.docvault.entity.Document;
import com.pw.docvault.entity.User;
import com.pw.docvault.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final GoogleCloudStorageService googleCloudStorageService;

    @Value(value = "${app.gsc.buffer.storage.space}")
    private Integer bufferStorageSpace;

    public DocumentService(DocumentRepository documentRepository, GoogleCloudStorageService googleCloudStorageService) {
        this.documentRepository = documentRepository;
        this.googleCloudStorageService = googleCloudStorageService;
    }

    public void upload(MultipartFile file, String title, String description, String visibility) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = user.getId();
        try {
            String key = googleCloudStorageService.upload(file.getInputStream(), file.getOriginalFilename(), userId, file.getContentType());
            Document document = new Document();
            document.setDescription(description);
            document.setVisibility(visibility);
            document.setMimeType(file.getContentType());
            document.setPath(key);
            document.setTitle(title);
            document.setOriginalFilename(file.getOriginalFilename());
            document.setOwner(user);
            documentRepository.save(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Long id) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Document with id " + id + " not found"));
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
