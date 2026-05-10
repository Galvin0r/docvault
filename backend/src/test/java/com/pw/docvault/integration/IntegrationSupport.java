package com.pw.docvault.integration;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.repository.security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Component
@RequiredArgsConstructor
class IntegrationSupport {

    private final WebApplicationContext webApplicationContext;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    MockMvc mockMvc() {
        return mockMvc;
    }

    void resetState() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        jdbcTemplate.execute("""
                TRUNCATE document_index_jobs, document_access, group_membership, group_join_requests,
                         documents, groups, activation_tokens, refresh_tokens, password_reset_tokens, users
                RESTART IDENTITY CASCADE
                """);
        resetSequences();
    }

    private void resetSequences() {
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

    Group createGroup(String name, GroupVisibility visibility) {
        var group = new Group();
        group.setName(name);
        group.setDescription(name + " description");
        group.setVisibility(visibility);
        return groupRepository.saveAndFlush(group);
    }

    GroupMembership createMembership(User user, Group group, GroupRole role) {
        var membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(role);
        return groupMembershipRepository.saveAndFlush(membership);
    }
}
