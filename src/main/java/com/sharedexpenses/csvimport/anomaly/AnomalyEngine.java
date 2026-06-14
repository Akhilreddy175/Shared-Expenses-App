package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssue;
import com.sharedexpenses.csvimport.ImportIssueRepository;
import com.sharedexpenses.csvimport.ImportJob;
import com.sharedexpenses.csvimport.ImportJobStatus;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.ImportRowRepository;
import com.sharedexpenses.csvimport.ImportRowStatus;
import com.sharedexpenses.csvimport.IssueSeverity;
import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.ExpenseRepository;
import com.sharedexpenses.group.GroupMember;
import com.sharedexpenses.group.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class AnomalyEngine {

    private final List<AnomalyDetector> detectors;
    private final ImportRowRepository rowRepository;
    private final ImportIssueRepository issueRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;

    public AnomalyEngine(List<AnomalyDetector> detectors,
                         ImportRowRepository rowRepository,
                         ImportIssueRepository issueRepository,
                         ExpenseRepository expenseRepository,
                         GroupMemberRepository groupMemberRepository) {
        this.detectors = detectors;
        this.rowRepository = rowRepository;
        this.issueRepository = issueRepository;
        this.expenseRepository = expenseRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    
    @Transactional
    public void analyseAllRows(ImportJob job, List<ImportRow> rows, Long groupId) {
        if (rows.isEmpty()) return;

        AnomalyContext context = buildContext(groupId);

        for (ImportRow row : rows) {
            List<AnomalyReport> reports = new ArrayList<>();
            for (AnomalyDetector detector : detectors) {
                reports.addAll(detector.detect(row, context));
            }

            if (reports.isEmpty()) continue;

            
            List<ImportIssue> issues = reports.stream()
                    .map(r -> new ImportIssue(
                            row.getId(),
                            r.getIssueType(),
                            r.getSeverity(),
                            r.getFieldName(),
                            r.getRawValue(),
                            r.getMessage(),
                            r.getRecommendedAction()))
                    .collect(Collectors.toList());
            issueRepository.saveAll(issues);

            
            if (row.getStatus() == ImportRowStatus.VALID) {
                boolean hasErrors = reports.stream()
                        .anyMatch(r -> r.getSeverity() == IssueSeverity.ERROR);
                if (hasErrors) {
                    row.setStatus(ImportRowStatus.INVALID);
                    rowRepository.save(row);
                }
            }
        }

        
        long valid   = rows.stream().filter(r -> r.getStatus() == ImportRowStatus.VALID).count();
        long invalid = rows.stream().filter(r -> r.getStatus() == ImportRowStatus.INVALID).count();
        job.updateCounts((int) (valid + invalid), (int) valid, (int) invalid);
    }

    
    AnomalyContext buildContext(Long groupId) {
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return new AnomalyContext(groupId, expenses, members);
    }

    public List<AnomalyDetector> getDetectors() { return detectors; }
}
