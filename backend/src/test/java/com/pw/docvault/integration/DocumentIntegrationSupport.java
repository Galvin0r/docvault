package com.pw.docvault.integration;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentAccess;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.model.enums.AccessPermission;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.repository.security.RoleRepository;
import com.pw.docvault.service.document.DocumentIndexWorker;
import com.pw.docvault.service.document.DocumentProcessingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Component
@RequiredArgsConstructor
class DocumentIntegrationSupport {

    private final WebApplicationContext webApplicationContext;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAccessRepository documentAccessRepository;
    private final DocumentIndexJobRepository documentIndexJobRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final DocumentFragmentRepository documentFragmentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentIndexWorker documentIndexWorker;
    private final PasswordEncoder passwordEncoder;
    private final DocumentProcessingClient documentProcessingClient;

    private MockMvc mockMvc;

    MockMvc mockMvc() {
        return mockMvc;
    }

    DocumentIndexWorker documentIndexWorker() {
        return documentIndexWorker;
    }

    Document findDocument(Long documentId) {
        return documentRepository.findWithOwnerById(documentId).orElseThrow();
    }

    boolean documentExists(Long documentId) {
        return documentRepository.existsById(documentId);
    }

    DocumentIndexJob createJob(Document document, DocumentSyncOperation operation) {
        var job = new DocumentIndexJob();
        job.setDocument(document);
        job.setAttempts((short) 0);
        job.setOperation(operation);
        job.setStatus(com.pw.docvault.model.enums.DocumentIndexJobStatus.PENDING);
        return documentIndexJobRepository.saveAndFlush(job);
    }

    List<DocumentIndexJob> jobsForDocument(Document document) {
        return documentIndexJobRepository.findAll().stream()
                .filter(job -> job.getDocument().getId().equals(document.getId()))
                .toList();
    }

    void resetState() throws InterruptedException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        jdbcTemplate.execute("""
                TRUNCATE document_index_jobs, document_access, group_membership, group_join_requests,
                         documents, groups, activation_tokens, refresh_tokens, password_reset_tokens, users
                RESTART IDENTITY CASCADE
                """);

        var indexOps = elasticsearchOperations.indexOps(DocumentFragment.class);
        if (indexOps.exists()) {
            indexOps.delete();
            awaitIndexDeleted(indexOps);
        }
        indexOps.createWithMapping();
        awaitIndexReady();
        refreshIndex();
    }

    User createUser(String login) {
        var role = roleRepository.findByName("USER").orElseThrow();
        var user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.test");
        user.setPassword(passwordEncoder.encode("secret"));
        user.setEnabled(true);
        user.setRoles(List.of(role));
        return userRepository.saveAndFlush(user);
    }

    Group createGroup(String name) {
        var group = new Group();
        group.setName(name);
        group.setDescription(name + " docs");
        group.setVisibility(GroupVisibility.PRIVATE);
        return groupRepository.saveAndFlush(group);
    }

    GroupMembership createMembership(User user, Group group) {
        var membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(GroupRole.USER);
        return groupMembershipRepository.saveAndFlush(membership);
    }

    Document createIndexedDocument(User owner, String title, DocumentVisibility visibility, Instant createdAt) {
        return createDocument(owner, title, visibility, DocumentStatus.INDEXED, createdAt);
    }

    Document createDocument(User owner, String title, DocumentVisibility visibility,
                            DocumentStatus status, Instant createdAt) {
        var document = new Document();
        document.setTitle(title);
        document.setDescription(title + " description");
        document.setOriginalFilename(title.toLowerCase().replace(' ', '-') + ".txt");
        document.setPath("user_%d/%s".formatted(owner.getId(), UUID.randomUUID()));
        document.setMimeType("text/plain");
        document.setVisibility(visibility);
        document.setOwner(owner);
        document.setSizeBytes(64L);
        document.setStatus(status);
        var saved = documentRepository.saveAndFlush(document);
        jdbcTemplate.update(
                "UPDATE documents SET created = ? WHERE id = ?",
                Timestamp.from(createdAt),
                saved.getId()
        );
        saved.setCreated(createdAt);
        return saved;
    }

    DocumentAccess grantUser(Document document, User user) {
        var access = new DocumentAccess();
        access.setDocument(document);
        access.setUser(user);
        access.setPermission(AccessPermission.READ);
        return documentAccessRepository.saveAndFlush(access);
    }

    DocumentAccess grantGroup(Document document, Group group) {
        var access = new DocumentAccess();
        access.setDocument(document);
        access.setGroup(group);
        access.setPermission(AccessPermission.READ);
        return documentAccessRepository.saveAndFlush(access);
    }

    void indexFragment(Document document, String content) {
        indexFragment(document, 0, content, List.of(), List.of());
    }

    void indexFragment(Document document, String content, List<Long> permittedUsers, List<Long> permittedGroups) {
        indexFragment(document, 0, content, permittedUsers, permittedGroups);
    }

    void indexFragment(Document document, int fragmentOrder, String content,
                       List<Long> permittedUsers, List<Long> permittedGroups) {
        var fragment = new DocumentFragment();
        fragment.setId(UUID.randomUUID().toString());
        fragment.setDocumentId(document.getId());
        fragment.setFragmentOrder(fragmentOrder);
        fragment.setTitle(document.getTitle());
        fragment.setOwnerId(document.getOwner().getId());
        fragment.setOwnerLogin(document.getOwner().getLogin());
        fragment.setCreatedAt(document.getCreated());
        fragment.setVisibility(document.getVisibility());
        fragment.setPermittedUserIds(List.copyOf(permittedUsers));
        fragment.setPermittedGroupIds(List.copyOf(permittedGroups));
        fragment.setContent(content);
        documentFragmentRepository.save(fragment);
    }

    long indexedFragmentCount() {
        return documentFragmentRepository.count();
    }

    void processorReturns(String content) {
        processorReturnsFragments(content);
    }

    void processorReturnsFragments(String... contents) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var consumer = (java.util.function.Consumer<DocumentFragment>) invocation.getArgument(2);
            for (int i = 0; i < contents.length; i++) {
                var fragment = new DocumentFragment();
                fragment.setFragmentOrder(i);
                fragment.setContent(contents[i]);
                consumer.accept(fragment);
            }
            return null;
        }).when(documentProcessingClient).processFromSignedUrl(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    void refreshIndex() {
        elasticsearchOperations.indexOps(DocumentFragment.class).refresh();
    }

    private void awaitIndexDeleted(IndexOperations indexOps) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (!indexOps.exists()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out waiting for Elasticsearch index deletion");
    }

    private void awaitIndexReady() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            var health = elasticsearchOperations.cluster().health();
            if (!health.isTimedOut()
                    && health.getInitializingShards() == 0
                    && health.getUnassignedShards() == 0) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out waiting for Elasticsearch shards to become active");
    }
}
