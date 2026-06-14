package com.sharedexpenses.expense.split;

import com.sharedexpenses.expense.SplitType;
import com.sharedexpenses.expense.dto.ParticipantRequest;

import java.math.BigDecimal;
import java.util.List;

public interface SplitStrategy {

    SplitType getType();

    List<BigDecimal> split(BigDecimal totalAmount, List<ParticipantRequest> participants);
}
}
