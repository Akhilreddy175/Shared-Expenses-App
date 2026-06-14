package com.sharedexpenses.expense.dto;

import com.sharedexpenses.expense.ExpenseParticipant;
import com.sharedexpenses.user.User;

import java.math.BigDecimal;

public class ExpenseParticipantResponse {

    private final Long userId;
    private final String displayName;
    private final String email;
    private final BigDecimal shareAmount;

    private ExpenseParticipantResponse(Long userId, String displayName, String email, BigDecimal shareAmount) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.shareAmount = shareAmount;
    }

    public static ExpenseParticipantResponse from(ExpenseParticipant participant, User user) {
        return new ExpenseParticipantResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                participant.getShareAmount()
        );
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public BigDecimal getShareAmount() { return shareAmount; }
}
