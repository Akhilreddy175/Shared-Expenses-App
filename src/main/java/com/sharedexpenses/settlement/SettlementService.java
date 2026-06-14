package com.sharedexpenses.settlement;

import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.settlement.dto.RecordSettlementRequest;
import com.sharedexpenses.settlement.dto.SettlementDetailResponse;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             GroupMemberRepository groupMemberRepository,
                             GroupService groupService,
                             UserRepository userRepository) {
        this.settlementRepository = settlementRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    
    @Transactional
    public SettlementDetailResponse recordSettlement(Long groupId, RecordSettlementRequest request,
                                                     Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        if (request.getPayerId().equals(request.getReceiverId())) {
            throw new ValidationException("Payer and receiver cannot be the same person");
        }

        validateGroupMembership(groupId, request.getPayerId(), "Payer");
        validateGroupMembership(groupId, request.getReceiverId(), "Receiver");

        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.getPayerId()));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.getReceiverId()));

        LocalDate date = request.getSettlementDate() != null
                ? request.getSettlementDate()
                : LocalDate.now();

        Settlement settlement = new Settlement(
                groupId,
                request.getPayerId(),
                request.getReceiverId(),
                request.getAmount(),
                date,
                request.getNote() != null ? request.getNote().trim() : null,
                currentUserId
        );

        Settlement saved = settlementRepository.save(settlement);
        return SettlementDetailResponse.from(saved, payer, receiver);
    }

    public List<SettlementDetailResponse> listSettlements(Long groupId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        List<Settlement> settlements = settlementRepository.findByGroupIdOrderBySettlementDateDesc(groupId);
        if (settlements.isEmpty()) return List.of();

        List<Long> userIds = settlements.stream()
                .flatMap(s -> java.util.stream.Stream.of(s.getPayerId(), s.getReceiverId()))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return settlements.stream()
                .map(s -> SettlementDetailResponse.from(
                        s,
                        usersById.get(s.getPayerId()),
                        usersById.get(s.getReceiverId())
                ))
                .collect(Collectors.toList());
    }

    public SettlementDetailResponse getSettlement(Long groupId, Long settlementId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        Settlement settlement = findOrThrow(groupId, settlementId);
        User payer = userRepository.findById(settlement.getPayerId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", settlement.getPayerId()));
        User receiver = userRepository.findById(settlement.getReceiverId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", settlement.getReceiverId()));

        return SettlementDetailResponse.from(settlement, payer, receiver);
    }

    
    @Transactional
    public void deleteSettlement(Long groupId, Long settlementId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        Settlement settlement = findOrThrow(groupId, settlementId);
        settlementRepository.delete(settlement);
    }

    

    private void validateGroupMembership(Long groupId, Long userId, String role) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ValidationException(role + " (user ID: " + userId + ") is not a member of this group");
        }
    }

    private Settlement findOrThrow(Long groupId, Long settlementId) {
        return settlementRepository.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Settlement " + settlementId + " not found in this group"));
    }
}
