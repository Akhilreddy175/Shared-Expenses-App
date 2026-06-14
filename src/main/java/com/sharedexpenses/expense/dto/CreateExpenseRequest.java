package com.sharedexpenses.expense.dto;

import com.sharedexpenses.expense.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g., INR, USD)")
    private String currency;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Paid by user ID is required")
    private Long paidBy;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotEmpty(message = "At least one participant is required")
    @Valid
    private List<ParticipantRequest> participants;

    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Long getPaidBy() { return paidBy; }
    public SplitType getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public List<ParticipantRequest> getParticipants() { return participants; }

    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public void setCategory(String category) { this.category = category; }
    public void setParticipants(List<ParticipantRequest> participants) { this.participants = participants; }
}
