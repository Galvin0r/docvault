package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {
}
