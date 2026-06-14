package com.sharedexpenses.balance.dto;

import com.sharedexpenses.user.User;

import java.math.BigDecimal;

/**
 * A single user's net financial position in a group.
 *
 * balance = totalPaid - totalShare + settledAmount
 *
 *   totalPaid:     Sum of all expense amounts this user paid on behalf of the group.
 *   totalShare:    Sum of this user's share_amount across all expense_participants rows.
 *   settledAmount: Net effect of settlements. Positive means they've paid out more cash
 *                  than they received (debts are being cleared). Negative means they've
 *                  received more than they paid (their credit is being consumed).
 *
 *   Positive balance → this person is still owed money (creditor).
 *   Negative balance → this person still owes money (debtor).
 *   Zero balance     → fully settled.
 */
public class UserBalanceResponse {

    private final Long userId;
    private final String displayName;
    private final BigDecimal totalPaid;
    private final BigDecimal totalShare;
    private final BigDecimal settledAmount;
    private final BigDecimal balance;

    private UserBalanceResponse(Long userId, String displayName,
                                BigDecimal totalPaid, BigDecimal totalShare,
                                BigDecimal settledAmount) {
        this.userId = userId;
        this.displayName = displayName;
        this.totalPaid = totalPaid;
        this.totalShare = totalShare;
        this.settledAmount = settledAmount;
        this.balance = totalPaid.subtract(totalShare).add(settledAmount);
    }

    public static UserBalanceResponse of(Long userId, User user,
                                         BigDecimal totalPaid, BigDecimal totalShare,
                                         BigDecimal settledAmount) {
        return new UserBalanceResponse(userId, user.getDisplayName(), totalPaid, totalShare, settledAmount);
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getTotalShare() { return totalShare; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public BigDecimal getBalance() { return balance; }
}
