package com.sharedexpenses.csvimport;

import com.sharedexpenses.expense.SplitType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class CsvParserService {

    static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000.00");
    static final int MAX_ROWS = 1000;

    private static final Set<String> REQUIRED_HEADERS = Set.of("date", "description", "amount", "paid_by");

    private final ImportRowRepository rowRepository;
    private final ImportIssueRepository issueRepository;

    public CsvParserService(ImportRowRepository rowRepository, ImportIssueRepository issueRepository) {
        this.rowRepository = rowRepository;
        this.issueRepository = issueRepository;
    }

    
    @Transactional
    public void parseAndStore(ImportJob importJob,
                              InputStream csvStream,
                              Map<String, Long> membersByName,
                              Set<Long> activeMemberIds) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                importJob.markFailed("The uploaded file is empty.");
                return;
            }

            Map<String, Integer> columnIndex = parseHeader(headerLine);
            List<String> missingRequired = REQUIRED_HEADERS.stream()
                    .filter(h -> !columnIndex.containsKey(h))
                    .sorted()
                    .collect(Collectors.toList());

            if (!missingRequired.isEmpty()) {
                importJob.markFailed("Missing required header columns: " + missingRequired +
                        ". Expected at least: date, description, amount, paid_by");
                return;
            }

            
            Set<String> seenDuplicateKeys = new HashSet<>();
            int rowNumber = 1;
            int valid = 0, invalid = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rowNumber++;

                if (rowNumber > MAX_ROWS + 1) {
                    
                    importJob.markFailed("File exceeds the maximum of " + MAX_ROWS +
                            " data rows. Split it into smaller files.");
                    return;
                }

                ParseResult result = parseRow(line, rowNumber, columnIndex,
                        membersByName, activeMemberIds, seenDuplicateKeys);

                ImportRow saved = rowRepository.save(result.row());
                
                List<ImportIssue> boundIssues = result.issues().stream()
                        .map(i -> new ImportIssue(saved.getId(), i.getIssueType(), i.getSeverity(),
                                i.getFieldName(), i.getRawValue(), i.getMessage()))
                        .collect(Collectors.toList());
                issueRepository.saveAll(boundIssues);

                if (result.row().getStatus() == ImportRowStatus.VALID) valid++;
                else invalid++;
            }

            importJob.markCompleted(valid + invalid, valid, invalid);

        } catch (IOException e) {
            importJob.markFailed("Could not read the file: " + e.getMessage());
        }
    }

    

    
    ParseResult parseRow(String rawLine, int rowNumber, Map<String, Integer> columnIndex,
                         Map<String, Long> membersByName, Set<Long> activeMemberIds,
                         Set<String> seenDuplicateKeys) {

        ImportRow row = new ImportRow(0L, rowNumber, rawLine);
        List<ImportIssue> issues = new ArrayList<>();

        String[] fields = parseCsvLine(rawLine);

        
        String dateRaw = safeGet(fields, columnIndex.get("date"));
        LocalDate expenseDate = null;
        if (isBlank(dateRaw)) {
            issues.add(error(row, ImportIssueType.MISSING_REQUIRED_FIELD, "date", dateRaw,
                    "Date is required. Format: YYYY-MM-DD"));
        } else {
            try {
                expenseDate = LocalDate.parse(dateRaw.trim());
                row.setExpenseDate(expenseDate);
                if (expenseDate.isAfter(LocalDate.now())) {
                    issues.add(warning(row, ImportIssueType.FUTURE_DATE, "date", dateRaw,
                            "Expense date " + expenseDate + " is in the future."));
                }
            } catch (DateTimeParseException e) {
                issues.add(error(row, ImportIssueType.INVALID_DATE, "date", dateRaw,
                        "Cannot parse date '" + dateRaw.trim() + "'. Expected format: YYYY-MM-DD"));
            }
        }

        
        String descriptionRaw = safeGet(fields, columnIndex.get("description"));
        if (isBlank(descriptionRaw)) {
            issues.add(error(row, ImportIssueType.MISSING_REQUIRED_FIELD, "description", descriptionRaw,
                    "Description is required."));
        } else {
            row.setDescription(descriptionRaw.trim());
        }

        
        String amountRaw = safeGet(fields, columnIndex.get("amount"));
        BigDecimal amount = null;
        if (isBlank(amountRaw)) {
            issues.add(error(row, ImportIssueType.MISSING_REQUIRED_FIELD, "amount", amountRaw,
                    "Amount is required."));
        } else {
            try {
                amount = new BigDecimal(amountRaw.trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    issues.add(error(row, ImportIssueType.INVALID_AMOUNT, "amount", amountRaw,
                            "Amount must be greater than zero (got: " + amount + ")"));
                    amount = null;
                } else {
                    row.setAmount(amount);
                    if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
                        issues.add(warning(row, ImportIssueType.AMOUNT_TOO_LARGE, "amount", amountRaw,
                                "Amount " + amount + " exceeds ₹" + LARGE_AMOUNT_THRESHOLD +
                                        ". Verify this is correct."));
                    }
                }
            } catch (NumberFormatException e) {
                issues.add(error(row, ImportIssueType.INVALID_AMOUNT, "amount", amountRaw,
                        "Cannot parse amount '" + amountRaw.trim() + "'. Expected a number (e.g., 2400.00)"));
            }
        }

        
        String currencyRaw = safeGet(fields, columnIndex.get("currency"));
        if (isBlank(currencyRaw)) {
            row.setCurrency("INR");
            issues.add(warning(row, ImportIssueType.DEFAULT_APPLIED, "currency", null,
                    "Currency not specified; defaulted to INR."));
        } else {
            String currency = currencyRaw.trim().toUpperCase();
            if (!currency.matches("[A-Z]{3}")) {
                issues.add(error(row, ImportIssueType.INVALID_CURRENCY, "currency", currencyRaw,
                        "Invalid currency '" + currencyRaw.trim() + "'. Must be a 3-letter ISO code (INR, USD, EUR…)"));
            } else {
                row.setCurrency(currency);
            }
        }

        
        String paidByRaw = safeGet(fields, columnIndex.get("paid_by"));
        if (isBlank(paidByRaw)) {
            issues.add(error(row, ImportIssueType.MISSING_REQUIRED_FIELD, "paid_by", paidByRaw,
                    "Paid by is required."));
        } else {
            Long paidByUserId = membersByName.get(paidByRaw.trim().toLowerCase(Locale.ROOT));
            if (paidByUserId == null) {
                issues.add(error(row, ImportIssueType.UNKNOWN_USER, "paid_by", paidByRaw,
                        "No group member found with name '" + paidByRaw.trim() +
                                "'. Check spelling and capitalisation."));
            } else {
                row.setPaidByName(paidByRaw.trim());
                row.setPaidByUserId(paidByUserId);
            }
        }

        
        String splitTypeRaw = safeGet(fields, columnIndex.get("split_type"));
        SplitType splitType = SplitType.EQUAL;
        if (isBlank(splitTypeRaw)) {
            row.setSplitType(SplitType.EQUAL.name());
            issues.add(warning(row, ImportIssueType.DEFAULT_APPLIED, "split_type", null,
                    "Split type not specified; defaulted to EQUAL."));
        } else {
            try {
                splitType = SplitType.valueOf(splitTypeRaw.trim().toUpperCase(Locale.ROOT));
                row.setSplitType(splitType.name());
            } catch (IllegalArgumentException e) {
                issues.add(error(row, ImportIssueType.INVALID_SPLIT_TYPE, "split_type", splitTypeRaw,
                        "Invalid split type '" + splitTypeRaw.trim() +
                                "'. Valid values: EQUAL, EXACT, PERCENTAGE, SHARES"));
            }
        }

        
        String categoryRaw = safeGet(fields, columnIndex.get("category"));
        if (!isBlank(categoryRaw)) {
            row.setCategory(categoryRaw.trim());
        }

        
        String participantsRaw = safeGet(fields, columnIndex.get("participants"));
        parseParticipants(participantsRaw, splitType, membersByName, activeMemberIds, row, issues);

        
        if (expenseDate != null && !isBlank(descriptionRaw) && amount != null) {
            String key = expenseDate + "|" + descriptionRaw.trim().toLowerCase(Locale.ROOT) + "|" + amount;
            if (!seenDuplicateKeys.add(key)) {
                issues.add(warning(row, ImportIssueType.DUPLICATE_ROW, null, null,
                        "A row with the same date, description, and amount already appeared in this CSV. " +
                                "Row " + rowNumber + " may be a duplicate."));
            }
        }

        
        boolean hasErrors = issues.stream()
                .anyMatch(i -> i.getSeverity() == IssueSeverity.ERROR);
        row.setStatus(hasErrors ? ImportRowStatus.INVALID : ImportRowStatus.VALID);

        return new ParseResult(row, issues);
    }

    private void parseParticipants(String participantsRaw, SplitType splitType,
                                   Map<String, Long> membersByName, Set<Long> activeMemberIds,
                                   ImportRow row, List<ImportIssue> issues) {
        if (isBlank(participantsRaw)) {
            if (splitType == SplitType.EQUAL) {
                String ids = activeMemberIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("|"));
                row.setParticipantsRaw("(all " + activeMemberIds.size() + " active members)");
                row.setParticipantIds(ids);
                issues.add(warning(row, ImportIssueType.DEFAULT_APPLIED, "participants", null,
                        "Participants not specified; defaulted to all " + activeMemberIds.size() +
                                " active group members for EQUAL split."));
            } else {
                issues.add(error(row, ImportIssueType.PARTICIPANT_VALUE_MISSING, "participants", null,
                        "Participants with values are required for " + splitType +
                                " split. Format: \"Name:value|Name:value\""));
            }
            return;
        }

        row.setParticipantsRaw(participantsRaw.trim());
        String[] parts = participantsRaw.split("\\|");

        List<Long> resolvedIds = new ArrayList<>();
        List<String> resolvedValues = new ArrayList<>();
        boolean hasErrors = false;

        for (String part : parts) {
            String trimmed = part.trim();
            if (splitType == SplitType.EQUAL) {
                
                Long userId = membersByName.get(trimmed.toLowerCase(Locale.ROOT));
                if (userId == null) {
                    issues.add(error(row, ImportIssueType.UNKNOWN_USER, "participants", trimmed,
                            "No group member found with name '" + trimmed + "'."));
                    hasErrors = true;
                } else {
                    resolvedIds.add(userId);
                }
            } else {
                
                int colon = trimmed.lastIndexOf(':');
                if (colon < 0) {
                    issues.add(error(row, ImportIssueType.PARTICIPANT_VALUE_MISSING, "participants", trimmed,
                            "Expected 'Name:value' format for " + splitType + " split, got: '" + trimmed + "'"));
                    hasErrors = true;
                } else {
                    String name = trimmed.substring(0, colon).trim();
                    String valueStr = trimmed.substring(colon + 1).trim();
                    Long userId = membersByName.get(name.toLowerCase(Locale.ROOT));
                    if (userId == null) {
                        issues.add(error(row, ImportIssueType.UNKNOWN_USER, "participants", name,
                                "No group member found with name '" + name + "'."));
                        hasErrors = true;
                    } else {
                        try {
                            new BigDecimal(valueStr); 
                            resolvedIds.add(userId);
                            resolvedValues.add(valueStr);
                        } catch (NumberFormatException e) {
                            issues.add(error(row, ImportIssueType.INVALID_AMOUNT, "participants", valueStr,
                                    "Cannot parse participant value '" + valueStr + "' for " + name));
                            hasErrors = true;
                        }
                    }
                }
            }
        }

        if (!hasErrors) {
            row.setParticipantIds(resolvedIds.stream()
                    .map(String::valueOf).collect(Collectors.joining("|")));
            if (!resolvedValues.isEmpty()) {
                row.setParticipantValues(String.join("|", resolvedValues));
            }
        }
    }

    

    
    String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); 
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    
    Map<String, Integer> parseHeader(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].toLowerCase(Locale.ROOT).trim(), i);
        }
        return index;
    }

    

    private String safeGet(String[] fields, Integer index) {
        if (index == null || index >= fields.length) return null;
        return fields[index];
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private ImportIssue error(ImportRow row, ImportIssueType type, String field, String rawValue, String msg) {
        return new ImportIssue(row.getImportJobId(), type, IssueSeverity.ERROR, field, rawValue, msg);
    }

    private ImportIssue warning(ImportRow row, ImportIssueType type, String field, String rawValue, String msg) {
        return new ImportIssue(row.getImportJobId(), type, IssueSeverity.WARNING, field, rawValue, msg);
    }

    
    record ParseResult(ImportRow row, List<ImportIssue> issues) {}
}
