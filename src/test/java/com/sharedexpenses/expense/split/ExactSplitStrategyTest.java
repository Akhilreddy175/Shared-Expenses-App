package com.sharedexpenses.expense.split;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.dto.ParticipantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ExactSplitStrategy")
class ExactSplitStrategyTest {

    private ExactSplitStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ExactSplitStrategy();
    }

    @Test
    @DisplayName("returns provided amounts when they sum to total")
    void returnsProvidedAmountsWhenValid() {
        List<ParticipantRequest> participants = List.of(
                withShare(1L, "600.00"),
                withShare(2L, "700.00"),
                withShare(3L, "500.00")
        );

        List<BigDecimal> shares = strategy.split(new BigDecimal("1800.00"), participants);

        assertThat(shares.get(0)).isEqualByComparingTo("600.00");
        assertThat(shares.get(1)).isEqualByComparingTo("700.00");
        assertThat(shares.get(2)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("throws when shareAmount is missing for a participant")
    void throwsWhenShareAmountMissing() {
        ParticipantRequest noAmount = new ParticipantRequest();
        noAmount.setUserId(1L);
        

        assertThatThrownBy(() -> strategy.split(new BigDecimal("100.00"), List.of(noAmount)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("shareAmount is required");
    }

    @Test
    @DisplayName("throws when amounts do not sum to total")
    void throwsWhenAmountsDontSumToTotal() {
        List<ParticipantRequest> participants = List.of(
                withShare(1L, "300.00"),
                withShare(2L, "300.00")
        );

        assertThatThrownBy(() -> strategy.split(new BigDecimal("1000.00"), participants))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must equal the expense amount");
    }

    @Test
    @DisplayName("accepts amounts within 1-cent rounding tolerance")
    void acceptsOnecentRoundingTolerance() {
        
        List<ParticipantRequest> participants = List.of(
                withShare(1L, "33.33"),
                withShare(2L, "33.33"),
                withShare(3L, "33.33")
        );

        assertThatNoException().isThrownBy(
                () -> strategy.split(new BigDecimal("100.00"), participants));
    }

    

    private ParticipantRequest withShare(Long userId, String amount) {
        ParticipantRequest p = new ParticipantRequest();
        p.setUserId(userId);
        p.setShareAmount(new BigDecimal(amount));
        return p;
    }
}
