package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Component
public class MemberNotActiveDetector implements AnomalyDetector {

    @Override
    public String getName() { return "MemberNotActiveDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        LocalDate expenseDate = row.getExpenseDate();
        if (expenseDate == null) return List.of(); 

        List<AnomalyReport> reports = new ArrayList<>();

        
        if (row.getPaidByUserId() != null) {
            checkMembership(row.getPaidByUserId(), row.getPaidByName(),
                    "paid_by", expenseDate, context, reports);
        }

        
        if (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) {
            for (String idStr : row.getParticipantIds().split("\\|")) {
                try {
                    Long userId = Long.parseLong(idStr.trim());
                    checkMembership(userId, "participant " + userId,
                            "participants", expenseDate, context, reports);
                } catch (NumberFormatException ignored) {}
            }
        }

        return reports;
    }

    private void checkMembership(Long userId, String displayName, String fieldName,
                                 LocalDate expenseDate, AnomalyContext context,
                                 List<AnomalyReport> reports) {
        if (!context.wasMemberActiveOn(userId, expenseDate)) {
            reports.add(new AnomalyReport(
                    getName(),
                    ImportIssueType.UNKNOWN_USER,
                    IssueSeverity.ERROR,
                    fieldName,
                    displayName,
                    "'" + displayName + "' was not an active group member on "
                            + expenseDate + ".",
                    "Either correct the expense date to a period when this member was active, "
                            + "or remove them from the participant list. "
                            + "Check the group membership history for exact dates."
            ));
        }
    }
}
