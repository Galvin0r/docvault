package com.pw.docvault.repository.group;


import com.pw.docvault.entity.group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
