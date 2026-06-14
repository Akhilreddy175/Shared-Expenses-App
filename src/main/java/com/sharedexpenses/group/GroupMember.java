package com.sharedexpenses.group;

import java.time.LocalDate;

import com.sharedexpenses.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_members")
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    protected GroupMember() {}

    public GroupMember(Long groupId, Long userId, LocalDate joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.leftAt = null;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public LocalDate getJoinedAt() { return joinedAt; }
    public LocalDate getLeftAt() { return leftAt; }

    public boolean isActive() {
        return leftAt == null;
    }

    public boolean isActiveOn(LocalDate date) {
        return !joinedAt.isAfter(date) && (leftAt == null || leftAt.isAfter(date));
    }

    public void leave(LocalDate date) {
        this.leftAt = date;
    }
}
