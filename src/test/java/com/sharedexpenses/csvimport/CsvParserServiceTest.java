package com.sharedexpenses.csvimport;

import com.sharedexpenses.expense.SplitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;


@DisplayName("CsvParserService — parsing logic")
class CsvParserServiceTest {

    private CsvParserService parser;

    
    private Map<String, Long> membersByName;
    private Set<Long> activeMemberIds;
    private Map<String, Integer> standardColumnIndex;

    @BeforeEach
    void setUp() {
        parser = new CsvParserService(null, null); 

        membersByName = new HashMap<>();
        membersByName.put("aisha", 1L);
        membersByName.put("rohan", 2L);
        membersByName.put("priya", 3L);

        activeMemberIds = new LinkedHashSet<>(Set.of(1L, 2L, 3L));

        standardColumnIndex = parser.parseHeader("date,description,amount,currency,paid_by,split_type,category,participants");
    }

    

    @Test
    @DisplayName("splits a plain comma-separated line correctly")
    void parsesSimpleLine() {
        String[] fields = parser.parseCsvLine("2026-04-01,Groceries,2400.00,INR,Aisha,EQUAL,Food,Aisha|Rohan|Priya");

        assertThat(fields).hasSize(8);
        assertThat(fields[0]).isEqualTo("2026-04-01");
        assertThat(fields[2]).isEqualTo("2400.00");
    }

    @Test
    @DisplayName("handles quoted fields that contain commas")
    void parsesQuotedFieldWithComma() {
        String[] fields = parser.parseCsvLine("2026-04-01,\"Rent, utilities\",12000.00,INR,Aisha,EQUAL,,");

        assertThat(fields[1]).isEqualTo("Rent, utilities");
        assertThat(fields[2]).isEqualTo("12000.00");
    }

    @Test
    @DisplayName("handles escaped double-quotes inside quoted fields")
    void parsesEscapedQuotes() {
        String[] fields = parser.parseCsvLine("2026-04-01,\"Tom's \"\"farewell\"\" dinner\",500.00,INR,Aisha,EQUAL,,");

        assertThat(fields[1]).isEqualTo("Tom's \"farewell\" dinner");
    }

    

    @Test
    @DisplayName("builds a case-insensitive column index from headers")
    void buildsColumnIndex() {
        Map<String, Integer> idx = parser.parseHeader("Date,DESCRIPTION,Amount,paid_by");

        assertThat(idx).containsEntry("date", 0);
        assertThat(idx).containsEntry("description", 1);
        assertThat(idx).containsEntry("amount", 2);
        assertThat(idx).containsEntry("paid_by", 3);
    }

    

    @Test
    @DisplayName("valid EQUAL row with all fields produces no ERROR issues")
    void validEqualRowProducesNoErrors() {
        String line = "2026-04-01,Groceries,2400.00,INR,Aisha,EQUAL,Food,Aisha|Rohan|Priya";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.VALID);
        assertThat(result.row().getDescription()).isEqualTo("Groceries");
        assertThat(result.row().getAmount()).isEqualByComparingTo("2400.00");
        assertThat(result.row().getPaidByUserId()).isEqualTo(1L);
        assertThat(result.row().getSplitType()).isEqualTo(SplitType.EQUAL.name());

        boolean hasErrors = result.issues().stream()
                .anyMatch(i -> i.getSeverity() == IssueSeverity.ERROR);
        assertThat(hasErrors).isFalse();
    }

    @Test
    @DisplayName("missing optional fields produce DEFAULT_APPLIED warnings, not errors")
    void missingOptionalFieldsProduceWarnings() {
        
        String line = "2026-04-01,Groceries,2400.00,,Aisha,,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.VALID);
        assertThat(result.row().getCurrency()).isEqualTo("INR"); 

        long warningCount = result.issues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.WARNING).count();
        assertThat(warningCount).isGreaterThanOrEqualTo(2); 
    }

    

    @Test
    @DisplayName("missing required fields produce ERROR issues and mark row INVALID")
    void missingRequiredFieldsMarkRowInvalid() {
        
        String line = "2026-04-01,,,INR,Aisha,EQUAL,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.INVALID);

        long errorCount = result.issues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.ERROR)
                .filter(i -> i.getIssueType() == ImportIssueType.MISSING_REQUIRED_FIELD)
                .count();
        assertThat(errorCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("invalid amount format produces INVALID_AMOUNT error")
    void invalidAmountProducesError() {
        String line = "2026-04-01,Groceries,not-a-number,INR,Aisha,EQUAL,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.INVALID_AMOUNT &&
                i.getSeverity() == IssueSeverity.ERROR);
    }

    @Test
    @DisplayName("unknown paid_by name produces UNKNOWN_USER error")
    void unknownUserProducesError() {
        String line = "2026-04-01,Groceries,2400.00,INR,Meera,EQUAL,,"; 

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.UNKNOWN_USER &&
                i.getFieldName().equals("paid_by"));
    }

    @Test
    @DisplayName("invalid date format produces INVALID_DATE error")
    void invalidDateProducesError() {
        String line = "01/04/2026,Groceries,2400.00,INR,Aisha,EQUAL,,"; 

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.INVALID_DATE);
    }

    @Test
    @DisplayName("invalid split type produces INVALID_SPLIT_TYPE error")
    void invalidSplitTypeProducesError() {
        String line = "2026-04-01,Groceries,2400.00,INR,Aisha,HALVES,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.INVALID_SPLIT_TYPE);
    }

    

    @Test
    @DisplayName("future date produces FUTURE_DATE warning but does not invalidate the row")
    void futureDateProducesWarningOnly() {
        LocalDate future = LocalDate.now().plusMonths(1);
        String line = future + ",Groceries,2400.00,INR,Aisha,EQUAL,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.VALID); 
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.FUTURE_DATE &&
                i.getSeverity() == IssueSeverity.WARNING);
    }

    @Test
    @DisplayName("large amount produces AMOUNT_TOO_LARGE warning but does not invalidate the row")
    void largeAmountProducesWarningOnly() {
        String line = "2026-04-01,Server purchase,999999.00,INR,Aisha,EQUAL,,";

        CsvParserService.ParseResult result = parseRow(line);

        assertThat(result.row().getStatus()).isEqualTo(ImportRowStatus.VALID);
        assertThat(result.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.AMOUNT_TOO_LARGE &&
                i.getSeverity() == IssueSeverity.WARNING);
    }

    

    @Test
    @DisplayName("duplicate row produces DUPLICATE_ROW warning on second occurrence")
    void duplicateRowProducesWarning() {
        String line = "2026-04-01,Groceries,2400.00,INR,Aisha,EQUAL,,";
        Set<String> seen = new HashSet<>();

        CsvParserService.ParseResult first = parser.parseRow(
                line, 2, standardColumnIndex, membersByName, activeMemberIds, seen);
        CsvParserService.ParseResult second = parser.parseRow(
                line, 3, standardColumnIndex, membersByName, activeMemberIds, seen);

        assertThat(first.issues()).noneMatch(i -> i.getIssueType() == ImportIssueType.DUPLICATE_ROW);
        assertThat(second.issues()).anyMatch(i ->
                i.getIssueType() == ImportIssueType.DUPLICATE_ROW &&
                i.getSeverity() == IssueSeverity.WARNING);
    }

    

    private CsvParserService.ParseResult parseRow(String line) {
        return parser.parseRow(line, 2, standardColumnIndex, membersByName, activeMemberIds, new HashSet<>());
    }
}
