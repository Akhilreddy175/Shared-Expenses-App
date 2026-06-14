package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;


@Component
public class SettlementAsExpenseDetector implements AnomalyDetector {

    
    private static final Set<String> SETTLEMENT_KEYWORDS = Set.of(
            "paid back", "payback", "pay back", "reimburse", "reimbursed",
            "reimbursement", "settlement", "settled", "transfer", "transferred",
            "repay", "repaid", "repayment", "lent", "borrowed", "returned",
            "gave back", "giving back", "owes me", "owe you", "owed me"
    );

    @Override
    public String getName() { return "SettlementAsExpenseDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        String description = row.getDescription();
        if (description == null || description.isBlank()) return List.of();

        String lowerDesc = description.toLowerCase(Locale.ROOT);
        boolean hasKeyword = SETTLEMENT_KEYWORDS.stream().anyMatch(lowerDesc::contains);
        if (!hasKeyword) return List.of();

        
        int participantCount = 0;
        if (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) {
            participantCount = row.getParticipantIds().split("\\|").length;
        }

        if (participantCount == 2) {
            return List.of(new AnomalyReport(
                    getName(),
                    ImportIssueType.DUPLICATE_ROW, 
                    IssueSeverity.WARNING,
                    "description",
                    description,
                    "Row '" + description + "' looks like a cash settlement between two people, "
                            + "not a shared expense. Recording it as an expense will cause "
                            + "the balance engine to double-count this payment.",
                    "Use POST /api/groups/{groupId}/settlements instead of an expense. "
                            + "If this is genuinely a shared expense (not a debt repayment), "
                            + "rename the description to avoid settlement keywords."
            ));
        }

        return List.of();
    }
}
