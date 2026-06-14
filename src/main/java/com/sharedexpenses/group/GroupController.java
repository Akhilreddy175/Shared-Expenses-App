package com.sharedexpenses.group;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.group.dto.AddMemberRequest;
import com.sharedexpenses.group.dto.CreateGroupRequest;
import com.sharedexpenses.group.dto.GroupDetailResponse;
import com.sharedexpenses.group.dto.GroupMemberResponse;
import com.sharedexpenses.group.dto.GroupSummaryResponse;
import com.sharedexpenses.security.UserPrincipal;

import jakarta.validation.Valid;

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
