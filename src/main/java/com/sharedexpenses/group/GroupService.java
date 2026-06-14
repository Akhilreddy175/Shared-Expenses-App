package com.sharedexpenses.group;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.group.dto.AddMemberRequest;
import com.sharedexpenses.group.dto.CreateGroupRequest;
import com.sharedexpenses.group.dto.GroupDetailResponse;
import com.sharedexpenses.group.dto.GroupMemberResponse;
import com.sharedexpenses.group.dto.GroupSummaryResponse;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;

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

        GroupMember creatorMembership = new GroupMember(saved.getId(), creatorId, LocalDate.now());
        groupMemberRepository.save(creatorMembership);

        GroupMemberResponse creatorResponse = GroupMemberResponse.from(creatorMembership, creator);
        return GroupDetailResponse.from(saved, creator.getDisplayName(), List.of(creatorResponse));
    }

    public GroupDetailResponse getGroup(Long groupId, Long currentUserId) {
        Group group = findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        return buildGroupDetailResponse(group);
    }

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

    @Transactional
    public GroupMemberResponse addMember(Long groupId, AddMemberRequest request, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + request.getUserId() + " not found"));

        if (groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNull(groupId, request.getUserId())) {
            throw new ValidationException(targetUser.getDisplayName() + " is already an active member of this group");
        }

        LocalDate joinedAt = request.getJoinedAt() != null ? request.getJoinedAt() : LocalDate.now();
        GroupMember member = new GroupMember(groupId, request.getUserId(), joinedAt);
        GroupMember saved = groupMemberRepository.save(member);

        return GroupMemberResponse.from(saved, targetUser);
    }

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

    public List<GroupMemberResponse> getActiveMembers(Long groupId, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        List<GroupMember> activeMembers = groupMemberRepository.findByGroupIdAndLeftAtIsNull(groupId);
        return buildMemberResponses(activeMembers);
    }

    public List<GroupMemberResponse> getMembershipHistory(Long groupId, Long currentUserId) {
        findGroupOrThrow(groupId);
        requireMembership(groupId, currentUserId);

        List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
        return buildMemberResponses(allMembers);
    }

    private Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
    }

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

    private List<GroupMemberResponse> buildMemberResponses(List<GroupMember> members) {
        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(m -> GroupMemberResponse.from(m, usersById.get(m.getUserId())))
                .collect(Collectors.toList());
    }
}
