package com.pw.docvault.repository;

import com.pw.docvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

}
