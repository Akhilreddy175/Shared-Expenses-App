package com.sharedexpenses.group.dto;

import com.sharedexpenses.group.Group;

import java.time.LocalDateTime;

/**
 * Lightweight group summary — used in the "my groups" list where we don't need
 * to send full member details for every group.
 */
public class GroupSummaryResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final int activeMemberCount;
    private final LocalDateTime createdAt;

    private GroupSummaryResponse(Long id, String name, String description,
                                 int activeMemberCount, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.activeMemberCount = activeMemberCount;
        this.createdAt = createdAt;
    }

    public static GroupSummaryResponse from(Group group, int activeMemberCount) {
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                activeMemberCount,
                group.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getActiveMemberCount() { return activeMemberCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
