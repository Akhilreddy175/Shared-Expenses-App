package com.sharedexpenses.group;

import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.group.dto.*;
import com.sharedexpenses.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        GroupDetailResponse group = groupService.createGroup(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(group, "Group created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupSummaryResponse>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<GroupSummaryResponse> groups = groupService.getMyGroups(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        GroupDetailResponse group = groupService.getGroup(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(group));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        GroupMemberResponse member = groupService.addMember(groupId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(member, "Member added"));
    }

    /**
     * Removes a member by setting their leftAt date.
     * leftAt is an optional query parameter — defaults to today in the service.
     * Example: DELETE /api/groups/1/members/3?leftAt=2026-03-31
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leftAt,
            @AuthenticationPrincipal UserPrincipal principal) {

        GroupMemberResponse member = groupService.removeMember(groupId, userId, leftAt, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(member, "Member removed"));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getActiveMembers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<GroupMemberResponse> members = groupService.getActiveMembers(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    @GetMapping("/{groupId}/members/history")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getMembershipHistory(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<GroupMemberResponse> history = groupService.getMembershipHistory(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
