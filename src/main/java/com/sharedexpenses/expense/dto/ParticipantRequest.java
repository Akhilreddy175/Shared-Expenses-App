package com.sharedexpenses.expense.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public class ParticipantRequest {

    @NotNull(message = "User ID is required for each participant")
    private Long userId;

    private BigDecimal shareAmount;

    private BigDecimal percentage;

    private BigDecimal shares;

    public Long getUserId() { return userId; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getShares() { return shares; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public void setShares(BigDecimal shares) { this.shares = shares; }
}
