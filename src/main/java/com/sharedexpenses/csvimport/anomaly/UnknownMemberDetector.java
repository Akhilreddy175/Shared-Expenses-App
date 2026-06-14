package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class UnknownMemberDetector implements AnomalyDetector {

    @Override
    public String getName() { return "UnknownMemberDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        List<AnomalyReport> reports = new ArrayList<>();

        
        if (row.getPaidByUserId() != null
                && !context.getAllMemberUserIds().contains(row.getPaidByUserId())) {
            reports.add(new AnomalyReport(
                    getName(),
                    ImportIssueType.UNKNOWN_USER,
                    IssueSeverity.ERROR,
                    "paid_by",
                    row.getPaidByName(),
                    "User ID " + row.getPaidByUserId() + " ('" + row.getPaidByName()
                            + "') is not a member of this group.",
                    "Add '" + row.getPaidByName() + "' to the group first, "
                            + "then re-upload the CSV."
            ));
        }

        
        if (row.getParticipantIds() != null && !row.getParticipantIds().isBlank()) {
            for (String idStr : row.getParticipantIds().split("\\|")) {
                try {
                    Long userId = Long.parseLong(idStr.trim());
                    if (!context.getAllMemberUserIds().contains(userId)) {
                        reports.add(new AnomalyReport(
                                getName(),
                                ImportIssueType.UNKNOWN_USER,
                                IssueSeverity.ERROR,
                                "participants",
                                idStr.trim(),
                                "Participant user ID " + userId
                                        + " is not a member of this group.",
                                "Add this user to the group, then re-upload the CSV."
                        ));
                    }
                } catch (NumberFormatException ignored) {
                    
                }
            }
        }

        return reports;
    }
}
