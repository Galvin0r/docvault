package com.pw.docvault.controller;

import com.pw.docvault.model.GroupMembershipDto;
import com.pw.docvault.service.GroupMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("group/membership")
public class GroupMembershipController {

    private final GroupMembershipService groupMembershipService;

    public GroupMembershipController(GroupMembershipService groupMembershipService) {
        this.groupMembershipService = groupMembershipService;
    }

    @GetMapping()
    public ResponseEntity<GroupMembershipDto> getGroupMembership(@RequestParam("userId") Long userId, @RequestParam("groupId") Long groupId) {
        GroupMembershipDto groupMembershipDto = groupMembershipService.retrieveGroupMembership(userId, groupId);
        return ResponseEntity.ok(groupMembershipDto);
    }
}
