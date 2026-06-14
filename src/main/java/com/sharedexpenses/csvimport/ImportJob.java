package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "import_jobs")
public class ImportJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportJobStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "imported_rows", nullable = false)
    private int importedRows;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    protected ImportJob() {}

    public ImportJob(Long groupId, String filename, Long uploadedBy) {
        this.groupId = groupId;
        this.filename = filename;
        this.uploadedBy = uploadedBy;
        this.status = ImportJobStatus.PROCESSING;
        this.totalRows = 0;
        this.validRows = 0;
        this.invalidRows = 0;
        this.importedRows = 0;
    }

    public void markCompleted(int total, int valid, int invalid) {
        this.totalRows = total;
        this.validRows = valid;
        this.invalidRows = invalid;
        this.status = ImportJobStatus.COMPLETED;
    }

    public void markFailed(String message) {
        this.errorMessage = message;
        this.status = ImportJobStatus.FAILED;
    }

    public void recordImportedRow() {
        this.importedRows++;
    }

    public void finaliseConfirm() {
        this.status = (this.importedRows == this.validRows)
                ? ImportJobStatus.IMPORTED
                : ImportJobStatus.PARTIALLY_IMPORTED;
    }

    
    public void updateCounts(int total, int valid, int invalid) {
        this.totalRows = total;
        this.validRows = valid;
        this.invalidRows = invalid;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getFilename() { return filename; }
    public ImportJobStatus getStatus() { return status; }
    public int getTotalRows() { return totalRows; }
    public int getValidRows() { return validRows; }
    public int getInvalidRows() { return invalidRows; }
    public int getImportedRows() { return importedRows; }
    public String getErrorMessage() { return errorMessage; }
    public Long getUploadedBy() { return uploadedBy; }
}
