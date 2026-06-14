package com.sharedexpenses.expense;

import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.expense.dto.CreateExpenseRequest;
import com.sharedexpenses.expense.dto.ExpenseResponse;
import com.sharedexpenses.expense.dto.ExpenseSummaryResponse;
import com.sharedexpenses.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.createExpense(groupId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(expense, "Expense recorded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseSummaryResponse>>> listExpenses(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ExpenseSummaryResponse> expenses = expenseService.listExpenses(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(expenses));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.getExpense(groupId, expenseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(expense));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.updateExpense(groupId, expenseId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(expense, "Expense updated"));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.deleteExpense(groupId, expenseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Expense deleted"));
    }
}
