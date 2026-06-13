package com.sharedexpenses.user.dto;

import java.time.LocalDateTime;

import com.sharedexpenses.user.User;

public class UserResponse {

    private final Long id;
    private final String email;
    private final String displayName;
    private final LocalDateTime createdAt;

    private UserResponse(Long id, String email, String displayName, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
