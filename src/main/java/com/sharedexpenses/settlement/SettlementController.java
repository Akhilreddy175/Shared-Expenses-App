package com.sharedexpenses.settlement;

import com.sharedexpenses.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    record RecordSettlementRequest(
            @NotNull(message = "Payer user ID is required") Long payerId,
            @NotNull(message = "Receiver user ID is required") Long receiverId,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
            LocalDate settlementDate,
            @Size(max = 500, message = "Note must not exceed 500 characters") String note
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> recordSettlement(
            @PathVariable Long groupId,
            @Valid @RequestBody RecordSettlementRequest req,
            @AuthenticationPrincipal UserPrincipal me) {
        Map<String, Object> result = settlementService.recordSettlement(
                groupId, req.payerId(), req.receiverId(), req.amount(), req.settlementDate(), me.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public List<Map<String, Object>> listSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal me) {
        return settlementService.listSettlements(groupId, me.getId());
    }

    @GetMapping("/{settlementId}")
    public Map<String, Object> getSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserPrincipal me) {
        return settlementService.getSettlement(groupId, settlementId, me.getId());
    }

    @DeleteMapping("/{settlementId}")
    public ResponseEntity<Void> deleteSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserPrincipal me) {
        settlementService.deleteSettlement(groupId, settlementId, me.getId());
        return ResponseEntity.noContent().build();
    }
}
