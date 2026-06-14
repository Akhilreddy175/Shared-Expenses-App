package com.sharedexpenses.group.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    // joinedAt is optional. If omitted, defaults to today in the service.
    // This allows back-dating: "Sam moved in April 15" — you record it on April 20
    // but set joinedAt to April 15 so historical expenses are attributed correctly.
    private LocalDate joinedAt;

    public Long getUserId() { return userId; }
    public LocalDate getJoinedAt() { return joinedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setJoinedAt(LocalDate joinedAt) { this.joinedAt = joinedAt; }
}
