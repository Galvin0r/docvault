package com.pw.docvault.controller;

import com.pw.docvault.entity.Document;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.DocumentRepository;
import com.pw.docvault.service.DocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("document")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentService documentService, DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("title") String title,
                                       @RequestParam("description") String description,
                                       @RequestParam("visibility") DocumentVisibility visibility) {
        documentService.upload(file, title, description, visibility);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(value = "/download/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
        Document document = documentRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Document with id " + id + " not found"));
        StreamingResponseBody stream = documentService.download(document);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
