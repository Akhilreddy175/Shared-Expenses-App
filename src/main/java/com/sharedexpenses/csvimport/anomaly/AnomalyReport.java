package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.IssueSeverity;


public class AnomalyReport {

    private final String detectorName;
    private final ImportIssueType issueType;
    private final IssueSeverity severity;
    private final String fieldName;
    private final String rawValue;
    private final String message;

    
    private final String recommendedAction;

    public AnomalyReport(String detectorName, ImportIssueType issueType, IssueSeverity severity,
                         String fieldName, String rawValue, String message, String recommendedAction) {
        this.detectorName = detectorName;
        this.issueType = issueType;
        this.severity = severity;
        this.fieldName = fieldName;
        this.rawValue = rawValue;
        this.message = message;
        this.recommendedAction = recommendedAction;
    }

    public String getDetectorName() { return detectorName; }
    public ImportIssueType getIssueType() { return issueType; }
    public IssueSeverity getSeverity() { return severity; }
    public String getFieldName() { return fieldName; }
    public String getRawValue() { return rawValue; }
    public String getMessage() { return message; }
    public String getRecommendedAction() { return recommendedAction; }
}
