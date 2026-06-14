package com.sharedexpenses.expense;

import java.math.BigDecimal;

import com.sharedexpenses.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_participants")
public class ExpenseParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "share_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    protected ExpenseParticipant() {}

    public ExpenseParticipant(Long expenseId, Long userId, BigDecimal shareAmount) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.shareAmount = shareAmount;
    }

    public Long getId() { return id; }
    public Long getExpenseId() { return expenseId; }
    public Long getUserId() { return userId; }
    public BigDecimal getShareAmount() { return shareAmount; }
}
