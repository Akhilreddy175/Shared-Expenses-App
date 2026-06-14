package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "import_rows")
public class ImportRow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    
    @Column(name = "raw_data", nullable = false, columnDefinition = "TEXT")
    private String rawData;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportRowStatus status;

    
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private String currency;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "paid_by_name")
    private String paidByName;

    @Column(name = "paid_by_user_id")
    private Long paidByUserId;

    @Column(name = "split_type")
    private String splitType;

    private String category;

    
    @Column(name = "participants_raw", columnDefinition = "TEXT")
    private String participantsRaw;

    
    @Column(name = "participant_ids", columnDefinition = "TEXT")
    private String participantIds;

    
    @Column(name = "participant_values", columnDefinition = "TEXT")
    private String participantValues;

    @Column(name = "created_expense_id")
    private Long createdExpenseId;

    protected ImportRow() {}

    public ImportRow(Long importJobId, int rowNumber, String rawData) {
        this.importJobId = importJobId;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
        this.status = ImportRowStatus.VALID; 
    }

    public void markImported(Long expenseId) {
        this.status = ImportRowStatus.IMPORTED;
        this.createdExpenseId = expenseId;
    }

    public Long getId() { return id; }
    public Long getImportJobId() { return importJobId; }
    public int getRowNumber() { return rowNumber; }
    public String getRawData() { return rawData; }
    public ImportRowStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getPaidByName() { return paidByName; }
    public Long getPaidByUserId() { return paidByUserId; }
    public String getSplitType() { return splitType; }
    public String getCategory() { return category; }
    public String getParticipantsRaw() { return participantsRaw; }
    public String getParticipantIds() { return participantIds; }
    public String getParticipantValues() { return participantValues; }
    public Long getCreatedExpenseId() { return createdExpenseId; }

    public void setStatus(ImportRowStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setPaidByName(String paidByName) { this.paidByName = paidByName; }
    public void setPaidByUserId(Long paidByUserId) { this.paidByUserId = paidByUserId; }
    public void setSplitType(String splitType) { this.splitType = splitType; }
    public void setCategory(String category) { this.category = category; }
    public void setParticipantsRaw(String participantsRaw) { this.participantsRaw = participantsRaw; }
    public void setParticipantIds(String participantIds) { this.participantIds = participantIds; }
    public void setParticipantValues(String participantValues) { this.participantValues = participantValues; }
}
