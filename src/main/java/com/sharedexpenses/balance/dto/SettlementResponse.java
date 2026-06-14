package com.sharedexpenses.balance.dto;

import java.math.BigDecimal;

public class SettlementResponse {

    private final Long fromUserId;
    private final String fromDisplayName;
    private final Long toUserId;
    private final String toDisplayName;
    private final BigDecimal amount;

    private SettlementResponse(Long fromUserId, String fromDisplayName,
                               Long toUserId, String toDisplayName, BigDecimal amount) {
        this.fromUserId = fromUserId;
        this.fromDisplayName = fromDisplayName;
        this.toUserId = toUserId;
        this.toDisplayName = toDisplayName;
        this.amount = amount;
    }

    public static SettlementResponse of(Long fromUserId, String fromDisplayName,
                                        Long toUserId, String toDisplayName, BigDecimal amount) {
        return new SettlementResponse(fromUserId, fromDisplayName, toUserId, toDisplayName, amount);
    }

    public Long getFromUserId() { return fromUserId; }
    public String getFromDisplayName() { return fromDisplayName; }
    public Long getToUserId() { return toUserId; }
    public String getToDisplayName() { return toDisplayName; }
    public BigDecimal getAmount() { return amount; }
}
