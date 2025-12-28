package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

}
