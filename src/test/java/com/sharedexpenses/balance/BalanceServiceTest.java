package com.sharedexpenses.balance;

import com.sharedexpenses.balance.dto.SettlementResponse;
import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.ExpenseParticipant;
import com.sharedexpenses.expense.SplitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the balance computation logic.
 *
 * Why test computeMinimumSettlements() directly?
 * The settlement algorithm is pure computation — it takes a Map<Long, BigDecimal>
 * and returns a List<SettlementResponse>. No database, no Spring context.
 * Testing it directly is faster, simpler, and more precise than wiring up mocks.
 * BalanceService.computeMinimumSettlements() is package-private for exactly this reason.
 */
@DisplayName("BalanceService — computation logic")
class BalanceServiceTest {

    // We use a minimal BalanceService instance with only the computation methods under test.
    // The constructor requires all dependencies but we can test the package-private methods
    // by creating the class with null dependencies — they're not used in these pure methods.
    private BalanceService service;

    @BeforeEach
    void setUp() {
        // null arguments are fine because the tested methods don't call any injected beans
        service = new BalanceService(null, null, null, null, null, null);
    }

    // ─── computeTotalPaid ─────────────────────────────────────────────────────

    @Test
    @DisplayName("sums amounts correctly for multiple payers")
    void computesTotalPaidPerUser() {
        Set<Long> userIds = Set.of(1L, 2L, 3L);
        List<Expense> expenses = List.of(
                expense(1L, "300.00"),   // user 1 paid 300
                expense(2L, "150.00"),   // user 2 paid 150
                expense(1L, "100.00")    // user 1 paid another 100
        );

        Map<Long, BigDecimal> paid = service.computeTotalPaid(expenses, userIds);

        assertThat(paid.get(1L)).isEqualByComparingTo("400.00");
        assertThat(paid.get(2L)).isEqualByComparingTo("150.00");
        assertThat(paid.get(3L)).isEqualByComparingTo("0.00");
    }

    // ─── computeTotalOwed ─────────────────────────────────────────────────────

    @Test
    @DisplayName("sums share amounts correctly for each participant")
    void computesTotalOwedPerUser() {
        Set<Long> userIds = Set.of(1L, 2L, 3L);
        List<ExpenseParticipant> participants = List.of(
                participant(1L, 1L, "100.00"),
                participant(1L, 2L, "100.00"),
                participant(1L, 3L, "100.00"),
                participant(2L, 1L, "50.00"),
                participant(2L, 2L, "50.00"),
                participant(2L, 3L, "50.00")
        );

        Map<Long, BigDecimal> owed = service.computeTotalOwed(participants, userIds);

        assertThat(owed.get(1L)).isEqualByComparingTo("150.00");
        assertThat(owed.get(2L)).isEqualByComparingTo("150.00");
        assertThat(owed.get(3L)).isEqualByComparingTo("150.00");
    }

    // ─── computeMinimumSettlements ────────────────────────────────────────────

    @Test
    @DisplayName("one debtor settles with one creditor in a single transaction")
    void oneDebtorOneCreditor() {
        Map<Long, BigDecimal> balances = mapOf(1L, "300.00", 2L, "-300.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan");

        List<SettlementResponse> settlements = service.computeMinimumSettlements(balances, names);

        assertThat(settlements).hasSize(1);
        assertThat(settlements.get(0).getFromDisplayName()).isEqualTo("Rohan");
        assertThat(settlements.get(0).getToDisplayName()).isEqualTo("Aisha");
        assertThat(settlements.get(0).getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("two debtors pay the same creditor in two transactions")
    void twoDebtorsOneCreditor() {
        // A=+500, B=-200, C=-300
        Map<Long, BigDecimal> balances = mapOf(1L, "500.00", 2L, "-200.00", 3L, "-300.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan", 3L, "Priya");

        List<SettlementResponse> settlements = service.computeMinimumSettlements(balances, names);

        // Greedy: largest debtor (Priya -300) pays largest creditor (Aisha +500) first
        assertThat(settlements).hasSize(2);

        BigDecimal totalTransferred = settlements.stream()
                .map(SettlementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("500.00");

        // Verify all settlements go to Aisha (the only creditor)
        assertThat(settlements).allSatisfy(s -> assertThat(s.getToDisplayName()).isEqualTo("Aisha"));
    }

    @Test
    @DisplayName("zero balances produce no settlements")
    void zeroBalancesProduceNoSettlements() {
        Map<Long, BigDecimal> balances = mapOf(1L, "0.00", 2L, "0.00", 3L, "0.00");
        Map<Long, String> names = namesOf(1L, "A", 2L, "B", 3L, "C");

        List<SettlementResponse> settlements = service.computeMinimumSettlements(balances, names);

        assertThat(settlements).isEmpty();
    }

    @Test
    @DisplayName("full flat-mate scenario: Aisha paid 300, Rohan paid 150, Priya paid 0")
    void fullFlatMateScenario() {
        // Expense 1: Aisha paid 300, equal split among 3 → 100 each
        // Expense 2: Rohan paid 150, equal split among 3 → 50 each
        // Aisha: paid 300, owed 150, balance = +150
        // Rohan: paid 150, owed 150, balance = 0
        // Priya: paid 0,   owed 150, balance = -150
        Map<Long, BigDecimal> balances = mapOf(1L, "150.00", 2L, "0.00", 3L, "-150.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan", 3L, "Priya");

        List<SettlementResponse> settlements = service.computeMinimumSettlements(balances, names);

        assertThat(settlements).hasSize(1);
        assertThat(settlements.get(0).getFromDisplayName()).isEqualTo("Priya");
        assertThat(settlements.get(0).getToDisplayName()).isEqualTo("Aisha");
        assertThat(settlements.get(0).getAmount()).isEqualByComparingTo("150.00");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Expense expense(Long paidBy, String amount) {
        return new Expense(1L, "Test expense", new BigDecimal(amount), "INR",
                LocalDate.of(2026, 4, 10), paidBy, SplitType.EQUAL, null, paidBy);
    }

    private ExpenseParticipant participant(Long expenseId, Long userId, String share) {
        return new ExpenseParticipant(expenseId, userId, new BigDecimal(share));
    }

    private Map<Long, BigDecimal> mapOf(Object... keysAndValues) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((Long) keysAndValues[i], new BigDecimal(keysAndValues[i + 1].toString()));
        }
        return map;
    }

    private Map<Long, String> namesOf(Object... keysAndValues) {
        Map<Long, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((Long) keysAndValues[i], (String) keysAndValues[i + 1]);
        }
        return map;
    }
}
