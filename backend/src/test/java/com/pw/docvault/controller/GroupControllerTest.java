package com.pw.docvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.group.GroupDto;
import com.pw.docvault.model.group.GroupJoinRequestDto;
import com.pw.docvault.model.group.GroupMembershipDto;
import com.pw.docvault.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class GroupControllerTest {

    @Mock
    private GroupService groupService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        var controller = new GroupController(groupService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                 .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                                 .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                                 .build();
    }

    @Test
    void createReturns201WithIdAndDelegates() throws Exception {
        var name = "groupName";
        var description = "groupDescription";
        GroupDto dto = new GroupDto(null, name, description, null, null, null, null, null);
        when(groupService.create(eq(name), eq(description), isNull())).thenReturn(123L);

        mockMvc.perform(post("/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isCreated())
               .andExpect(content().string("123"));

        verify(groupService).create(name, description, null);
    }

    @Test
    void deleteReturns204AndDelegates() throws Exception {
        var groupId = 7L;
        mockMvc.perform(delete("/groups/{id}", groupId))
               .andExpect(status().isNoContent());

        verify(groupService).delete(groupId);
    }

    @Test
    void editReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        GroupDto dto = new GroupDto(groupId, "N", "D", null, null, null, null, null);

        mockMvc.perform(patch("/groups/{id}", groupId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isNoContent());

        verify(groupService).edit(groupId, dto);
    }

    @Test
    void getReturns200AndDelegates() throws Exception {
        var groupId = 2L;
        GroupDto dto = new GroupDto(groupId, "A", "B", null, null, null, null, null);
        when(groupService.get(groupId)).thenReturn(dto);

        mockMvc.perform(get("/groups/{id}", groupId))
               .andExpect(status().isOk());

        verify(groupService).get(groupId);
    }

    // find

    @Test
    void findWithoutNameDefaultsToEmptyString() throws Exception {
        Page<GroupDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(groupService.findByName(eq(""), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/groups")
                                .param("page", "0")
                                .param("size", "5"))
               .andExpect(status().isOk());

        verify(groupService).findByName(eq(""), any(Pageable.class));
    }

    @Test
    void findWithNameForwardsValue() throws Exception {
        Page<GroupDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(groupService.findByName(eq("abc"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/groups")
                                .param("name", "abc")
                                .param("page", "1")
                                .param("size", "3"))
               .andExpect(status().isOk());

        verify(groupService).findByName(eq("abc"), any(Pageable.class));
    }

    @Test
    void leaveGroupReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        mockMvc.perform(delete("/groups/{id}/leave", groupId))
               .andExpect(status().isNoContent());

        verify(groupService).leave(groupId);
    }

    @Test
    void changeRoleReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        var userId = 3L;
        mockMvc.perform(patch("/groups/{id}/members/{userId}/role", groupId, userId)
                                .param("role", GroupRole.ADMIN.name()))
               .andExpect(status().isNoContent());

        verify(groupService).changeRole(groupId, userId, GroupRole.ADMIN);
    }

    @Test
    void removeMemberReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        var userId = 3L;
        mockMvc.perform(delete("/groups/{id}/members/{userId}", groupId, userId))
               .andExpect(status().isNoContent());

        verify(groupService).removeMember(groupId, userId);
    }

    @Test
    void addMemberByIdReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        var userId = 6L;
        mockMvc.perform(post("/groups/{id}/members/{userId}", groupId, userId))
               .andExpect(status().isNoContent());

        verify(groupService).addMember(groupId, userId);
    }

    @Test
    void addMemberByEmailReturns204AndDelegates() throws Exception {
        var groupId = 2L;
        mockMvc.perform(post("/groups/{id}/members", groupId)
                                .param("email", "bob@ex.com"))
               .andExpect(status().isNoContent());

        verify(groupService).addMember(groupId, "bob@ex.com");
    }

    @Test
    void joinReturns200AndDelegates() throws Exception {
        var groupId = 2L;
        GroupMembershipDto membership = new GroupMembershipDto(1L, 7L, "me", groupId, "G", GroupRole.USER);
        when(groupService.join(groupId)).thenReturn(membership);

        mockMvc.perform(post("/groups/{id}/members/me", groupId))
               .andExpect(status().isOk());

        verify(groupService).join(groupId);
    }

    @Test
    void findGroupMembersSortsByIdAsc() throws Exception {
        var groupId = 9L;
        Page<GroupMembershipDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(groupService.findGroupMembers(eq(groupId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/groups/{id}/members", groupId)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "name,desc"))
               .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(groupService).findGroupMembers(eq(groupId), captor.capture());
        Pageable used = captor.getValue();

        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(10);
        assertThat(used.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void getMembershipReturns200AndDelegates() throws Exception {
        var groupId = 2L;
        GroupMembershipDto m = new GroupMembershipDto(1L, 7L, "me", groupId, "G", GroupRole.USER);
        when(groupService.getMembership(groupId)).thenReturn(m);

        mockMvc.perform(get("/groups/{id}/members/me", groupId))
               .andExpect(status().isOk());

        verify(groupService).getMembership(groupId);
    }

    @Test
    void getRequestReturns200AndDelegates() throws Exception {
        var groupId = 2L;
        GroupJoinRequestDto dto = new GroupJoinRequestDto(5L, 7L, "me", groupId, "G", null, null);
        when(groupService.getRequest(groupId)).thenReturn(dto);

        mockMvc.perform(get("/groups/{id}/requests/me", groupId))
               .andExpect(status().isOk());

        verify(groupService).getRequest(groupId);
    }

    @Test
    void findJoinRequestsReturns200AndDelegates() throws Exception {
        var id = 4L;
        Page<GroupJoinRequestDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(groupService.findPendingRequests(eq(id), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/groups/{id}/requests", id)
                                .param("page", "2")
                                .param("size", "5"))
               .andExpect(status().isOk());

        verify(groupService).findPendingRequests(eq(id), any(Pageable.class));
    }

    @Test
    void acceptRequestReturns204AndDelegates() throws Exception {
        var id = 11L;
        mockMvc.perform(post("/groups/requests/{requestId}/accept", id))
               .andExpect(status().isNoContent());

        verify(groupService).acceptRequest(id);
    }

    @Test
    void rejectRequestReturns204AndDelegates() throws Exception {
        var id = 12L;
        mockMvc.perform(post("/groups/requests/{requestId}/reject", id))
               .andExpect(status().isNoContent());

        verify(groupService).rejectRequest(id);
    }
}
