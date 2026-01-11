package com.pw.docvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.service.document.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock 
    private DocumentService documentService;

    @Mock 
    private DocumentRepository documentRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, documentRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void uploadDraftReturnsCreatedId() throws Exception {
        when(documentService.createDocumentDraft(eq("My File"), eq("Desc"), eq(DocumentVisibility.PUBLIC)))
                .thenReturn(10L);

        mockMvc.perform(post("/documents/draft")
                        .param("title", "My File")
                        .param("description", "Desc")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isCreated())
                .andExpect(content().string("10"));
    }

    @Test
    void initiateUploadReturnsSignedUrl() throws Exception {
        when(documentService.initiateUpload(eq(10L), eq("image/png"), eq("pic.png")))
                .thenReturn("http://signed-url");

        mockMvc.perform(post("/documents/10/sign-upload")
                        .param("contentType", "image/png")
                        .param("originalFilename", "pic.png"))
                .andExpect(status().isOk())
                .andExpect(content().string("http://signed-url"));
    }

    @Test
    void completeUploadReturnsCreated() throws Exception {
        mockMvc.perform(post("/documents/10/complete-upload"))
                .andExpect(status().isCreated());

        verify(documentService).completeUpload(10L);
    }

    @Test
    void downloadReturnsSignedUrl() throws Exception {
        when(documentService.download(10L)).thenReturn("http://download-url");

        mockMvc.perform(get("/documents/download/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("http://download-url"));
    }

    @Test
    void listDocumentsReturnsPage() throws Exception {
        DocumentDto dto = new DocumentDto(10L, "T", "D", "f", "ct", Instant.now(), 
                DocumentVisibility.PUBLIC, 1L, "u", 100L, DocumentStatus.UPLOADED, null, null);
        
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        
        when(documentService.listUserDocuments(any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/documents")
                        .param("titleSearch", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/documents/delete/10"))
                .andExpect(status().isNoContent());

        verify(documentService).delete(10L);
    }
}
