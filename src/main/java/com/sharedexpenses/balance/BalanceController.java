package com.sharedexpenses.balance;

import com.sharedexpenses.balance.dto.GroupBalanceResponse;
import com.sharedexpenses.balance.dto.SettlementResponse;
import com.sharedexpenses.balance.dto.UserBalanceResponse;
import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GroupBalanceResponse>> getGroupBalances(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        GroupBalanceResponse balances = balanceService.getGroupBalances(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(balances));
    }

   
    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> getSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<SettlementResponse> settlements = balanceService.getSettlements(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(settlements));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserBalanceResponse>> getUserBalance(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        UserBalanceResponse balance = balanceService.getUserBalance(groupId, userId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }
}
