package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentFragment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(DocumentProcessingClient.class)
public class StubDocumentProcessingClient implements DocumentProcessingClient {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    @Override
    public void processFromSignedUrl(String signedUrl, String mimeType, java.util.function.Consumer<DocumentFragment> fragmentConsumer) {
        String safeMimeType = (mimeType == null || mimeType.isBlank()) ? DEFAULT_MIME_TYPE : mimeType;
        int fragmentCount = fragmentCountFor(safeMimeType);

        for (int i = 0; i < fragmentCount; i++) {
            DocumentFragment fragment = new DocumentFragment();
            fragment.setFragmentOrder(i);
            fragment.setContent("""
                    Stub fragment %d generated for mime type %s.
                    Replace StubDocumentProcessingClient with the streaming processor service later.
                    """.formatted(i + 1, safeMimeType).trim());
            fragmentConsumer.accept(fragment);
        }
    }

    private int fragmentCountFor(String mimeType) {
        if (mimeType.startsWith("text/")) {
            return 1;
        }
        if ("application/pdf".equalsIgnoreCase(mimeType)) {
            return 3;
        }
        return 2;
    }
}
