package com.sharedexpenses.expense.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.expense.dto.ParticipantRequest;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    public SplitType getType() {
        return SplitType.EXACT;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, List<ParticipantRequest> participants) {
        for (ParticipantRequest p : participants) {
            if (p.getShareAmount() == null) {
                throw new ValidationException(
                        "shareAmount is required for each participant when using EXACT split");
            }
            if (p.getShareAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException(
                        "shareAmount cannot be negative");
            }
        }

        BigDecimal sum = participants.stream()
                .map(ParticipantRequest::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diff = totalAmount.subtract(sum).abs();
        if (diff.compareTo(TOLERANCE) > 0) {
            throw new ValidationException(
                    "The sum of share amounts (" + sum + ") must equal the expense amount (" + totalAmount + ")");
        }

        return participants.stream()
                .map(p -> p.getShareAmount().setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.toList());
    }
}
