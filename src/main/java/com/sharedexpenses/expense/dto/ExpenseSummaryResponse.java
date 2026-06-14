package com.sharedexpenses.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.SplitType;

public class ExpenseSummaryResponse {

    private final Long id;
    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final LocalDate expenseDate;
    private final String paidByDisplayName;
    private final SplitType splitType;
    private final String category;
    private final int participantCount;

    private ExpenseSummaryResponse(Long id, String description, BigDecimal amount, String currency,
                                   LocalDate expenseDate, String paidByDisplayName,
                                   SplitType splitType, String category, int participantCount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.paidByDisplayName = paidByDisplayName;
        this.splitType = splitType;
        this.category = category;
        this.participantCount = participantCount;
    }

    public static ExpenseSummaryResponse from(Expense expense, String paidByDisplayName, int participantCount) {
        return new ExpenseSummaryResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getExpenseDate(),
                paidByDisplayName,
                expense.getSplitType(),
                expense.getCategory(),
                participantCount
        );
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getPaidByDisplayName() { return paidByDisplayName; }
    public SplitType getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public int getParticipantCount() { return participantCount; }
}
