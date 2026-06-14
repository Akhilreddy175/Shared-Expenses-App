package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import com.sharedexpenses.expense.Expense;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class DuplicateExpenseDetector implements AnomalyDetector {

    @Override
    public String getName() { return "DuplicateExpenseDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        if (row.getExpenseDate() == null || row.getAmount() == null
                || row.getDescription() == null || row.getPaidByUserId() == null) {
            return List.of(); 
        }

        for (Expense existing : context.getExistingExpenses()) {
            boolean sameDate   = existing.getExpenseDate().equals(row.getExpenseDate());
            boolean sameAmount = existing.getAmount().compareTo(row.getAmount()) == 0;
            boolean samePayer  = existing.getPaidBy().equals(row.getPaidByUserId());
            boolean sameDesc   = existing.getDescription()
                    .equalsIgnoreCase(row.getDescription().trim());

            if (sameDate && sameAmount && samePayer && sameDesc) {
                return List.of(new AnomalyReport(
                        getName(),
                        ImportIssueType.DUPLICATE_ROW,
                        IssueSeverity.WARNING,
                        null,
                        null,
                        "Expense '" + row.getDescription() + "' on " + row.getExpenseDate()
                                + " for " + row.getCurrency() + " " + row.getAmount()
                                + " appears to already exist in this group (expense ID: "
                                + existing.getId() + ").",
                        "Open expense #" + existing.getId() + " and verify it is the same. "
                                + "If so, skip this row. If this is a genuine new expense, "
                                + "change the description slightly to distinguish it."
                ));
            }
        }
        return List.of();
    }
}
