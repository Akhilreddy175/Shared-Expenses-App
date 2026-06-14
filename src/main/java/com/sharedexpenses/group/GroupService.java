package com.sharedexpenses.group;

import com.sharedexpenses.AppException;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository memberRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Group createGroup(String name, String description, Long creatorId) {
        Group group = new Group(name.trim(), description != null ? description.trim() : null, creatorId);
        group = groupRepository.save(group);
        memberRepository.save(new GroupMember(group.getId(), creatorId, LocalDate.now()));
        return group;
    }

    public List<Group> getGroupsForUser(Long userId) {
        return memberRepository.findByUserIdAndLeftAtIsNull(userId).stream()
                .map(m -> groupRepository.findById(m.getGroupId()).orElseThrow())
                .collect(Collectors.toList());
    }

    public Group getGroup(Long groupId, Long userId) {
        requireMember(groupId, userId);
        return groupRepository.findById(groupId)
                .orElseThrow(() -> AppException.notFound("Group not found"));
    }

    @Transactional
    public Group updateGroup(Long groupId, String name, String description, Long userId) {
        requireMember(groupId, userId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> AppException.notFound("Group not found"));
        group.setName(name.trim());
        if (description != null) group.setDescription(description.trim());
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        requireMember(groupId, userId);
        groupRepository.deleteById(groupId);
    }

    public List<Map<String, Object>> getActiveMembers(Long groupId, Long userId) {
        requireMember(groupId, userId);
        return buildMemberList(memberRepository.findByGroupIdAndLeftAtIsNull(groupId));
    }

    public List<Map<String, Object>> getMemberHistory(Long groupId, Long userId) {
        requireMember(groupId, userId);
        return buildMemberList(memberRepository.findByGroupId(groupId));
    }

    @Transactional
    public Map<String, Object> addMember(Long groupId, Long targetUserId, LocalDate joinedAt, Long currentUserId) {
        requireMember(groupId, currentUserId);
        if (!groupRepository.existsById(groupId)) throw AppException.notFound("Group not found");
        if (!userRepository.existsById(targetUserId)) throw AppException.notFound("User not found");
        if (memberRepository.existsByGroupIdAndUserIdAndLeftAtIsNull(groupId, targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User is already an active member");
        }
        LocalDate date = joinedAt != null ? joinedAt : LocalDate.now();
        GroupMember m = memberRepository.save(new GroupMember(groupId, targetUserId, date));
        return memberToMap(m);
    }

    @Transactional
    public Map<String, Object> removeMember(Long groupId, Long targetUserId, LocalDate leftAt, Long currentUserId) {
        requireMember(groupId, currentUserId);
        GroupMember m = memberRepository.findByGroupIdAndUserIdAndLeftAtIsNull(groupId, targetUserId)
                .orElseThrow(() -> AppException.notFound("Active membership not found"));
        LocalDate date = leftAt != null ? leftAt : LocalDate.now();
        m.leave(date);
        return memberToMap(memberRepository.save(m));
    }

    public void requireMember(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserIdAndLeftAtIsNull(groupId, userId)) {
            throw new AccessDeniedException("You are not an active member of this group");
        }
    }

    private List<Map<String, Object>> buildMemberList(List<GroupMember> members) {
        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("memberId", m.getId());
            map.put("userId", m.getUserId());
            User u = usersById.get(m.getUserId());
            map.put("displayName", u != null ? u.getDisplayName() : "Unknown");
            map.put("email", u != null ? u.getEmail() : null);
            map.put("joinedAt", m.getJoinedAt());
            map.put("leftAt", m.getLeftAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> memberToMap(GroupMember m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("memberId", m.getId());
        map.put("userId", m.getUserId());
        map.put("groupId", m.getGroupId());
        map.put("joinedAt", m.getJoinedAt());
        map.put("leftAt", m.getLeftAt());
        return map;
    }
}
