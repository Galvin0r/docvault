package com.pw.docvault.e2e;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.repository.security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Profile("e2e")
@RestController
@RequestMapping("/e2e")
@RequiredArgsConstructor
public class E2eFixtureController {

    private static final String PASSWORD = "password123";

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFragmentRepository documentFragmentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/reset")
    public E2eFixture reset() {
        resetPostgres();
        resetElasticsearch();

        User alice = createUser("alice");
        User bob = createUser("bob");

        Group research = createGroup("Research Team", GroupVisibility.PUBLIC);
        Group legal = createGroup("Legal Archive", GroupVisibility.PRIVATE);
        createMembership(alice, research, GroupRole.OWNER);
        createMembership(alice, legal, GroupRole.OWNER);
        createMembership(bob, research, GroupRole.USER);

        Document publicDocument = createDocument(
                alice,
                "Solar Clinic Backup Power",
                "solar-clinic.txt",
                DocumentVisibility.PUBLIC,
                Instant.parse("2026-03-20T12:00:00Z")
        );
        indexFragment(publicDocument, 0, "Battery cabinets keep the clinic online during grid outages.", List.of(), List.of());
        indexFragment(publicDocument, 1, "The backup inverter is tested every Friday morning.", List.of(), List.of());

        Document privateDocument = createDocument(
                alice,
                "Legal Archive Retention",
                "legal-archive.pdf",
                DocumentVisibility.PRIVATE,
                Instant.parse("2026-04-03T09:15:00Z")
        );
        indexFragment(privateDocument, 0, "Archive records are retained for compliance review.", List.of(), List.of());

        elasticsearchOperations.indexOps(DocumentFragment.class).refresh();

        return new E2eFixture(
                new Credentials("alice", "alice@example.test", PASSWORD),
                new Credentials("bob", "bob@example.test", PASSWORD),
                publicDocument.getId(),
                privateDocument.getId(),
                research.getId(),
                legal.getId()
        );
    }

    private void resetPostgres() {
        jdbcTemplate.execute("""
                TRUNCATE document_index_jobs, document_access, group_membership, group_join_requests,
                         documents, groups, activation_tokens, refresh_tokens, password_reset_tokens,
                         users_roles, users
                RESTART IDENTITY CASCADE
                """);
        jdbcTemplate.execute("""
                ALTER SEQUENCE users_seq RESTART WITH 1;
                ALTER SEQUENCE activation_tokens_seq RESTART WITH 1;
                ALTER SEQUENCE refresh_tokens_seq RESTART WITH 1;
                ALTER SEQUENCE groups_seq RESTART WITH 1;
                ALTER SEQUENCE documents_seq RESTART WITH 1;
                ALTER SEQUENCE document_access_seq RESTART WITH 1;
                ALTER SEQUENCE group_membership_seq RESTART WITH 1;
                ALTER SEQUENCE password_reset_tokens_seq RESTART WITH 1;
                ALTER SEQUENCE group_join_requests_seq RESTART WITH 1;
                ALTER SEQUENCE document_index_jobs_seq RESTART WITH 1;
                """);
    }

    private void resetElasticsearch() {
        var indexOps = elasticsearchOperations.indexOps(DocumentFragment.class);
        if (indexOps.exists()) {
            indexOps.delete();
            awaitIndexDeleted();
        }
        indexOps.createWithMapping();
    }

    private void awaitIndexDeleted() {
        var indexOps = elasticsearchOperations.indexOps(DocumentFragment.class);
        for (int i = 0; i < 50; i++) {
            if (!indexOps.exists()) {
                return;
            }
            sleep();
        }
        throw new IllegalStateException("Timed out waiting for Elasticsearch index deletion");
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while resetting E2E fixtures", ex);
        }
    }

    private User createUser(String login) {
        var role = roleRepository.findByName("USER").orElseThrow();
        var user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setEnabled(true);
        user.setRoles(List.of(role));
        return userRepository.saveAndFlush(user);
    }

    private Group createGroup(String name, GroupVisibility visibility) {
        var group = new Group();
        group.setName(name);
        group.setDescription(name + " documents");
        group.setVisibility(visibility);
        return groupRepository.saveAndFlush(group);
    }

    private GroupMembership createMembership(User user, Group group, GroupRole role) {
        var membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(role);
        return groupMembershipRepository.saveAndFlush(membership);
    }

    private Document createDocument(User owner, String title, String filename, DocumentVisibility visibility, Instant createdAt) {
        var document = new Document();
        document.setTitle(title);
        document.setDescription(title + " description");
        document.setOriginalFilename(filename);
        document.setPath("e2e/%s/%s".formatted(owner.getLogin(), filename));
        document.setMimeType(filename.endsWith(".pdf") ? "application/pdf" : "text/plain");
        document.setVisibility(visibility);
        document.setOwner(owner);
        document.setSizeBytes(2048L);
        document.setStatus(DocumentStatus.INDEXED);
        var saved = documentRepository.saveAndFlush(document);
        jdbcTemplate.update("UPDATE documents SET created = ? WHERE id = ?", java.sql.Timestamp.from(createdAt), saved.getId());
        saved.setCreated(createdAt);
        return saved;
    }

    private void indexFragment(Document document, int order, String content, List<Long> users, List<Long> groups) {
        var fragment = new DocumentFragment();
        fragment.setId(UUID.randomUUID().toString());
        fragment.setDocumentId(document.getId());
        fragment.setFragmentOrder(order);
        fragment.setTitle(document.getTitle());
        fragment.setOriginalFilename(document.getOriginalFilename());
        fragment.setMimeType(document.getMimeType());
        fragment.setSizeBytes(document.getSizeBytes());
        fragment.setOwnerId(document.getOwner().getId());
        fragment.setOwnerLogin(document.getOwner().getLogin());
        fragment.setCreatedAt(document.getCreated());
        fragment.setVisibility(document.getVisibility());
        fragment.setPermittedUserIds(List.copyOf(users));
        fragment.setPermittedGroupIds(List.copyOf(groups));
        fragment.setContent(content);
        documentFragmentRepository.save(fragment);
    }

    public record E2eFixture(
            Credentials alice,
            Credentials bob,
            Long publicDocumentId,
            Long privateDocumentId,
            Long researchGroupId,
            Long legalGroupId
    ) {
    }

    public record Credentials(String login, String email, String password) {
    }
}
