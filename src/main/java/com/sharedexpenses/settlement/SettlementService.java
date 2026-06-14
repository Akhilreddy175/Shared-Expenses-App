package com.sharedexpenses.settlement;

import com.sharedexpenses.AppException;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public SettlementService(SettlementRepository settlementRepository,
                             GroupMemberRepository memberRepository,
                             UserRepository userRepository,
                             GroupService groupService) {
        this.settlementRepository = settlementRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @Transactional
    public Map<String, Object> recordSettlement(Long groupId, Long payerId, Long receiverId, BigDecimal amount,
                                                LocalDate settlementDate, Long currentUserId) {

        groupService.requireMember(groupId, currentUserId);

        if (payerId.equals(receiverId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Payer and receiver cannot be the same person");
        }

        if (!memberRepository.existsByGroupIdAndUserId(groupId, payerId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Payer (user ID: " + payerId + ") is not a member of this group");
        }
        if (!memberRepository.existsByGroupIdAndUserId(groupId, receiverId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Receiver (user ID: " + receiverId + ") is not a member of this group");
        }

        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> AppException.notFound("Payer not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> AppException.notFound("Receiver not found"));

        LocalDate date = settlementDate != null ? settlementDate : LocalDate.now();

        Settlement settlement = new Settlement(groupId, payerId, receiverId, amount, date, null, currentUserId);
        settlement = settlementRepository.save(settlement);

        return toMap(settlement, payer, receiver);
    }

    public List<Map<String, Object>> listSettlements(Long groupId, Long userId) {
        groupService.requireMember(groupId, userId);

        List<Settlement> settlements = settlementRepository.findByGroupIdOrderBySettlementDateDesc(groupId);
        if (settlements.isEmpty()) return List.of();

        List<Long> userIds = settlements.stream()
                .flatMap(s -> java.util.stream.Stream.of(s.getPayerId(), s.getReceiverId()))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return settlements.stream()
                .map(s -> toMap(s, usersById.get(s.getPayerId()), usersById.get(s.getReceiverId())))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSettlement(Long groupId, Long settlementId, Long userId) {
        groupService.requireMember(groupId, userId);
        Settlement settlement = settlementRepository.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> AppException.notFound("Settlement not found"));

        User payer = userRepository.findById(settlement.getPayerId())
                .orElseThrow(() -> AppException.notFound("Payer not found"));
        User receiver = userRepository.findById(settlement.getReceiverId())
                .orElseThrow(() -> AppException.notFound("Receiver not found"));

        return toMap(settlement, payer, receiver);
    }

    @Transactional
    public void deleteSettlement(Long groupId, Long settlementId, Long userId) {
        groupService.requireMember(groupId, userId);
        Settlement settlement = settlementRepository.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> AppException.notFound("Settlement not found"));
        settlementRepository.delete(settlement);
    }

    private Map<String, Object> toMap(Settlement settlement, User payer, User receiver) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", settlement.getId());
        map.put("groupId", settlement.getGroupId());
        map.put("payerUserId", payer.getId());
        map.put("payerDisplayName", payer.getDisplayName());
        map.put("receiverUserId", receiver.getId());
        map.put("receiverDisplayName", receiver.getDisplayName());
        map.put("amount", settlement.getAmount());
        map.put("settlementDate", settlement.getSettlementDate());
        map.put("note", settlement.getNote());
        map.put("createdAt", settlement.getCreatedAt());
        return map;
    }
}
