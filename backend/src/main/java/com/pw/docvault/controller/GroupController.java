package com.pw.docvault.controller;

import com.pw.docvault.model.GroupDto;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.service.GroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<Long> create(@RequestParam("name") String groupName,
                                       @RequestParam("description") String description,
                                       @RequestParam("visibility") GroupVisibility visibility) {
        Long newId = groupService.create(groupName, description, visibility);
        return ResponseEntity.status(HttpStatus.CREATED).body(newId);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("id") Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/edit/{id}")
    public ResponseEntity<Void> edit(@PathVariable("id") Long id, @RequestBody GroupDto groupDto) {
        groupService.edit(id, groupDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<GroupDto> get(@PathVariable("id") Long id) {
        GroupDto groupDto = groupService.get(id);
        return ResponseEntity.status(HttpStatus.OK).body(groupDto);
    }

    @GetMapping("/find")
    public ResponseEntity<Page<GroupDto>> find(@RequestParam("name") String name, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").descending());
        Page<GroupDto> groups = groupService.findByName(name, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(groups);
    }
}
