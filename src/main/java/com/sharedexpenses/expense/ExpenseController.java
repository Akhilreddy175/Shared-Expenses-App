package com.sharedexpenses.expense;

import com.sharedexpenses.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    record ParticipantReq(
            @NotNull Long userId,
            BigDecimal shareAmount,
            BigDecimal percentage,
            BigDecimal shares
    ) {}

    record CreateExpenseReq(
            @NotBlank String description,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String currency,
            @NotNull LocalDate expenseDate,
            @NotNull Long paidBy,
            @NotNull SplitType splitType,
            String category,
            @NotNull List<ParticipantReq> participants
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@PathVariable Long groupId,
                                                      @Valid @RequestBody CreateExpenseReq req,
                                                      @AuthenticationPrincipal UserPrincipal me) {
        List<ExpenseService.ParticipantInput> inputs = req.participants().stream()
                .map(p -> new ExpenseService.ParticipantInput(p.userId(), p.shareAmount(), p.percentage(), p.shares()))
                .collect(Collectors.toList());

        Map<String, Object> result = expenseService.createExpense(
                groupId, req.description(), req.amount(), req.currency(),
                req.expenseDate(), req.paidBy(), req.splitType(), req.category(),
                inputs, me.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable Long groupId,
                                          @AuthenticationPrincipal UserPrincipal me) {
        return expenseService.listExpenses(groupId, me.getId());
    }

    @GetMapping("/{expenseId}")
    public Map<String, Object> get(@PathVariable Long groupId,
                                   @PathVariable Long expenseId,
                                   @AuthenticationPrincipal UserPrincipal me) {
        return expenseService.getExpense(groupId, expenseId, me.getId());
    }

    @PutMapping("/{expenseId}")
    public Map<String, Object> update(@PathVariable Long groupId,
                                      @PathVariable Long expenseId,
                                      @Valid @RequestBody CreateExpenseReq req,
                                      @AuthenticationPrincipal UserPrincipal me) {
        List<ExpenseService.ParticipantInput> inputs = req.participants().stream()
                .map(p -> new ExpenseService.ParticipantInput(p.userId(), p.shareAmount(), p.percentage(), p.shares()))
                .collect(Collectors.toList());

        return expenseService.updateExpense(
                groupId, expenseId, req.description(), req.amount(), req.currency(),
                req.expenseDate(), req.paidBy(), req.splitType(), req.category(),
                inputs, me.getId()
        );
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId,
                                       @PathVariable Long expenseId,
                                       @AuthenticationPrincipal UserPrincipal me) {
        expenseService.deleteExpense(groupId, expenseId, me.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balances")
    public Map<String, Object> getBalances(@PathVariable Long groupId,
                                           @AuthenticationPrincipal UserPrincipal me) {
        return expenseService.calculateBalances(groupId, me.getId());
    }

    @GetMapping("/balances/settlements")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSuggestedSettlements(@PathVariable Long groupId,
                                                             @AuthenticationPrincipal UserPrincipal me) {
        Map<String, Object> balances = expenseService.calculateBalances(groupId, me.getId());
        return (List<Map<String, Object>>) balances.get("suggestedSettlements");
    }
}
