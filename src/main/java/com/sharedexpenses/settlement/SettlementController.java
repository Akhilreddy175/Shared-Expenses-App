package com.sharedexpenses.settlement;

import com.sharedexpenses.common.ApiResponse;
import com.sharedexpenses.security.UserPrincipal;
import com.sharedexpenses.settlement.dto.RecordSettlementRequest;
import com.sharedexpenses.settlement.dto.SettlementDetailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> recordSettlement(
            @PathVariable Long groupId,
            @Valid @RequestBody RecordSettlementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        SettlementDetailResponse settlement = settlementService.recordSettlement(
                groupId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(settlement, "Settlement recorded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SettlementDetailResponse>>> listSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<SettlementDetailResponse> settlements = settlementService.listSettlements(
                groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(settlements));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserPrincipal principal) {

        SettlementDetailResponse settlement = settlementService.getSettlement(
                groupId, settlementId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(settlement));
    }

    @DeleteMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<Void>> deleteSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserPrincipal principal) {

        settlementService.deleteSettlement(groupId, settlementId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Settlement deleted"));
    }
}
