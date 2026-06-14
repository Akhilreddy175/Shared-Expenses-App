package com.sharedexpenses.settlement;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "settlements")
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    protected Settlement() {}

    public Settlement(Long groupId, Long payerId, Long receiverId,
                      BigDecimal amount, LocalDate settlementDate, String note, Long createdBy) {
        this.groupId = groupId;
        this.payerId = payerId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.settlementDate = settlementDate;
        this.note = note;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getPayerId() { return payerId; }
    public Long getReceiverId() { return receiverId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getNote() { return note; }
    public Long getCreatedBy() { return createdBy; }
}
