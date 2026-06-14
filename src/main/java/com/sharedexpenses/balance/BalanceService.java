package com.sharedexpenses.balance;

import com.sharedexpenses.balance.dto.GroupBalanceResponse;
import com.sharedexpenses.balance.dto.SettlementResponse;
import com.sharedexpenses.balance.dto.UserBalanceResponse;
import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.expense.ExpenseParticipant;
import com.sharedexpenses.expense.ExpenseParticipantRepository;
import com.sharedexpenses.expense.ExpenseRepository;
import com.sharedexpenses.group.Group;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.settlement.Settlement;
import com.sharedexpenses.settlement.SettlementRepository;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class BalanceService {

    
    private static final BigDecimal ROUNDING_THRESHOLD = new BigDecimal("0.005");

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository participantRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public BalanceService(GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository,
                          ExpenseRepository expenseRepository,
                          ExpenseParticipantRepository participantRepository,
                          SettlementRepository settlementRepository,
                          UserRepository userRepository,
                          GroupService groupService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.settlementRepository = settlementRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    public GroupBalanceResponse getGroupBalances(Long groupId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        List<ExpenseParticipant> participants = expenses.isEmpty() ? List.of()
                : participantRepository.findByExpenseIdIn(
                        expenses.stream().map(Expense::getId).collect(Collectors.toList()));
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);

        Set<Long> userIds = collectUserIds(groupId, expenses, participants, settlements);
        Map<Long, User> usersById = loadUsers(userIds);

        Map<Long, BigDecimal> paid = computeTotalPaid(expenses, userIds);
        Map<Long, BigDecimal> owed = computeTotalOwed(participants, userIds);
        Map<Long, BigDecimal> settledNet = computeSettlementNet(settlements, userIds);

        List<UserBalanceResponse> balances = userIds.stream()
                .map(id -> UserBalanceResponse.of(
                        id, usersById.get(id), paid.get(id), owed.get(id), settledNet.get(id)))
                .sorted(Comparator.comparing(UserBalanceResponse::getDisplayName))
                .collect(Collectors.toList());

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return GroupBalanceResponse.of(group, totalExpenses, balances);
    }

    public UserBalanceResponse getUserBalance(Long groupId, Long targetUserId, Long currentUserId) {
        GroupBalanceResponse groupBalance = getGroupBalances(groupId, currentUserId);
        return groupBalance.getMemberBalances().stream()
                .filter(b -> b.getUserId().equals(targetUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User " + targetUserId + " has no financial activity in this group"));
    }

    public List<SettlementResponse> getSettlements(Long groupId, Long currentUserId) {
        GroupBalanceResponse groupBalance = getGroupBalances(groupId, currentUserId);

        Map<Long, BigDecimal> remaining = groupBalance.getMemberBalances().stream()
                .collect(Collectors.toMap(
                        UserBalanceResponse::getUserId,
                        UserBalanceResponse::getBalance,
                        BigDecimal::add,
                        HashMap::new
                ));

        Map<Long, String> displayNames = groupBalance.getMemberBalances().stream()
                .collect(Collectors.toMap(
                        UserBalanceResponse::getUserId,
                        UserBalanceResponse::getDisplayName
                ));

        return computeMinimumSettlements(remaining, displayNames);
    }

    

    Map<Long, BigDecimal> computeTotalPaid(List<Expense> expenses, Set<Long> userIds) {
        Map<Long, BigDecimal> paid = new HashMap<>();
        userIds.forEach(id -> paid.put(id, BigDecimal.ZERO));
        expenses.forEach(e -> paid.merge(e.getPaidBy(), e.getAmount(), BigDecimal::add));
        return paid;
    }

    Map<Long, BigDecimal> computeTotalOwed(List<ExpenseParticipant> participants, Set<Long> userIds) {
        Map<Long, BigDecimal> owed = new HashMap<>();
        userIds.forEach(id -> owed.put(id, BigDecimal.ZERO));
        participants.forEach(p -> owed.merge(p.getUserId(), p.getShareAmount(), BigDecimal::add));
        return owed;
    }

    
    Map<Long, BigDecimal> computeSettlementNet(List<Settlement> settlements, Set<Long> userIds) {
        Map<Long, BigDecimal> net = new HashMap<>();
        userIds.forEach(id -> net.put(id, BigDecimal.ZERO));
        for (Settlement s : settlements) {
            net.merge(s.getPayerId(), s.getAmount(), BigDecimal::add);
            net.merge(s.getReceiverId(), s.getAmount().negate(), BigDecimal::add);
        }
        return net;
    }

    
    List<SettlementResponse> computeMinimumSettlements(Map<Long, BigDecimal> remaining,
                                                        Map<Long, String> displayNames) {
        List<SettlementResponse> suggestions = new ArrayList<>();
        int maxIterations = remaining.size() * 2;

        for (int i = 0; i < maxIterations; i++) {
            Optional<Map.Entry<Long, BigDecimal>> debtorEntry = remaining.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(ROUNDING_THRESHOLD.negate()) < 0)
                    .min(Map.Entry.comparingByValue());

            if (debtorEntry.isEmpty()) break;

            Optional<Map.Entry<Long, BigDecimal>> creditorEntry = remaining.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(ROUNDING_THRESHOLD) > 0)
                    .max(Map.Entry.comparingByValue());

            if (creditorEntry.isEmpty()) break;

            Long debtorId = debtorEntry.get().getKey();
            Long creditorId = creditorEntry.get().getKey();
            BigDecimal transfer = debtorEntry.get().getValue().abs()
                    .min(creditorEntry.get().getValue())
                    .setScale(2, RoundingMode.HALF_UP);

            suggestions.add(SettlementResponse.of(
                    debtorId, displayNames.get(debtorId),
                    creditorId, displayNames.get(creditorId),
                    transfer
            ));

            remaining.merge(debtorId, transfer, BigDecimal::add);
            remaining.merge(creditorId, transfer.negate(), BigDecimal::add);
        }

        return suggestions;
    }

    

    private Set<Long> collectUserIds(Long groupId, List<Expense> expenses,
                                     List<ExpenseParticipant> participants,
                                     List<Settlement> settlements) {
        Set<Long> ids = new HashSet<>();
        groupMemberRepository.findByGroupIdAndLeftAtIsNull(groupId)
                .forEach(m -> ids.add(m.getUserId()));
        expenses.forEach(e -> ids.add(e.getPaidBy()));
        participants.forEach(p -> ids.add(p.getUserId()));
        settlements.forEach(s -> { ids.add(s.getPayerId()); ids.add(s.getReceiverId()); });
        return ids;
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
