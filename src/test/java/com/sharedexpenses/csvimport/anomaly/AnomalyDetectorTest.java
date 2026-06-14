package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportIssueType;
import com.sharedexpenses.csvimport.ImportRow;
import com.sharedexpenses.csvimport.ImportRowStatus;
import com.sharedexpenses.csvimport.IssueSeverity;
import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.group.GroupMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;


@DisplayName("AnomalyDetector implementations")
class AnomalyDetectorTest {

    private static final Long GROUP_ID = 1L;
    private static final Long USER_AISHA = 1L;
    private static final Long USER_ROHAN = 2L;
    private static final Long USER_PRIYA = 3L;

    private AnomalyContext context;

    @BeforeEach
    void setUp() {
        
        List<GroupMember> members = List.of(
                member(USER_AISHA, LocalDate.of(2026, 1, 1), null),
                member(USER_ROHAN, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1)),
                member(USER_PRIYA, LocalDate.of(2026, 4, 1), null)
        );
        List<Expense> expenses = List.of(
                existingExpense(USER_AISHA, LocalDate.of(2026, 4, 1), "Groceries", new BigDecimal("2400.00"))
        );
        context = new AnomalyContext(GROUP_ID, expenses, members);
    }

    

    @Nested
    @DisplayName("NegativeAmountDetector")
    class NegativeAmountDetectorTests {
        private final NegativeAmountDetector detector = new NegativeAmountDetector();

        @Test
        @DisplayName("no anomaly for positive amount")
        void positiveAmountIsClean() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Dinner", "1500.00");
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("ERROR for zero amount")
        void zeroAmountIsError() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Dinner", "0.00");
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getSeverity()).isEqualTo(IssueSeverity.ERROR);
            assertThat(reports.get(0).getIssueType()).isEqualTo(ImportIssueType.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("ERROR for negative amount")
        void negativeAmountIsError() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Dinner", "-100.00");
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getRecommendedAction()).isNotBlank();
        }

        @Test
        @DisplayName("ERROR for null amount")
        void nullAmountIsError() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Dinner", null);
            assertThat(detector.detect(row, context)).hasSize(1);
        }
    }

    

    @Nested
    @DisplayName("DuplicateExpenseDetector")
    class DuplicateExpenseDetectorTests {
        private final DuplicateExpenseDetector detector = new DuplicateExpenseDetector();

        @Test
        @DisplayName("WARNING when all four fields match an existing expense")
        void exactMatchProducesWarning() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 1), "Groceries", "2400.00");
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getSeverity()).isEqualTo(IssueSeverity.WARNING);
        }

        @Test
        @DisplayName("no anomaly when description differs")
        void differentDescriptionIsNotDuplicate() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 1), "Weekly Groceries", "2400.00");
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("no anomaly when amount differs")
        void differentAmountIsNotDuplicate() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 1), "Groceries", "3000.00");
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("case-insensitive description matching")
        void caseInsensitiveDuplicateDetection() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 1), "GROCERIES", "2400.00");
            assertThat(detector.detect(row, context)).hasSize(1);
        }
    }

    

    @Nested
    @DisplayName("SettlementAsExpenseDetector")
    class SettlementAsExpenseDetectorTests {
        private final SettlementAsExpenseDetector detector = new SettlementAsExpenseDetector();

        @Test
        @DisplayName("WARNING for description with settlement keyword + 2 participants")
        void settlementKeywordWithTwoParticipants() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Paid back Rohan", "500.00");
            row.setParticipantIds(USER_AISHA + "|" + USER_ROHAN);
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getSeverity()).isEqualTo(IssueSeverity.WARNING);
            assertThat(reports.get(0).getRecommendedAction()).contains("settlements");
        }

        @Test
        @DisplayName("no anomaly for settlement keyword with 3+ participants (shared expense, not bilateral)")
        void settlementKeywordWithThreeParticipantsIsOk() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Reimbursement dinner", "1500.00");
            row.setParticipantIds(USER_AISHA + "|" + USER_ROHAN + "|" + USER_PRIYA);
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("no anomaly for normal expense description")
        void normalExpenseDescriptionIsClean() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 10), "Dinner at restaurant", "3000.00");
            row.setParticipantIds(USER_AISHA + "|" + USER_ROHAN);
            assertThat(detector.detect(row, context)).isEmpty();
        }
    }

    

    @Nested
    @DisplayName("InvalidSplitDetector")
    class InvalidSplitDetectorTests {
        private final InvalidSplitDetector detector = new InvalidSplitDetector();

        @Test
        @DisplayName("no anomaly for EQUAL split")
        void equalSplitHasNoIssues() {
            ImportRow row = rowWithSplit(SplitType.EQUAL, "2400.00", null);
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("ERROR when PERCENTAGE values sum to less than 100")
        void percentageSumTooLow() {
            ImportRow row = rowWithSplit(SplitType.PERCENTAGE, "2400.00", "40|30|20"); 
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getSeverity()).isEqualTo(IssueSeverity.ERROR);
            assertThat(reports.get(0).getMessage()).contains("90");
        }

        @Test
        @DisplayName("no anomaly when PERCENTAGE values sum exactly to 100")
        void percentageSumExactly100() {
            ImportRow row = rowWithSplit(SplitType.PERCENTAGE, "2400.00", "40|30|30");
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("ERROR when EXACT amounts do not sum to expense total")
        void exactSumMismatch() {
            ImportRow row = rowWithSplit(SplitType.EXACT, "2400.00", "600|700|500"); 
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getMessage()).contains("1800");
        }

        @Test
        @DisplayName("no anomaly when EXACT amounts match expense total")
        void exactSumCorrect() {
            ImportRow row = rowWithSplit(SplitType.EXACT, "2400.00", "800|800|800");
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("ERROR for zero share value in SHARES split")
        void zeroShareValue() {
            ImportRow row = rowWithSplit(SplitType.SHARES, "2400.00", "0|1|1");
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getRecommendedAction()).isNotBlank();
        }
    }

    

    @Nested
    @DisplayName("MemberNotActiveDetector")
    class MemberNotActiveDetectorTests {
        private final MemberNotActiveDetector detector = new MemberNotActiveDetector();

        @Test
        @DisplayName("no anomaly when all members were active on expense date")
        void allMembersActiveOnDate() {
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 4, 15), "Dinner", "1500.00");
            row.setParticipantIds(USER_AISHA + "|" + USER_ROHAN + "|" + USER_PRIYA);
            assertThat(detector.detect(row, context)).isEmpty();
        }

        @Test
        @DisplayName("ERROR when payer had left before expense date")
        void payerLeftBeforeExpenseDate() {
            
            ImportRow row = row(USER_ROHAN, LocalDate.of(2026, 5, 10), "Dinner", "1500.00");
            row.setParticipantIds(USER_AISHA.toString());
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).getSeverity()).isEqualTo(IssueSeverity.ERROR);
        }

        @Test
        @DisplayName("ERROR when participant had not yet joined on expense date")
        void participantNotYetJoined() {
            
            ImportRow row = row(USER_AISHA, LocalDate.of(2026, 3, 15), "Dinner", "1500.00");
            row.setParticipantIds(USER_AISHA + "|" + USER_PRIYA);
            List<AnomalyReport> reports = detector.detect(row, context);
            assertThat(reports).anyMatch(r -> r.getSeverity() == IssueSeverity.ERROR);
        }
    }

    

    @Nested
    @DisplayName("AnomalyContext membership date checks")
    class AnomalyContextTests {

        @Test
        @DisplayName("active member returns true on any date in their membership")
        void activeMemberPassesCheck() {
            assertThat(context.wasMemberActiveOn(USER_AISHA, LocalDate.of(2026, 6, 1))).isTrue();
        }

        @Test
        @DisplayName("former member returns false after their leftAt date")
        void formerMemberFailsAfterLeftAt() {
            
            assertThat(context.wasMemberActiveOn(USER_ROHAN, LocalDate.of(2026, 5, 1))).isFalse();
            assertThat(context.wasMemberActiveOn(USER_ROHAN, LocalDate.of(2026, 4, 30))).isTrue();
        }

        @Test
        @DisplayName("new member returns false before their joinedAt date")
        void newMemberFailsBeforeJoinedAt() {
            
            assertThat(context.wasMemberActiveOn(USER_PRIYA, LocalDate.of(2026, 3, 31))).isFalse();
            assertThat(context.wasMemberActiveOn(USER_PRIYA, LocalDate.of(2026, 4, 1))).isTrue();
        }
    }

    

    private ImportRow row(Long payerUserId, LocalDate date, String description, String amountStr) {
        ImportRow row = new ImportRow(1L, 2, description + "," + amountStr);
        row.setStatus(ImportRowStatus.VALID);
        row.setPaidByUserId(payerUserId);
        row.setPaidByName("User" + payerUserId);
        row.setExpenseDate(date);
        row.setDescription(description);
        row.setCurrency("INR");
        row.setSplitType(SplitType.EQUAL.name());
        if (amountStr != null) row.setAmount(new BigDecimal(amountStr));
        return row;
    }

    private ImportRow rowWithSplit(SplitType splitType, String totalAmount, String participantValues) {
        ImportRow row = new ImportRow(1L, 2, "test");
        row.setStatus(ImportRowStatus.VALID);
        row.setExpenseDate(LocalDate.of(2026, 4, 10));
        row.setDescription("Test expense");
        row.setCurrency("INR");
        row.setPaidByUserId(USER_AISHA);
        row.setSplitType(splitType.name());
        if (totalAmount != null) row.setAmount(new BigDecimal(totalAmount));
        if (participantValues != null) {
            row.setParticipantValues(participantValues);
            row.setParticipantIds(USER_AISHA + "|" + USER_ROHAN + "|" + USER_PRIYA);
        }
        return row;
    }

    private GroupMember member(Long userId, LocalDate joinedAt, LocalDate leftAt) {
        GroupMember m = new GroupMember(GROUP_ID, userId, joinedAt);
        if (leftAt != null) m.leave(leftAt);
        return m;
    }

    private Expense existingExpense(Long paidBy, LocalDate date, String description, BigDecimal amount) {
        return new Expense(GROUP_ID, description, amount, "INR", date, paidBy, SplitType.EQUAL, null, paidBy);
    }
}
