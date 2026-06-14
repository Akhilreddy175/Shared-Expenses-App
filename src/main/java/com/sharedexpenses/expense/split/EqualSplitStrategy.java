package com.sharedexpenses.expense.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.expense.dto.ParticipantRequest;

@Component
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getType() {
        return SplitType.EQUAL;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, List<ParticipantRequest> participants) {
        int count = participants.size();
        BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal distributed = perPerson.multiply(BigDecimal.valueOf(count - 1));
        BigDecimal lastPersonShare = totalAmount.subtract(distributed);

        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < count - 1; i++) {
            shares.add(perPerson);
        }
        shares.add(lastPersonShare);
        return shares;
    }
}
