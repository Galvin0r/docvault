package com.pw.docvault.controller;

import com.pw.docvault.model.group.GroupDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.group.GroupJoinRequestDto;
import com.pw.docvault.model.group.GroupMembershipDto;
import com.pw.docvault.service.group.GroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody GroupDto dto) {
        Long newId = groupService.create(dto.name(), dto.description(), dto.visibility());
        return ResponseEntity.status(HttpStatus.CREATED).body(newId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> edit(@PathVariable("id") Long id, @RequestBody GroupDto groupDto) {
        groupService.edit(id, groupDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<GroupDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(groupService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<GroupDto>> find(@RequestParam(required = false) String name, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(groupService.findByName(name == null ? "" : name, pageable));
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable("id") Long id) {
        groupService.leave(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @PathVariable Long userId,
                                           @RequestParam GroupRole role) {
        groupService.changeRole(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> addMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.addMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id, @RequestParam String email) {
        groupService.addMember(id, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/me")
    public ResponseEntity<GroupMembershipDto> join(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.join(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Page<GroupMembershipDto>> findGroupMembers(@PathVariable Long id, Pageable pageable) {
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC,
                                                                                              "id"));
        return ResponseEntity.ok(groupService.findGroupMembers(id, sorted));
    }

    @GetMapping("/{id}/members/me")
    public ResponseEntity<GroupMembershipDto> getMembership(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getMembership(id));
    }

    @GetMapping("/{id}/requests/me")
    public ResponseEntity<GroupJoinRequestDto> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getRequest(id));
    }

    @GetMapping("/{id}/requests")
    public ResponseEntity<Page<GroupJoinRequestDto>> findJoinRequests(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(groupService.findPendingRequests(id, pageable));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptRequest(@PathVariable Long requestId) {
        groupService.acceptRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long requestId) {
        groupService.rejectRequest(requestId);
        return ResponseEntity.noContent().build();
    }
}
