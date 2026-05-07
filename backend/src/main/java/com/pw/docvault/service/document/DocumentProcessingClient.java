package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentFragment;

import java.util.function.Consumer;

public interface DocumentProcessingClient {

    void processFromSignedUrl(String signedUrl, String mimeType, Consumer<DocumentFragment> fragmentConsumer);

    float[] embedText(String text);
}