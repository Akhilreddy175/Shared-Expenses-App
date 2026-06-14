package com.sharedexpenses.expense.split;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.SplitType;

@Component
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies;

    public SplitStrategyFactory(List<SplitStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SplitStrategy::getType, Function.identity()));
    }

    public SplitStrategy getStrategy(SplitType splitType) {
        SplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw new ValidationException("No split strategy implemented for type: " + splitType);
        }
        return strategy;
    }
}
