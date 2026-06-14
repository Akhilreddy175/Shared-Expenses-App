package com.sharedexpenses.group.dto;

import com.sharedexpenses.group.GroupMember;
import com.sharedexpenses.user.User;

import java.time.LocalDate;

/**
 * Represents one member's entry in a group — includes their user details
 * and the dates they were active. Used in both active member lists and history views.
 */
public class GroupMemberResponse {

    private final Long userId;
    private final String displayName;
    private final String email;
    private final LocalDate joinedAt;
    private final LocalDate leftAt;
    private final boolean active;

    private GroupMemberResponse(Long userId, String displayName, String email,
                                LocalDate joinedAt, LocalDate leftAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.active = leftAt == null;
    }

    public static GroupMemberResponse from(GroupMember member, User user) {
        return new GroupMemberResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                member.getJoinedAt(),
                member.getLeftAt()
        );
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public LocalDate getJoinedAt() { return joinedAt; }
    public LocalDate getLeftAt() { return leftAt; }
    public boolean isActive() { return active; }
}
