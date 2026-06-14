package com.sharedexpenses.csvimport.dto;

import com.sharedexpenses.csvimport.ImportJob;
import com.sharedexpenses.csvimport.ImportJobStatus;

import java.time.LocalDateTime;

public class ImportJobResponse {

    private final Long id;
    private final Long groupId;
    private final String filename;
    private final ImportJobStatus status;
    private final int totalRows;
    private final int validRows;
    private final int invalidRows;
    private final int importedRows;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    private ImportJobResponse(Long id, Long groupId, String filename, ImportJobStatus status,
                              int totalRows, int validRows, int invalidRows, int importedRows,
                              String errorMessage, LocalDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.filename = filename;
        this.status = status;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.importedRows = importedRows;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(), job.getGroupId(), job.getFilename(), job.getStatus(),
                job.getTotalRows(), job.getValidRows(), job.getInvalidRows(), job.getImportedRows(),
                job.getErrorMessage(), job.getCreatedAt());
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
    public LocalDateTime getCreatedAt() { return createdAt; }
}
