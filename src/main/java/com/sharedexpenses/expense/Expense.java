package com.sharedexpenses.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sharedexpenses.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "paid_by", nullable = false)
    private Long paidBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType;

    private String category;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    protected Expense() {}

    public Expense(Long groupId, String description, BigDecimal amount, String currency,
                   LocalDate expenseDate, Long paidBy, SplitType splitType,
                   String category, Long createdBy) {
        this.groupId = groupId;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.category = category;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Long getPaidBy() { return paidBy; }
    public SplitType getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public Long getCreatedBy() { return createdBy; }

    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public void setCategory(String category) { this.category = category; }
}
