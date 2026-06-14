package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import com.sharedexpenses.expense.SplitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


@Component
public class InvalidSplitDetector implements AnomalyDetector {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @Override
    public String getName() { return "InvalidSplitDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        List<AnomalyReport> reports = new ArrayList<>();
        String splitTypeStr = row.getSplitType();
        if (splitTypeStr == null) return List.of();

        SplitType splitType;
        try {
            splitType = SplitType.valueOf(splitTypeStr);
        } catch (IllegalArgumentException e) {
            return List.of(); 
        }

        if (splitType == SplitType.EQUAL) return List.of(); 

        
        if (row.getParticipantValues() == null || row.getParticipantValues().isBlank()) {
            reports.add(new AnomalyReport(
                    getName(),
                    ImportIssueType.PARTICIPANT_VALUE_MISSING,
                    IssueSeverity.ERROR,
                    "participants",
                    null,
                    "Split type " + splitType + " requires participant values "
                            + "(format: Name:value|Name:value), but none were provided.",
                    "Add values to each participant. Example for " + splitType
                            + ": \"Aisha:600.00|Rohan:700.00|Priya:500.00\""
            ));
            return reports;
        }

        List<BigDecimal> values = parseValues(row.getParticipantValues(), reports);
        if (values == null) return reports; 

        switch (splitType) {
            case PERCENTAGE -> validatePercentages(values, reports);
            case EXACT      -> validateExact(values, row.getAmount(), reports);
            case SHARES     -> validateShares(values, reports);
        }

        return reports;
    }

    

    private void validatePercentages(List<BigDecimal> values, List<AnomalyReport> reports) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = sum.subtract(HUNDRED).abs();
        if (diff.compareTo(TOLERANCE) > 0) {
            reports.add(new AnomalyReport(
                    getName(),
                    ImportIssueType.INVALID_AMOUNT,
                    IssueSeverity.ERROR,
                    "participants",
                    sum.toPlainString() + "%",
                    "PERCENTAGE split values must sum to 100%, but they sum to "
                            + sum.toPlainString() + "% (difference: "
                            + diff.toPlainString() + "%).",
                    "Adjust the participant percentages so they total exactly 100% and re-upload."
            ));
        }
    }

    private void validateExact(List<BigDecimal> values, BigDecimal expenseAmount,
                               List<AnomalyReport> reports) {
        if (expenseAmount == null) return;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = sum.subtract(expenseAmount).abs();
        if (diff.compareTo(TOLERANCE) > 0) {
            reports.add(new AnomalyReport(
                    getName(),
                    ImportIssueType.INVALID_AMOUNT,
                    IssueSeverity.ERROR,
                    "participants",
                    sum.toPlainString(),
                    "EXACT split amounts sum to " + sum.toPlainString()
                            + " but the expense total is " + expenseAmount.toPlainString()
                            + " (difference: " + diff.toPlainString() + ").",
                    "Ensure all participant amounts add up to the expense total ("
                            + expenseAmount.toPlainString() + ") and re-upload."
            ));
        }
    }

    private void validateShares(List<BigDecimal> values, List<AnomalyReport> reports) {
        for (BigDecimal value : values) {
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                reports.add(new AnomalyReport(
                        getName(),
                        ImportIssueType.INVALID_AMOUNT,
                        IssueSeverity.ERROR,
                        "participants",
                        value.toPlainString(),
                        "SHARES split requires all share values to be positive, "
                                + "but found: " + value.toPlainString() + ".",
                        "Replace zero or negative share values with positive numbers and re-upload. "
                                + "A share of 1 means equal weight; 2 means double weight."
                ));
                return; 
            }
        }
    }

    private List<BigDecimal> parseValues(String participantValues, List<AnomalyReport> reports) {
        String[] parts = participantValues.split("\\|");
        List<BigDecimal> values = new ArrayList<>();
        for (String part : parts) {
            try {
                values.add(new BigDecimal(part.trim()));
            } catch (NumberFormatException e) {
                reports.add(new AnomalyReport(
                        getName(),
                        ImportIssueType.INVALID_AMOUNT,
                        IssueSeverity.ERROR,
                        "participants",
                        part.trim(),
                        "Cannot parse participant value '" + part.trim() + "' as a number.",
                        "Fix the malformed value in the participants column and re-upload."
                ));
                return null; 
            }
        }
        return values;
    }
}
