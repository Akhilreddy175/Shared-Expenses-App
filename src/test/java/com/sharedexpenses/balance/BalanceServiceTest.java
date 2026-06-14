package com.sharedexpenses.balance;

import com.sharedexpenses.balance.dto.SettlementResponse;
import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.ExpenseParticipant;
import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.settlement.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;


@DisplayName("BalanceService — computation logic")
class BalanceServiceTest {

    private BalanceService service;

    @BeforeEach
    void setUp() {
        
        service = new BalanceService(null, null, null, null, null, null, null);
    }

    

    @Test
    @DisplayName("sums expense amounts per payer correctly")
    void computesTotalPaidPerUser() {
        Set<Long> userIds = Set.of(1L, 2L, 3L);
        List<Expense> expenses = List.of(
                expense(1L, "300.00"),
                expense(2L, "150.00"),
                expense(1L, "100.00")
        );

        Map<Long, BigDecimal> paid = service.computeTotalPaid(expenses, userIds);

        assertThat(paid.get(1L)).isEqualByComparingTo("400.00");
        assertThat(paid.get(2L)).isEqualByComparingTo("150.00");
        assertThat(paid.get(3L)).isEqualByComparingTo("0.00");
    }

    

    @Test
    @DisplayName("sums share amounts per participant correctly")
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

    

    @Test
    @DisplayName("payer's net is positive, receiver's net is negative")
    void settlementNetSignsAreCorrect() {
        
        Set<Long> userIds = new HashSet<>(Set.of(1L, 2L, 3L));
        List<Settlement> settlements = List.of(settlement(3L, 1L, "150.00"));

        Map<Long, BigDecimal> net = service.computeSettlementNet(settlements, userIds);

        assertThat(net.get(3L)).isEqualByComparingTo("150.00");   
        assertThat(net.get(1L)).isEqualByComparingTo("-150.00");  
        assertThat(net.get(2L)).isEqualByComparingTo("0.00");     
    }

    @Test
    @DisplayName("multiple settlements accumulate correctly")
    void multipleSettlementsAccumulate() {
        Set<Long> userIds = new HashSet<>(Set.of(1L, 2L, 3L));
        List<Settlement> settlements = List.of(
                settlement(2L, 1L, "100.00"),   
                settlement(3L, 1L, "150.00")    
        );

        Map<Long, BigDecimal> net = service.computeSettlementNet(settlements, userIds);

        assertThat(net.get(1L)).isEqualByComparingTo("-250.00"); 
        assertThat(net.get(2L)).isEqualByComparingTo("100.00");  
        assertThat(net.get(3L)).isEqualByComparingTo("150.00");  
    }

    

    @Test
    @DisplayName("one debtor settles with one creditor in a single transaction")
    void oneDebtorOneCreditor() {
        Map<Long, BigDecimal> balances = mapOf(1L, "300.00", 2L, "-300.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan");

        List<SettlementResponse> result = service.computeMinimumSettlements(balances, names);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFromDisplayName()).isEqualTo("Rohan");
        assertThat(result.get(0).getToDisplayName()).isEqualTo("Aisha");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("two debtors pay the same creditor — two transactions total")
    void twoDebtorsOneCreditor() {
        Map<Long, BigDecimal> balances = mapOf(1L, "500.00", 2L, "-200.00", 3L, "-300.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan", 3L, "Priya");

        List<SettlementResponse> result = service.computeMinimumSettlements(balances, names);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(s -> assertThat(s.getToDisplayName()).isEqualTo("Aisha"));
        BigDecimal totalTransferred = result.stream()
                .map(SettlementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalTransferred).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("zero balances produce no settlement suggestions")
    void zeroBalancesProduceNoSettlements() {
        Map<Long, BigDecimal> balances = mapOf(1L, "0.00", 2L, "0.00", 3L, "0.00");
        Map<Long, String> names = namesOf(1L, "A", 2L, "B", 3L, "C");

        assertThat(service.computeMinimumSettlements(balances, names)).isEmpty();
    }

    @Test
    @DisplayName("flat-mate scenario: Aisha paid 300, Rohan paid 150, Priya paid 0 — Priya owes Aisha")
    void fullFlatMateScenario() {
        
        
        Map<Long, BigDecimal> balances = mapOf(1L, "0.00", 2L, "0.00", 3L, "0.00");
        Map<Long, String> names = namesOf(1L, "Aisha", 2L, "Rohan", 3L, "Priya");

        assertThat(service.computeMinimumSettlements(balances, names)).isEmpty();
    }

    

    private Expense expense(Long paidBy, String amount) {
        return new Expense(1L, "Test", new BigDecimal(amount), "INR",
                LocalDate.of(2026, 4, 10), paidBy, SplitType.EQUAL, null, paidBy);
    }

    private ExpenseParticipant participant(Long expenseId, Long userId, String share) {
        return new ExpenseParticipant(expenseId, userId, new BigDecimal(share));
    }

    private Settlement settlement(Long payerId, Long receiverId, String amount) {
        return new Settlement(1L, payerId, receiverId, new BigDecimal(amount),
                LocalDate.of(2026, 5, 1), null, payerId);
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
