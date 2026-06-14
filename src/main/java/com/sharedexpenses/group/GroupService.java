package com.sharedexpenses.group;

import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.group.dto.*;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a group and immediately adds the creator as the first member.
     * The creator's joinedAt is today — they're starting the group now.
     */
    @Transactional
    public GroupDetailResponse createGroup(CreateGroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", creatorId));

        Group group = new Group(
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                creatorId
        );
        Group saved = groupRepository.save(group);

        // Add creator as the first member
        GroupMember creatorMembership = new GroupMember(saved.getId(), creatorId, LocalDate.now());
        groupMemberRepository.save(creatorMembership);

        GroupMemberResponse creatorResponse = GroupMemberResponse.from(creatorMembership, creator);
        return GroupDetailResponse.from(saved, creator.getDisplayName(), List.of(creatorResponse));
    }

    /**
     * Returns full group details including active members.
     * Only available to current group members.
     */
    public GroupDetailResponse getGroup(Long groupId, Long currentUserId) {
        Group group = findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        return buildGroupDetailResponse(group);
    }

    /**
     * Returns all groups the current user is an active member of.
     */
    public List<GroupSummaryResponse> getMyGroups(Long currentUserId) {
        List<GroupMember> myMemberships = groupMemberRepository.findByUserIdAndLeftAtIsNull(currentUserId);

        return myMemberships.stream()
                .map(membership -> {
                    Group group = groupRepository.findById(membership.getGroupId())
                            .orElseThrow(() -> ResourceNotFoundException.of("Group", membership.getGroupId()));
                    int memberCount = groupMemberRepository.findByGroupIdAndLeftAtIsNull(group.getId()).size();
                    return GroupSummaryResponse.from(group, memberCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Adds a new member to the group.
     * joinedAt defaults to today if not provided — but callers can back-date it.
     * For example, "Sam moved in April 15" but we're adding him to the system April 20.
     */
    @Transactional
    public GroupMemberResponse addMember(Long groupId, AddMemberRequest request, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + request.getUserId() + " not found"));

        // Prevent adding someone who is already an active member
        if (groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNull(groupId, request.getUserId())) {
            throw new ValidationException(targetUser.getDisplayName() + " is already an active member of this group");
        }

        LocalDate joinedAt = request.getJoinedAt() != null ? request.getJoinedAt() : LocalDate.now();
        GroupMember member = new GroupMember(groupId, request.getUserId(), joinedAt);
        GroupMember saved = groupMemberRepository.save(member);

        return GroupMemberResponse.from(saved, targetUser);
    }

    /**
     * Records that a member has left the group.
     * leftAt defaults to today if not provided — supports back-dating.
     * The GroupMember record is kept; only leftAt is set.
     */
    @Transactional
    public GroupMemberResponse removeMember(Long groupId, Long targetUserId, LocalDate leftAt, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        GroupMember membership = groupMemberRepository
                .findByGroupIdAndUserIdAndLeftAtIsNull(groupId, targetUserId)
                .orElseThrow(() -> new ValidationException("This user is not an active member of the group"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", targetUserId));

        LocalDate effectiveLeftAt = leftAt != null ? leftAt : LocalDate.now();
        membership.leave(effectiveLeftAt);
        GroupMember saved = groupMemberRepository.save(membership);

        return GroupMemberResponse.from(saved, targetUser);
    }

    /**
     * Returns only the currently active members (leftAt IS NULL).
     */
    public List<GroupMemberResponse> getActiveMembers(Long groupId, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        List<GroupMember> activeMembers = groupMemberRepository.findByGroupIdAndLeftAtIsNull(groupId);
        return buildMemberResponses(activeMembers);
    }

    /**
     * Returns the full membership history — everyone who was ever in the group,
     * with their join and leave dates. Used for the import audit and debugging.
     */
    public List<GroupMemberResponse> getMembershipHistory(Long groupId, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
        return buildMemberResponses(allMembers);
    }

    // --- Helpers ---

    private Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
    }

    /**
     * Authorization check: the current user must be an active member.
     * Using AccessDeniedException so it maps to HTTP 403 via GlobalExceptionHandler.
     */
    public void requireMembership(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNull(groupId, userId)) {
            throw new AccessDeniedException("You are not an active member of this group");
        }
    }

    private GroupDetailResponse buildGroupDetailResponse(Group group) {
        List<GroupMember> activeMembers = groupMemberRepository.findByGroupIdAndLeftAtIsNull(group.getId());
        List<GroupMemberResponse> memberResponses = buildMemberResponses(activeMembers);

        User creator = userRepository.findById(group.getCreatedBy())
                .orElseThrow(() -> ResourceNotFoundException.of("User", group.getCreatedBy()));

        return GroupDetailResponse.from(group, creator.getDisplayName(), memberResponses);
    }

    /**
     * Loads all users for a list of members in a single query (findAllById does SELECT ... WHERE id IN (...))
     * to avoid N+1 queries when a group has many members.
     */
    private List<GroupMemberResponse> buildMemberResponses(List<GroupMember> members) {
        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(m -> GroupMemberResponse.from(m, usersById.get(m.getUserId())))
                .collect(Collectors.toList());
    }
}
