package com.sharedexpenses.expense.split;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.dto.ParticipantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SharesSplitStrategy")
class SharesSplitStrategyTest {

    private SharesSplitStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SharesSplitStrategy();
    }

    @Test
    @DisplayName("2:1:1 ratio splits 120 into 60, 30, 30")
    void twoToOneToOneRatio() {
        List<ParticipantRequest> participants = List.of(
                withShares(1L, "2"),
                withShares(2L, "1"),
                withShares(3L, "1")
        );

        List<BigDecimal> shares = strategy.split(new BigDecimal("120.00"), participants);

        assertThat(shares.get(0)).isEqualByComparingTo("60.00");
        assertThat(shares.get(1)).isEqualByComparingTo("30.00");
        assertThat(shares.get(2)).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("shares always sum exactly to total (rounding handled by last participant)")
    void sharesAlwaysSumToTotal() {
        
        List<ParticipantRequest> participants = List.of(
                withShares(1L, "3"),
                withShares(2L, "1")
        );

        List<BigDecimal> shares = strategy.split(new BigDecimal("100.00"), participants);

        BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("absolute values don't matter — only the ratio")
    void absoluteValueDoesNotMatter() {
        
        List<BigDecimal> small = strategy.split(new BigDecimal("120.00"),
                List.of(withShares(1L, "2"), withShares(2L, "1"), withShares(3L, "1")));

        List<BigDecimal> large = strategy.split(new BigDecimal("120.00"),
                List.of(withShares(1L, "200"), withShares(2L, "100"), withShares(3L, "100")));

        assertThat(small.get(0)).isEqualByComparingTo(large.get(0));
        assertThat(small.get(1)).isEqualByComparingTo(large.get(1));
        assertThat(small.get(2)).isEqualByComparingTo(large.get(2));
    }

    @Test
    @DisplayName("throws when shares field is missing")
    void throwsWhenSharesFieldMissing() {
        ParticipantRequest p = new ParticipantRequest();
        p.setUserId(1L);
        

        assertThatThrownBy(() -> strategy.split(new BigDecimal("100.00"), List.of(p)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("shares is required");
    }

    @Test
    @DisplayName("throws when shares is zero or negative")
    void throwsWhenSharesNotPositive() {
        assertThatThrownBy(() -> strategy.split(new BigDecimal("100.00"),
                List.of(withShares(1L, "0"))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("positive");
    }

    

    private ParticipantRequest withShares(Long userId, String sharesValue) {
        ParticipantRequest p = new ParticipantRequest();
        p.setUserId(userId);
        p.setShares(new BigDecimal(sharesValue));
        return p;
    }
}
