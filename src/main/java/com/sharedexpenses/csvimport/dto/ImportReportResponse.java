package com.sharedexpenses.csvimport.dto;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportJob;
import com.sharedexpenses.csvimport.ImportJobStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public class ImportReportResponse {

    private final Long jobId;
    private final String filename;
    private final ImportJobStatus status;
    private final int totalRows;
    private final int validRows;
    private final int invalidRows;
    private final int importedRows;
    private final String errorMessage;
    private final LocalDateTime createdAt;
    private final Map<ImportIssueType, Long> issueSummary;
    private final List<ImportRowDetail> rows;

    private ImportReportResponse(Long jobId, String filename, ImportJobStatus status,
                                 int totalRows, int validRows, int invalidRows, int importedRows,
                                 String errorMessage, LocalDateTime createdAt,
                                 Map<ImportIssueType, Long> issueSummary, List<ImportRowDetail> rows) {
        this.jobId = jobId;
        this.filename = filename;
        this.status = status;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.importedRows = importedRows;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.issueSummary = issueSummary;
        this.rows = rows;
    }

    public static ImportReportResponse of(ImportJob job, Map<ImportIssueType, Long> issueSummary,
                                          List<ImportRowDetail> rows) {
        return new ImportReportResponse(
                job.getId(), job.getFilename(), job.getStatus(),
                job.getTotalRows(), job.getValidRows(), job.getInvalidRows(), job.getImportedRows(),
                job.getErrorMessage(), job.getCreatedAt(), issueSummary, rows);
    }

    public Long getJobId() { return jobId; }
    public String getFilename() { return filename; }
    public ImportJobStatus getStatus() { return status; }
    public int getTotalRows() { return totalRows; }
    public int getValidRows() { return validRows; }
    public int getInvalidRows() { return invalidRows; }
    public int getImportedRows() { return importedRows; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Map<ImportIssueType, Long> getIssueSummary() { return issueSummary; }
    public List<ImportRowDetail> getRows() { return rows; }
}
