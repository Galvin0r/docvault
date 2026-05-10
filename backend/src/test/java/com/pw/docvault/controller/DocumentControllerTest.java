package com.pw.docvault.controller;

import com.pw.docvault.model.document.DocumentAccessDto;
import com.pw.docvault.model.document.DocumentContentFragmentDto;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.document.DocumentSearchResultDto;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.service.document.DocumentAccessService;
import com.pw.docvault.service.document.DocumentSearchService;
import com.pw.docvault.service.document.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    private DocumentAccessService documentAccessService;

    @Mock
    private DocumentSearchService documentSearchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, documentAccessService, documentSearchService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void uploadDraftReturnsCreatedId() throws Exception {
        when(documentService.createDocumentDraft(eq("My File"), eq("Desc"), eq(DocumentVisibility.PUBLIC)))
                .thenReturn(10L);

        mockMvc.perform(post("/document/draft")
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

        mockMvc.perform(post("/document/10/sign-upload")
                        .param("contentType", "image/png")
                        .param("originalFilename", "pic.png"))
                .andExpect(status().isOk())
                .andExpect(content().string("http://signed-url"));
    }

    @Test
    void completeUploadReturnsCreated() throws Exception {
        mockMvc.perform(post("/document/10/complete-upload"))
                .andExpect(status().isCreated());

        verify(documentService).completeUpload(10L);
    }

    @Test
    void updateVisibilityReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/document/10/visibility")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isNoContent());

        verify(documentService).updateVisibility(10L, DocumentVisibility.PUBLIC);
    }

    @Test
    void updateTitleReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/document/10/title")
                        .param("title", "Updated title"))
                .andExpect(status().isNoContent());

        verify(documentService).updateTitle(10L, "Updated title");
    }

    @Test
    void downloadReturnsSignedUrl() throws Exception {
        when(documentService.download(10L)).thenReturn("http://download-url");

        mockMvc.perform(get("/document/download/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("http://download-url"));
    }

    @Test
    void getDocumentReturnsReadableDocument() throws Exception {
        var uploadedAt = Instant.parse("2026-04-11T10:15:30Z");
        var dto = new DocumentDto(10L, "Readable", "D", "f.txt", "text/plain", uploadedAt,
                DocumentVisibility.PRIVATE, 1L, "alice", 100L, DocumentStatus.INDEXED, null, null);
        when(documentService.getReadableDocument(10L)).thenReturn(dto);

        mockMvc.perform(get("/document/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Readable"))
                .andExpect(jsonPath("$.ownerLogin").value("alice"));
    }

    @Test
    void contentPreviewReturnsReadableFragments() throws Exception {
        when(documentService.getReadableContentPreview(10L, 2)).thenReturn(List.of(
                new DocumentContentFragmentDto(0, "First"),
                new DocumentContentFragmentDto(1, "Second")
        ));

        mockMvc.perform(get("/document/10/fragments").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fragmentOrder").value(0))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].fragmentOrder").value(1));
    }

    @Test
    void listDocumentsReturnsPage() throws Exception {
        DocumentDto dto = new DocumentDto(10L, "T", "D", "f", "ct", Instant.now(), 
                DocumentVisibility.PUBLIC, 1L, "u", 100L, DocumentStatus.UPLOADED, null, null);
        
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        
        when(documentService.listUserDocuments(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/document")
                        .param("titleSearch", "test")
                        .param("groupId", "7")
                        .param("ownedOnly", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(documentService).listUserDocuments("test", null, null, null, 7L, true, PageRequest.of(0, 10));
    }

    @Test
    void searchReturnsPage() throws Exception {
        var uploadedAt = Instant.parse("2026-04-11T10:15:30Z");
        var dto = new DocumentSearchResultDto(
                10L, 2, "T", "tax.pdf", "application/pdf", 2048L,
                "<mark>T</mark>", "snippet", "<mark>snippet</mark>",
                uploadedAt, 1L, "alice", DocumentVisibility.PUBLIC, 2.5f
        );
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        when(documentSearchService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/document/search")
                        .param("mode", "KEYWORD")
                        .param("content", "invoice")
                        .param("title", "tax")
                        .param("author", "alice")
                        .param("uploadedFrom", "2026-04-01T00:00:00Z")
                        .param("uploadedTo", "2026-04-30T23:59:59Z")
                        .param("scope", "PUBLIC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].documentId").value(10))
                .andExpect(jsonPath("$.content[0].originalFilename").value("tax.pdf"))
                .andExpect(jsonPath("$.content[0].size").value(2048))
                .andExpect(jsonPath("$.content[0].highlightedContentSnippet").value("<mark>snippet</mark>"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(documentSearchService).search(
                eq(DocumentSearchMode.KEYWORD),
                eq("invoice"),
                eq("tax"),
                eq("alice"),
                eq(Instant.parse("2026-04-01T00:00:00Z")),
                eq(Instant.parse("2026-04-30T23:59:59Z")),
                eq(DocumentSearchScope.PUBLIC),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void listAccessReturnsEntries() throws Exception {
        var access = new DocumentAccessDto(1L, 10L, 2L, "alice", null, null);
        when(documentAccessService.listAccess(10L)).thenReturn(List.of(access));

        mockMvc.perform(get("/document/10/access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(10))
                .andExpect(jsonPath("$[0].userLogin").value("alice"));
    }

    @Test
    void grantUserAccessReturnsNoContent() throws Exception {
        mockMvc.perform(put("/document/10/access/users/2"))
                .andExpect(status().isNoContent());

        verify(documentAccessService).grantUserAccess(10L, 2L);
    }

    @Test
    void grantUserAccessByLoginReturnsNoContent() throws Exception {
        mockMvc.perform(put("/document/10/access/users/by-login")
                        .param("login", "alice"))
                .andExpect(status().isNoContent());

        verify(documentAccessService).grantUserAccessByLogin(10L, "alice");
    }

    @Test
    void revokeUserAccessReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/document/10/access/users/2"))
                .andExpect(status().isNoContent());

        verify(documentAccessService).revokeUserAccess(10L, 2L);
    }

    @Test
    void grantGroupAccessReturnsNoContent() throws Exception {
        mockMvc.perform(put("/document/10/access/groups/4"))
                .andExpect(status().isNoContent());

        verify(documentAccessService).grantGroupAccess(10L, 4L);
    }

    @Test
    void revokeGroupAccessReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/document/10/access/groups/4"))
                .andExpect(status().isNoContent());

        verify(documentAccessService).revokeGroupAccess(10L, 4L);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/document/delete/10"))
                .andExpect(status().isNoContent());

        verify(documentService).delete(10L);
    }
}
