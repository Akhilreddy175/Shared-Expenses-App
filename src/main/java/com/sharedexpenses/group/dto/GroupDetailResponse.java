package com.sharedexpenses.group.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sharedexpenses.group.Group;

public class GroupDetailResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final String createdByDisplayName;
    private final LocalDateTime createdAt;
    private final List<GroupMemberResponse> activeMembers;

    private GroupDetailResponse(Long id, String name, String description,
                                String createdByDisplayName, LocalDateTime createdAt,
                                List<GroupMemberResponse> activeMembers) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByDisplayName = createdByDisplayName;
        this.createdAt = createdAt;
        this.activeMembers = activeMembers;
    }

    public static GroupDetailResponse from(Group group, String createdByDisplayName,
                                           List<GroupMemberResponse> activeMembers) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                createdByDisplayName,
                group.getCreatedAt(),
                activeMembers
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCreatedByDisplayName() { return createdByDisplayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<GroupMemberResponse> getActiveMembers() { return activeMembers; }
}
