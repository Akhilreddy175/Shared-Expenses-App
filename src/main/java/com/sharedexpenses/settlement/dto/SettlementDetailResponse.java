package com.sharedexpenses.settlement.dto;

import com.sharedexpenses.settlement.Settlement;
import com.sharedexpenses.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SettlementDetailResponse {

    private final Long id;
    private final Long groupId;
    private final Long payerUserId;
    private final String payerDisplayName;
    private final Long receiverUserId;
    private final String receiverDisplayName;
    private final BigDecimal amount;
    private final LocalDate settlementDate;
    private final String note;
    private final LocalDateTime createdAt;

    private SettlementDetailResponse(Long id, Long groupId,
                                     Long payerUserId, String payerDisplayName,
                                     Long receiverUserId, String receiverDisplayName,
                                     BigDecimal amount, LocalDate settlementDate,
                                     String note, LocalDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.payerUserId = payerUserId;
        this.payerDisplayName = payerDisplayName;
        this.receiverUserId = receiverUserId;
        this.receiverDisplayName = receiverDisplayName;
        this.amount = amount;
        this.settlementDate = settlementDate;
        this.note = note;
        this.createdAt = createdAt;
    }

    public static SettlementDetailResponse from(Settlement settlement, User payer, User receiver) {
        return new SettlementDetailResponse(
                settlement.getId(),
                settlement.getGroupId(),
                payer.getId(), payer.getDisplayName(),
                receiver.getId(), receiver.getDisplayName(),
                settlement.getAmount(),
                settlement.getSettlementDate(),
                settlement.getNote(),
                settlement.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getPayerUserId() { return payerUserId; }
    public String getPayerDisplayName() { return payerDisplayName; }
    public Long getReceiverUserId() { return receiverUserId; }
    public String getReceiverDisplayName() { return receiverDisplayName; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
