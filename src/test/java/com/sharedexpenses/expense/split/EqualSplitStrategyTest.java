package com.sharedexpenses.expense.split;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.dto.ParticipantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EqualSplitStrategy")
class EqualSplitStrategyTest {

    private EqualSplitStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EqualSplitStrategy();
    }

    @Test
    @DisplayName("divides an amount that splits cleanly")
    void dividesCleanlySplittableAmount() {
        List<BigDecimal> shares = strategy.split(new BigDecimal("120.00"), participantsOf(3));

        assertThat(shares).hasSize(3);
        assertThat(shares).allSatisfy(s -> assertThat(s).isEqualByComparingTo("40.00"));
    }

    @Test
    @DisplayName("rounding remainder goes to the last participant")
    void roundingRemainderGoesToLastParticipant() {
        // 100 / 3 = 33.33 + 33.33 + 33.34
        List<BigDecimal> shares = strategy.split(new BigDecimal("100.00"), participantsOf(3));

        assertThat(shares).hasSize(3);
        assertThat(shares.get(0)).isEqualByComparingTo("33.33");
        assertThat(shares.get(1)).isEqualByComparingTo("33.33");
        assertThat(shares.get(2)).isEqualByComparingTo("33.34");
    }

    @Test
    @DisplayName("shares always sum exactly to the total")
    void sharesAlwaysSumToTotal() {
        BigDecimal total = new BigDecimal("2401.00");
        List<BigDecimal> shares = strategy.split(total, participantsOf(6));

        BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("works for a single participant")
    void singleParticipantGetsFullAmount() {
        List<BigDecimal> shares = strategy.split(new BigDecimal("500.00"), participantsOf(1));

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("EQUAL flat-mate scenario: 2400 / 6 = 400 each")
    void flatMateScenario() {
        List<BigDecimal> shares = strategy.split(new BigDecimal("2400.00"), participantsOf(6));

        assertThat(shares).allSatisfy(s -> assertThat(s).isEqualByComparingTo("400.00"));
        BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("2400.00");
    }

    // --- helpers ---

    private List<ParticipantRequest> participantsOf(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    ParticipantRequest p = new ParticipantRequest();
                    p.setUserId((long) i);
                    return p;
                })
                .toList();
    }
}
