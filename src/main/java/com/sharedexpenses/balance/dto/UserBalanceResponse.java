package com.sharedexpenses.balance.dto;

import com.sharedexpenses.user.User;

import java.math.BigDecimal;

public class UserBalanceResponse {

    private final Long userId;
    private final String displayName;
    private final BigDecimal totalPaid;
    private final BigDecimal totalShare;
    private final BigDecimal balance;

    private UserBalanceResponse(Long userId, String displayName,
                                BigDecimal totalPaid, BigDecimal totalShare) {
        this.userId = userId;
        this.displayName = displayName;
        this.totalPaid = totalPaid;
        this.totalShare = totalShare;
        this.balance = totalPaid.subtract(totalShare);
    }

    public static UserBalanceResponse of(Long userId, User user,
                                         BigDecimal totalPaid, BigDecimal totalShare) {
        return new UserBalanceResponse(userId, user.getDisplayName(), totalPaid, totalShare);
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getTotalShare() { return totalShare; }
    public BigDecimal getBalance() { return balance; }
}
