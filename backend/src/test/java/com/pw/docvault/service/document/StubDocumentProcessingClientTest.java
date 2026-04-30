package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentFragment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubDocumentProcessingClientTest {

    private final StubDocumentProcessingClient stubDocumentProcessingClient = new StubDocumentProcessingClient();

    @Test
    void processFromSignedUrlCreatesSingleTextFragment() {
        List<DocumentFragment> fragments = new ArrayList<>();

        stubDocumentProcessingClient.processFromSignedUrl("https://signed", "text/plain", fragments::add);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.getFirst().getFragmentOrder()).isZero();
        assertThat(fragments.getFirst().getContent()).contains("text/plain");
        assertThat(fragments.getFirst().getEmbedding()).hasSize(384);
    }

    @Test
    void processFromSignedUrlCreatesPdfFragments() {
        List<DocumentFragment> fragments = new ArrayList<>();

        stubDocumentProcessingClient.processFromSignedUrl("https://signed", "application/pdf", fragments::add);

        assertThat(fragments).hasSize(3);
        assertThat(fragments).extracting(DocumentFragment::getFragmentOrder).containsExactly(0, 1, 2);
    }

    @Test
    void embedTextReturnsDeterministicVector() {
        float[] first = stubDocumentProcessingClient.embedText("retention policy");
        float[] second = stubDocumentProcessingClient.embedText("retention policy");

        assertThat(first).hasSize(384);
        assertThat(first).containsExactly(second);
    }
}
