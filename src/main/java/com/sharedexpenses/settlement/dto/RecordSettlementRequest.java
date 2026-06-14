package com.sharedexpenses.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordSettlementRequest {

    @NotNull(message = "Payer user ID is required")
    private Long payerId;

    @NotNull(message = "Receiver user ID is required")
    private Long receiverId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    // Optional — defaults to today in the service
    // Back-dating is allowed: if the cash changed hands last Tuesday, record it accurately
    private LocalDate settlementDate;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    public Long getPayerId() { return payerId; }
    public Long getReceiverId() { return receiverId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getNote() { return note; }

    public void setPayerId(Long payerId) { this.payerId = payerId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public void setNote(String note) { this.note = note; }
}
