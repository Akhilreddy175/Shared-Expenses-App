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
public class SharesSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getType() {
        return SplitType.SHARES;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, List<ParticipantRequest> participants) {
        for (ParticipantRequest p : participants) {
            if (p.getShares() == null) {
                throw new ValidationException(
                        "shares is required for each participant when using SHARES split");
            }
            if (p.getShares().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(
                        "shares must be a positive number (got: " + p.getShares() + ")");
            }
        }

        BigDecimal totalShares = participants.stream()
                .map(ParticipantRequest::getShares)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal distributed = BigDecimal.ZERO;
        int last = participants.size() - 1;

        for (int i = 0; i <= last; i++) {
            if (i == last) {
                amounts.add(totalAmount.subtract(distributed));
            } else {
                BigDecimal amount = totalAmount
                        .multiply(participants.get(i).getShares())
                        .divide(totalShares, 2, RoundingMode.DOWN);
                amounts.add(amount);
                distributed = distributed.add(amount);
            }
        }
        return amounts;
    }
}
