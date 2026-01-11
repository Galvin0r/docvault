package com.pw.docvault.service.document;

import com.google.cloud.storage.Blob;
import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.exception.ConflictException;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.DocumentMapper;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentAccessRepository documentAccessRepository;

    @Mock
    private DocumentIndexJobRepository documentIndexJobRepository;

    @Mock
    private GoogleCloudStorageService googleCloudStorageService;

    @Mock
    private CurrentUserProvider currentUser;

    @Mock
    private DocumentIndexJobService documentIndexJobService;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        
        ReflectionTestUtils.setField(documentService, "bufferStorageSpace", 500);
    }

    @Test
    void createDocumentDraftSavesNewDocument() {
        when(currentUser.get()).thenReturn(user);
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(10L);
            return doc;
        });

        Long id = documentService.createDocumentDraft("Title", "Desc", DocumentVisibility.PRIVATE);

        assertThat(id).isEqualTo(10L);
        verify(documentRepository).save(argThat(doc -> 
            doc.getTitle().equals("Title") && 
            doc.getDescription().equals("Desc") &&
            doc.getOwner().equals(user) &&
            doc.getStatus() == DocumentStatus.UPLOADING
        ));
    }

    @Test
    void initiateUploadGeneratesSignedUrl() {
        when(currentUser.get()).thenReturn(user);
        Document doc = new Document();
        doc.setId(10L);
        doc.setTitle("My Doc");
        doc.setOwner(user);
        doc.setStatus(DocumentStatus.UPLOADING);

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(googleCloudStorageService.generatePutSignedUrl(anyString(), anyString())).thenReturn("http://signed-url");

        String url = documentService.initiateUpload(10L, "text/plain", "file.txt");

        assertThat(url).isEqualTo("http://signed-url");
        assertThat(doc.getOriginalFilename()).isEqualTo("file.txt");
        assertThat(doc.getMimeType()).isEqualTo("text/plain");
        assertThat(doc.getPath()).contains("user_1/My Doc_");
        verify(documentRepository).save(doc);
    }

    @Test
    void initiateUploadThrowsConflictIfAlreadyUploaded() {
        when(currentUser.get()).thenReturn(user);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOwner(user);
        doc.setStatus(DocumentStatus.UPLOADED);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThrows(ConflictException.class, () -> 
            documentService.initiateUpload(10L, "text/plain", "file.txt")
        );
    }

    @Test
    void initiateUploadThrowsForbiddenIfWrongOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(currentUser.get()).thenReturn(user);
        
        Document doc = new Document();
        doc.setId(10L);
        doc.setOwner(otherUser);
        doc.setStatus(DocumentStatus.UPLOADING);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThrows(ForbiddenException.class, () -> 
            documentService.initiateUpload(10L, "text/plain", "file.txt")
        );
    }

    @Test
    void completeUploadUpdatesStatusAndCreatesJob() {
        when(currentUser.get()).thenReturn(user);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOwner(user);
        doc.setPath("path/to/file");

        Blob blob = mock(Blob.class);
        when(blob.exists()).thenReturn(true);
        when(blob.getSize()).thenReturn(1024L);

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(googleCloudStorageService.getMetadata("path/to/file")).thenReturn(blob);

        documentService.completeUpload(10L);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(doc.getSizeBytes()).isEqualTo(1024L);
        verify(documentRepository).save(doc);
        verify(documentIndexJobService).create(doc);
    }

    @Test
    void completeUploadThrowsIfFileNotFoundInGCS() {
        when(currentUser.get()).thenReturn(user);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOwner(user);
        doc.setPath("missing");

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(googleCloudStorageService.getMetadata("missing")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> documentService.completeUpload(10L));
    }

    @Test
    void deleteRemovesFromDbAndGcs() {
        Document doc = new Document();
        doc.setId(10L);
        doc.setPath("path/to/delete");

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        documentService.delete(10L);

        verify(documentIndexJobRepository).deleteByDocumentId(10L);
        verify(documentAccessRepository).deleteByDocumentId(10L);
        verify(documentRepository).delete(doc);
        verify(googleCloudStorageService).delete("path/to/delete");
    }

    @Test
    void downloadReturnsSignedUrl() {
        Document doc = new Document();
        doc.setId(10L);
        doc.setPath("path/to/download");

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(googleCloudStorageService.generateGetSignedUrl("path/to/download")).thenReturn("http://download-url");

        String url = documentService.download(10L);

        assertThat(url).isEqualTo("http://download-url");
    }

    @Test
    void listUserDocumentsEnrichesWithOwnerJobInfo() {
        when(currentUser.get()).thenReturn(user);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOwner(user);

        Page<Document> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findDocumentsWithAccess(eq(user.getId()), any(), any(), any(), any(), any()))
                .thenReturn(page);

        DocumentDto baseDto = new DocumentDto(10L, "Title", "Desc", "file.txt", "text/plain", 
                Instant.now(), DocumentVisibility.PRIVATE, 1L, "testuser", 1024L, DocumentStatus.UPLOADED, null, null);
        
        when(documentMapper.toDto(doc)).thenReturn(baseDto);
        
        DocumentIndexJob job = new DocumentIndexJob();
        job.setAttempts((short) 5);
        job.setLastError("Failed to index");
        when(documentIndexJobRepository.findByDocumentId(10L)).thenReturn(Optional.of(job));

        Page<DocumentDto> result = documentService.listUserDocuments(null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        DocumentDto enriched = result.getContent().get(0);
        assertThat(enriched.attempts()).isEqualTo((short) 5);
        assertThat(enriched.lastError()).isEqualTo("Failed to index");
    }
}
