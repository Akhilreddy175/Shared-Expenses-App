package com.sharedexpenses.group.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private LocalDate joinedAt;

    public Long getUserId() { return userId; }
    public LocalDate getJoinedAt() { return joinedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setJoinedAt(LocalDate joinedAt) { this.joinedAt = joinedAt; }
}
