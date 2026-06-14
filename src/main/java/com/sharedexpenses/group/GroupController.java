package com.sharedexpenses.group;

import com.sharedexpenses.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    record CreateGroupRequest(@NotBlank String name, String description) {}
    record AddMemberRequest(Long userId, LocalDate joinedAt) {}
    record RemoveMemberRequest(LocalDate leftAt) {}

    @PostMapping
    public ResponseEntity<Group> createGroup(@Valid @RequestBody CreateGroupRequest req,
                                             @AuthenticationPrincipal UserPrincipal me) {
        Group group = groupService.createGroup(req.name(), req.description(), me.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public List<Group> myGroups(@AuthenticationPrincipal UserPrincipal me) {
        return groupService.getGroupsForUser(me.getId());
    }

    @GetMapping("/{id}")
    public Group getGroup(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return groupService.getGroup(id, me.getId());
    }

    @PutMapping("/{id}")
    public Group updateGroup(@PathVariable Long id,
                             @Valid @RequestBody CreateGroupRequest req,
                             @AuthenticationPrincipal UserPrincipal me) {
        return groupService.updateGroup(id, req.name(), req.description(), me.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal me) {
        groupService.deleteGroup(id, me.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public List<Map<String, Object>> activeMembers(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal me) {
        return groupService.getActiveMembers(id, me.getId());
    }

    @GetMapping("/{id}/members/history")
    public List<Map<String, Object>> memberHistory(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal me) {
        return groupService.getMemberHistory(id, me.getId());
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMember(@PathVariable Long id,
                                                         @RequestBody AddMemberRequest req,
                                                         @AuthenticationPrincipal UserPrincipal me) {
        Map<String, Object> result = groupService.addMember(id, req.userId(), req.joinedAt(), me.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Map<String, Object> removeMember(@PathVariable Long id,
                                            @PathVariable Long userId,
                                            @RequestBody(required = false) RemoveMemberRequest req,
                                            @AuthenticationPrincipal UserPrincipal me) {
        LocalDate leftAt = (req != null) ? req.leftAt() : null;
        return groupService.removeMember(id, userId, leftAt, me.getId());
    }
}
