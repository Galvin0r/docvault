package com.pw.docvault.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.model.group.GroupDto;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class GroupIT extends AbstractWebIntegrationIT {

    private final IntegrationSupport support;
    private final ObjectMapper objectMapper;

    @BeforeEach
    void resetState() {
        support.resetState();
    }

    @Test
    void requestOnlyGroupJoinRequestCanBeAcceptedByOwner() throws Exception {
        var owner = support.createUser("owner");
        var member = support.createUser("member");
        var groupId = createGroup(owner, "Research Requests", GroupVisibility.REQUEST_ONLY);

        support.mockMvc().perform(get("/groups/%d/members".formatted(groupId))
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userLogin").value("owner"))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));

        support.mockMvc().perform(post("/groups/%d/members/me".formatted(groupId))
                        .with(user(member)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        var requestResponse = support.mockMvc().perform(get("/groups/%d/requests".formatted(groupId))
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userLogin").value("member"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long requestId = objectMapper.readTree(requestResponse).path("content").get(0).path("id").asLong();

        support.mockMvc().perform(post("/groups/requests/%d/accept".formatted(requestId))
                        .with(user(owner)))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(get("/groups/%d/members/me".formatted(groupId))
                        .with(user(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLogin").value("member"))
                .andExpect(jsonPath("$.role").value("USER"));

        support.mockMvc().perform(get("/groups/%d/requests".formatted(groupId))
                        .with(user(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerAndAdminsCanManageMembersWhileUsersCannot() throws Exception {
        var owner = support.createUser("owner");
        var admin = support.createUser("admin");
        var member = support.createUser("member");
        var outsider = support.createUser("outsider");
        var groupId = createGroup(owner, "Member Managed", GroupVisibility.PRIVATE);

        support.mockMvc().perform(post("/groups/%d/members".formatted(groupId))
                        .with(user(owner))
                        .param("email", admin.getEmail()))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(patch("/groups/%d/members/%d/role".formatted(groupId, admin.getId()))
                        .with(user(owner))
                        .param("role", GroupRole.ADMIN.name()))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(post("/groups/%d/members/%d".formatted(groupId, member.getId()))
                        .with(user(admin)))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(post("/groups/%d/members/%d".formatted(groupId, outsider.getId()))
                        .with(user(member)))
                .andExpect(status().isForbidden());

        support.mockMvc().perform(delete("/groups/%d/members/%d".formatted(groupId, member.getId()))
                        .with(user(admin)))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(get("/groups/%d/members".formatted(groupId))
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].userLogin").value("owner"))
                .andExpect(jsonPath("$.content[1].userLogin").value("admin"))
                .andExpect(jsonPath("$.content[1].role").value("ADMIN"));
    }

    @Test
    void anonymousUsersCannotUseGroupEndpoints() throws Exception {
        support.mockMvc().perform(get("/groups"))
                .andExpect(status().isUnauthorized());

        support.mockMvc().perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GroupDto(null, "Nope", null, GroupVisibility.PUBLIC, null, null, null)
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void visibleGroupSearchAndMembershipsRespectPrivateGroups() throws Exception {
        var owner = support.createUser("owner");
        var viewer = support.createUser("viewer");
        var privateGroup = support.createGroup("Private Lab", GroupVisibility.PRIVATE);
        var publicGroup = support.createGroup("Public Lab", GroupVisibility.PUBLIC);
        var requestOnlyGroup = support.createGroup("Request Lab", GroupVisibility.REQUEST_ONLY);
        support.createMembership(owner, privateGroup, GroupRole.OWNER);
        support.createMembership(owner, publicGroup, GroupRole.OWNER);
        support.createMembership(owner, requestOnlyGroup, GroupRole.OWNER);

        support.mockMvc().perform(get("/groups")
                        .with(user(viewer))
                        .param("name", "Lab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Public Lab", "Request Lab")));

        support.createMembership(viewer, privateGroup, GroupRole.USER);

        support.mockMvc().perform(get("/groups/members")
                        .with(user(viewer))
                        .param("userLogin", "owner")
                        .param("groupName", "Lab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void editVisibilityProcessesPendingRequestsAndDeleteRequiresOwner() throws Exception {
        var owner = support.createUser("owner");
        var requester = support.createUser("requester");
        var admin = support.createUser("admin");
        var groupId = createGroup(owner, "Visibility Managed", GroupVisibility.REQUEST_ONLY);

        support.mockMvc().perform(post("/groups/%d/members/me".formatted(groupId))
                        .with(user(requester)))
                .andExpect(status().isOk());
        support.mockMvc().perform(post("/groups/%d/members/%d".formatted(groupId, admin.getId()))
                        .with(user(owner)))
                .andExpect(status().isNoContent());
        support.mockMvc().perform(patch("/groups/%d/members/%d/role".formatted(groupId, admin.getId()))
                        .with(user(owner))
                        .param("role", GroupRole.ADMIN.name()))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(patch("/groups/%d".formatted(groupId))
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GroupDto(null, "Visibility Managed", "opened", GroupVisibility.PUBLIC, null, null, null)
                        )))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(get("/groups/%d/members/me".formatted(groupId))
                        .with(user(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLogin").value("requester"));

        support.mockMvc().perform(delete("/groups/%d".formatted(groupId))
                        .with(user(admin)))
                .andExpect(status().isForbidden());

        support.mockMvc().perform(delete("/groups/%d".formatted(groupId))
                        .with(user(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    void privateVisibilityRejectsPendingRequestsAndMembersCanLeave() throws Exception {
        var owner = support.createUser("owner");
        var requester = support.createUser("requester");
        var member = support.createUser("member");
        var groupId = createGroup(owner, "Private Transition", GroupVisibility.REQUEST_ONLY);

        support.mockMvc().perform(post("/groups/%d/members/me".formatted(groupId))
                        .with(user(requester)))
                .andExpect(status().isOk());
        support.mockMvc().perform(post("/groups/%d/members/%d".formatted(groupId, member.getId()))
                        .with(user(owner)))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(patch("/groups/%d".formatted(groupId))
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GroupDto(null, null, "closed", GroupVisibility.PRIVATE, null, null, null)
                        )))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(get("/groups/%d/requests/me".formatted(groupId))
                        .with(user(requester)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        support.mockMvc().perform(delete("/groups/%d/leave".formatted(groupId))
                        .with(user(owner)))
                .andExpect(status().isForbidden());
        support.mockMvc().perform(delete("/groups/%d/leave".formatted(groupId))
                        .with(user(member)))
                .andExpect(status().isNoContent());
        support.mockMvc().perform(get("/groups/%d/members/me".formatted(groupId))
                        .with(user(member)))
                .andExpect(status().isNotFound());
    }

    private Long createGroup(org.springframework.security.core.userdetails.UserDetails owner,
                             String name,
                             GroupVisibility visibility) throws Exception {
        var dto = new GroupDto(null, name, name + " description", visibility, null, null, null);
        var response = support.mockMvc().perform(post("/groups")
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.parseLong(response);
    }
}
