package com.sharedexpenses.expense;

import com.sharedexpenses.AppException;
import com.sharedexpenses.group.GroupMember;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.settlement.Settlement;
import com.sharedexpenses.settlement.SettlementRepository;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseSplitRepository splitRepository,
                          GroupMemberRepository memberRepository,
                          UserRepository userRepository,
                          SettlementRepository settlementRepository,
                          GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.settlementRepository = settlementRepository;
        this.groupService = groupService;
    }

    public record ParticipantInput(Long userId, BigDecimal shareAmount, BigDecimal percentage, BigDecimal shares) {}

    @Transactional
    public Map<String, Object> createExpense(Long groupId, String description, BigDecimal amount, String currency,
                                           LocalDate expenseDate, Long paidBy, SplitType splitType, String category,
                                           List<ParticipantInput> participants, Long currentUserId) {

        groupService.requireMember(groupId, currentUserId);
        validateMemberOnDate(groupId, paidBy, expenseDate);
        for (ParticipantInput p : participants) {
            validateMemberOnDate(groupId, p.userId(), expenseDate);
        }

        Expense expense = new Expense(
                groupId, description.trim(), amount, currency.toUpperCase(),
                expenseDate, paidBy, splitType, category != null ? category.trim() : null, currentUserId
        );
        expense = expenseRepository.save(expense);

        List<ExpenseSplit> splits = buildSplits(expense.getId(), amount, splitType, participants);
        splitRepository.saveAll(splits);

        return toMap(expense, splits);
    }

    public List<Map<String, Object>> listExpenses(Long groupId, Long userId) {
        groupService.requireMember(groupId, userId);
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId);
        if (expenses.isEmpty()) return List.of();

        List<Long> expIds = expenses.stream().map(Expense::getId).collect(Collectors.toList());
        Map<Long, List<ExpenseSplit>> splitsByExpId = splitRepository.findByExpenseIdIn(expIds).stream()
                .collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));

        return expenses.stream()
                .map(e -> toMap(e, splitsByExpId.getOrDefault(e.getId(), List.of())))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getExpense(Long groupId, Long expenseId, Long userId) {
        groupService.requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        List<ExpenseSplit> splits = splitRepository.findByExpenseId(expenseId);
        return toMap(expense, splits);
    }

    @Transactional
    public Map<String, Object> updateExpense(Long groupId, Long expenseId, String description, BigDecimal amount,
                                           String currency, LocalDate expenseDate, Long paidBy, SplitType splitType,
                                           String category, List<ParticipantInput> participants, Long currentUserId) {

        groupService.requireMember(groupId, currentUserId);
        Expense expense = findExpense(groupId, expenseId);

        validateMemberOnDate(groupId, paidBy, expenseDate);
        for (ParticipantInput p : participants) {
            validateMemberOnDate(groupId, p.userId(), expenseDate);
        }

        expense.setDescription(description.trim());
        expense.setAmount(amount);
        expense.setCurrency(currency.toUpperCase());
        expense.setExpenseDate(expenseDate);
        expense.setPaidBy(paidBy);
        expense.setSplitType(splitType);
        expense.setCategory(category != null ? category.trim() : null);
        expense = expenseRepository.save(expense);

        splitRepository.deleteByExpenseId(expenseId);
        List<ExpenseSplit> splits = buildSplits(expenseId, amount, splitType, participants);
        splitRepository.saveAll(splits);

        return toMap(expense, splits);
    }

    @Transactional
    public void deleteExpense(Long groupId, Long expenseId, Long userId) {
        groupService.requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        expenseRepository.delete(expense);
        splitRepository.deleteByExpenseId(expenseId);
    }

    public Map<String, Object> calculateBalances(Long groupId, Long userId) {
        groupService.requireMember(groupId, userId);

        List<GroupMember> members = memberRepository.findByGroupId(groupId);
        List<Long> userIds = members.stream().map(GroupMember::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> users = loadUsers(new HashSet<>(userIds));

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        List<Long> expIds = expenses.stream().map(Expense::getId).collect(Collectors.toList());
        List<ExpenseSplit> splits = expIds.isEmpty() ? List.of() : splitRepository.findByExpenseIdIn(expIds);
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);

        Map<Long, BigDecimal> paid = new HashMap<>();
        Map<Long, BigDecimal> owed = new HashMap<>();
        Map<Long, BigDecimal> settled = new HashMap<>();

        userIds.forEach(uid -> {
            paid.put(uid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            owed.put(uid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            settled.put(uid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        });

        expenses.forEach(e -> {
            BigDecimal amt = convertToInr(e.getAmount(), e.getCurrency());
            paid.merge(e.getPaidBy(), amt, BigDecimal::add);
        });

        splits.forEach(p -> {
            Expense e = expenses.stream().filter(ex -> ex.getId().equals(p.getExpenseId())).findFirst().orElse(null);
            if (e != null) {
                BigDecimal inrAmt = convertToInr(p.getShareAmount(), e.getCurrency());
                owed.merge(p.getUserId(), inrAmt, BigDecimal::add);
            }
        });

        settlements.forEach(s -> {
            settled.merge(s.getPayerId(), s.getAmount(), BigDecimal::add);
            settled.merge(s.getReceiverId(), s.getAmount().negate(), BigDecimal::add);
        });

        BigDecimal totalExpenses = expenses.stream()
                .map(e -> convertToInr(e.getAmount(), e.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> memberBalances = userIds.stream().map(uid -> {
            BigDecimal net = paid.get(uid).subtract(owed.get(uid)).add(settled.get(uid));
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("userId", uid);
            User u = users.get(uid);
            b.put("displayName", u != null ? u.getDisplayName() : "Unknown");
            b.put("totalPaid", paid.get(uid));
            b.put("totalOwed", owed.get(uid));
            b.put("settledNet", settled.get(uid));
            b.put("balance", net);
            return b;
        }).sorted(Comparator.comparing(b -> (String) b.get("displayName"))).collect(Collectors.toList());

        Map<Long, BigDecimal> remaining = new HashMap<>();
        memberBalances.forEach(b -> remaining.put((Long) b.get("userId"), (BigDecimal) b.get("balance")));
        Map<Long, String> names = users.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDisplayName()));
        List<Map<String, Object>> suggestions = computeSettlements(remaining, names);

        return Map.of("totalExpenses", totalExpenses, "memberBalances", memberBalances, "suggestedSettlements", suggestions);
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private Expense findExpense(Long groupId, Long expenseId) {
        return expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> AppException.notFound("Expense not found"));
    }

    private void validateMemberOnDate(Long groupId, Long userId, LocalDate date) {
        memberRepository.findActiveMemberOnDate(groupId, userId, date)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "User " + userId + " was not an active member on " + date));
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private BigDecimal convertToInr(BigDecimal amount, String currency) {
        if (amount == null) return BigDecimal.ZERO;
        if ("USD".equalsIgnoreCase(currency)) {
            // Apply standard static exchange rate: 1 USD = 83 INR (implied currency rule)
            return amount.multiply(new BigDecimal("83.00")).setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private List<ExpenseSplit> buildSplits(Long expenseId, BigDecimal total, SplitType type, List<ParticipantInput> participants) {
        int n = participants.size();
        List<ExpenseSplit> result = new ArrayList<>();

        switch (type) {
            case EQUAL -> {
                BigDecimal share = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
                BigDecimal remainder = total.subtract(share.multiply(BigDecimal.valueOf(n)));
                for (int i = 0; i < n; i++) {
                    BigDecimal s = (i == n - 1) ? share.add(remainder) : share;
                    result.add(new ExpenseSplit(expenseId, participants.get(i).userId(), s));
                }
            }
            case EXACT -> {
                for (ParticipantInput p : participants) {
                    result.add(new ExpenseSplit(expenseId, p.userId(), p.shareAmount()));
                }
            }
            case PERCENTAGE -> {
                for (ParticipantInput p : participants) {
                    BigDecimal s = total.multiply(p.percentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    result.add(new ExpenseSplit(expenseId, p.userId(), s));
                }
            }
            case SHARES -> {
                BigDecimal totalShares = participants.stream()
                        .map(ParticipantInput::shares)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                for (int i = 0; i < n; i++) {
                    ParticipantInput p = participants.get(i);
                    BigDecimal s = total.multiply(p.shares()).divide(totalShares, 2, RoundingMode.HALF_UP);
                    result.add(new ExpenseSplit(expenseId, p.userId(), s));
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> computeSettlements(Map<Long, BigDecimal> remaining, Map<Long, String> names) {
        BigDecimal threshold = new BigDecimal("0.005");
        List<Map<String, Object>> suggestions = new ArrayList<>();
        int max = remaining.size() * 2;
        for (int i = 0; i < max; i++) {
            Optional<Map.Entry<Long, BigDecimal>> debtor = remaining.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(threshold.negate()) < 0)
                    .min(Map.Entry.comparingByValue());
            Optional<Map.Entry<Long, BigDecimal>> creditor = remaining.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(threshold) > 0)
                    .max(Map.Entry.comparingByValue());
            if (debtor.isEmpty() || creditor.isEmpty()) break;

            BigDecimal transfer = debtor.get().getValue().abs()
                    .min(creditor.get().getValue()).setScale(2, RoundingMode.HALF_UP);
            Long from = debtor.get().getKey();
            Long to = creditor.get().getKey();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("fromUserId", from);
            s.put("fromName", names.getOrDefault(from, "?"));
            s.put("toUserId", to);
            s.put("toName", names.getOrDefault(to, "?"));
            s.put("amount", transfer);
            suggestions.add(s);

            remaining.merge(from, transfer, BigDecimal::add);
            remaining.merge(to, transfer.negate(), BigDecimal::add);
        }
        return suggestions;
    }

    private Map<String, Object> toMap(Expense e, List<ExpenseSplit> splits) {
        Set<Long> uids = new HashSet<>(splits.stream().map(ExpenseSplit::getUserId).collect(Collectors.toList()));
        uids.add(e.getPaidBy());
        Map<Long, User> users = loadUsers(uids);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("groupId", e.getGroupId());
        m.put("description", e.getDescription());
        m.put("amount", e.getAmount());
        m.put("currency", e.getCurrency());
        m.put("expenseDate", e.getExpenseDate());
        m.put("paidBy", e.getPaidBy());
        User payer = users.get(e.getPaidBy());
        m.put("paidByName", payer != null ? payer.getDisplayName() : "Unknown");
        m.put("splitType", e.getSplitType());
        m.put("category", e.getCategory());
        m.put("participants", splits.stream().map(p -> {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("userId", p.getUserId());
            User u = users.get(p.getUserId());
            pm.put("displayName", u != null ? u.getDisplayName() : "Unknown");
            pm.put("shareAmount", p.getShareAmount());
            return pm;
        }).collect(Collectors.toList()));
        return m;
    }
}
