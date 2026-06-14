package com.sharedexpenses.csvimport;

import com.sharedexpenses.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "import_issues")
public class Anomaly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_row_id", nullable = false)
    private Long importRowId;

    @Column(name = "issue_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportIssueType issueType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "raw_value")
    private String rawValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    protected Anomaly() {}

    public Anomaly(Long importRowId, ImportIssueType issueType, IssueSeverity severity,
                   String fieldName, String rawValue, String message) {
        this(importRowId, issueType, severity, fieldName, rawValue, message, null);
    }

    public Anomaly(Long importRowId, ImportIssueType issueType, IssueSeverity severity,
                   String fieldName, String rawValue, String message, String recommendedAction) {
        this.importRowId = importRowId;
        this.issueType = issueType;
        this.severity = severity;
        this.fieldName = fieldName;
        this.rawValue = rawValue;
        this.message = message;
        this.recommendedAction = recommendedAction;
    }

    public Long getId() { return id; }
    public Long getImportRowId() { return importRowId; }
    public ImportIssueType getIssueType() { return issueType; }
    public IssueSeverity getSeverity() { return severity; }
    public String getFieldName() { return fieldName; }
    public String getRawValue() { return rawValue; }
    public String getMessage() { return message; }
    public String getRecommendedAction() { return recommendedAction; }
}
