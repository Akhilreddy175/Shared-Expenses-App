package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.IssueSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;


@Component
public class InvalidCurrencyDetector implements AnomalyDetector {

    
    
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED",
            "CHF", "SEK", "NOK", "DKK", "MYR", "THB", "HKD", "NZD", "ZAR",
            "SAR", "QAR", "KWD", "BHD", "OMR", "BDT", "LKR", "NPR", "PKR"
    );

    @Override
    public String getName() { return "InvalidCurrencyDetector"; }

    @Override
    public List<AnomalyReport> detect(ImportRow row, AnomalyContext context) {
        String currency = row.getCurrency();
        if (currency == null || currency.isBlank()) return List.of();

        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            return List.of(new AnomalyReport(
                    getName(),
                    ImportIssueType.INVALID_CURRENCY,
                    IssueSeverity.WARNING,
                    "currency",
                    currency,
                    "Currency code '" + currency + "' is not in the recognised currency list.",
                    "Verify this is the correct currency. If intentional, proceed. "
                            + "Common alternatives: INR, USD, EUR, GBP. "
                            + "If this is a typo, fix it in the CSV and re-upload."
            ));
        }
        return List.of();
    }
}
