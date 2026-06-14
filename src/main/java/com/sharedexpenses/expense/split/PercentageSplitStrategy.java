package com.sharedexpenses.expense.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.expense.dto.ParticipantRequest;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    public SplitType getType() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, List<ParticipantRequest> participants) {
        for (ParticipantRequest p : participants) {
            if (p.getPercentage() == null) {
                throw new ValidationException(
                        "percentage is required for each participant when using PERCENTAGE split");
            }
            if (p.getPercentage().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getPercentage().compareTo(HUNDRED) > 0) {
                throw new ValidationException(
                        "Each percentage must be between 0 and 100 (got: " + p.getPercentage() + ")");
            }
        }

        BigDecimal percentageSum = participants.stream()
                .map(ParticipantRequest::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diff = HUNDRED.subtract(percentageSum).abs();
        if (diff.compareTo(TOLERANCE) > 0) {
            throw new ValidationException(
                    "Percentages must sum to 100 (got " + percentageSum + ")");
        }

        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal distributed = BigDecimal.ZERO;
        int last = participants.size() - 1;

        for (int i = 0; i <= last; i++) {
            if (i == last) {
                shares.add(totalAmount.subtract(distributed));
            } else {
                BigDecimal share = totalAmount
                        .multiply(participants.get(i).getPercentage())
                        .divide(HUNDRED, 2, RoundingMode.DOWN);
                shares.add(share);
                distributed = distributed.add(share);
            }
        }
        return shares;
    }
}
