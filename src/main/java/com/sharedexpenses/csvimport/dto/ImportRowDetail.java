package com.sharedexpenses.csvimport.dto;

import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.ImportRowStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ImportRowDetail {

    private final Long id;
    private final int rowNumber;
    private final String rawData;
    private final ImportRowStatus status;
    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final LocalDate expenseDate;
    private final String paidByName;
    private final String splitType;
    private final String category;
    private final String participantsRaw;
    private final Long createdExpenseId;
    private final List<ImportIssueDetail> issues;

    private ImportRowDetail(Long id, int rowNumber, String rawData, ImportRowStatus status,
                            String description, BigDecimal amount, String currency, LocalDate expenseDate,
                            String paidByName, String splitType, String category, String participantsRaw,
                            Long createdExpenseId, List<ImportIssueDetail> issues) {
        this.id = id;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
        this.status = status;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.paidByName = paidByName;
        this.splitType = splitType;
        this.category = category;
        this.participantsRaw = participantsRaw;
        this.createdExpenseId = createdExpenseId;
        this.issues = issues;
    }

    public static ImportRowDetail from(ImportRow row, List<ImportIssueDetail> issues) {
        return new ImportRowDetail(
                row.getId(), row.getRowNumber(), row.getRawData(), row.getStatus(),
                row.getDescription(), row.getAmount(), row.getCurrency(), row.getExpenseDate(),
                row.getPaidByName(), row.getSplitType(), row.getCategory(), row.getParticipantsRaw(),
                row.getCreatedExpenseId(), issues);
    }

    public Long getId() { return id; }
    public int getRowNumber() { return rowNumber; }
    public String getRawData() { return rawData; }
    public ImportRowStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getPaidByName() { return paidByName; }
    public String getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public String getParticipantsRaw() { return participantsRaw; }
    public Long getCreatedExpenseId() { return createdExpenseId; }
    public List<ImportIssueDetail> getIssues() { return issues; }
}
