package com.sharedexpenses.group;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Records when a user joined and left a group.
 *
 * Why this matters:
 * Sam moved in mid-April. The CSV has expenses dated April 10 that include Sam.
 * If we only store "Sam is a member," we can't determine whether April 10 falls
 * inside or outside Sam's membership. With joinedAt=2026-04-15, we know April 10
 * is before Sam joined — and that expense is anomalous.
 *
 * Similarly, Meera moved out end of March. Any expense after March 31 that lists
 * Meera as a participant is an error — and we can only catch it because we know
 * her leftAt date.
 *
 * Why not hard-delete when someone leaves?
 * If we deleted Meera's membership record when she left, we'd lose the historical
 * record. The import report needs to show "Meera was flagged as inactive in the
 * April 2 expense" — that's only possible if the record still exists.
 */
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

    // Null means the member is still active. A non-null value is their last day.
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
