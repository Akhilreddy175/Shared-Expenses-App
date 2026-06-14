package com.sharedexpenses.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.SplitType;

public class ExpenseResponse {

    private final Long id;
    private final Long groupId;
    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final LocalDate expenseDate;
    private final Long paidByUserId;
    private final String paidByDisplayName;
    private final SplitType splitType;
    private final String category;
    private final LocalDateTime createdAt;
    private final List<ExpenseParticipantResponse> participants;

    private ExpenseResponse(Long id, Long groupId, String description, BigDecimal amount,
                            String currency, LocalDate expenseDate, Long paidByUserId,
                            String paidByDisplayName, SplitType splitType, String category,
                            LocalDateTime createdAt, List<ExpenseParticipantResponse> participants) {
        this.id = id;
        this.groupId = groupId;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.paidByUserId = paidByUserId;
        this.paidByDisplayName = paidByDisplayName;
        this.splitType = splitType;
        this.category = category;
        this.createdAt = createdAt;
        this.participants = participants;
    }

    public static ExpenseResponse from(Expense expense, String paidByDisplayName,
                                        List<ExpenseParticipantResponse> participants) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroupId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getExpenseDate(),
                expense.getPaidBy(),
                paidByDisplayName,
                expense.getSplitType(),
                expense.getCategory(),
                expense.getCreatedAt(),
                participants
        );
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Long getPaidByUserId() { return paidByUserId; }
    public String getPaidByDisplayName() { return paidByDisplayName; }
    public SplitType getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ExpenseParticipantResponse> getParticipants() { return participants; }
}
