package com.sharedexpenses.csvimport.dto;

import com.sharedexpenses.csvimport.ImportIssue;
import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.IssueSeverity;

public class ImportIssueDetail {

    private final ImportIssueType issueType;
    private final IssueSeverity severity;
    private final String fieldName;
    private final String rawValue;
    private final String message;

    private ImportIssueDetail(ImportIssueType issueType, IssueSeverity severity,
                              String fieldName, String rawValue, String message) {
        this.issueType = issueType;
        this.severity = severity;
        this.fieldName = fieldName;
        this.rawValue = rawValue;
        this.message = message;
    }

    public static ImportIssueDetail from(ImportIssue issue) {
        return new ImportIssueDetail(
                issue.getIssueType(), issue.getSeverity(),
                issue.getFieldName(), issue.getRawValue(), issue.getMessage());
    }

    public ImportIssueType getIssueType() { return issueType; }
    public IssueSeverity getSeverity() { return severity; }
    public String getFieldName() { return fieldName; }
    public String getRawValue() { return rawValue; }
    public String getMessage() { return message; }
}
