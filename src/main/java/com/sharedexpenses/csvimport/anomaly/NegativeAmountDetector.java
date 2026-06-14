package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
public class NegativeAmountDetector implements AnomalyDetector {

    @Override
    public String getName() { return "NegativeAmountDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        BigDecimal amount = row.getAmount();

        if (amount == null) {
            return List.of(new AnomalyReport(
                    getName(),
                    ImportIssueType.INVALID_AMOUNT,
                    IssueSeverity.ERROR,
                    "amount",
                    null,
                    "Amount is missing.",
                    "Provide a positive amount in the 'amount' column and re-upload."
            ));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(new AnomalyReport(
                    getName(),
                    ImportIssueType.INVALID_AMOUNT,
                    IssueSeverity.ERROR,
                    "amount",
                    amount.toPlainString(),
                    "Amount must be greater than zero (got: " + amount.toPlainString() + ").",
                    "Replace " + amount.toPlainString() + " with a positive value and re-upload."
            ));
        }

        return List.of();
    }
}
