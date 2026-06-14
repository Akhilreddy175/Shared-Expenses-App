package com.sharedexpenses.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharedexpenses.common.ResourceNotFoundException;
import com.sharedexpenses.common.ValidationException;
import com.sharedexpenses.expense.dto.CreateExpenseRequest;
import com.sharedexpenses.expense.dto.ExpenseParticipantResponse;
import com.sharedexpenses.expense.dto.ExpenseResponse;
import com.sharedexpenses.expense.dto.ExpenseSummaryResponse;
import com.sharedexpenses.expense.dto.ParticipantRequest;
import com.sharedexpenses.group.GroupMemberRepository;
import com.sharedexpenses.group.GroupService;
import com.sharedexpenses.user.User;
import com.sharedexpenses.user.UserRepository;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository participantRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseParticipantRepository participantRepository,
                          GroupMemberRepository groupMemberRepository,
                          GroupService groupService,
                          UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest request, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        validateMemberOnDate(groupId, request.getPaidBy(), request.getExpenseDate(),
                "The person who paid was not an active member of this group on " + request.getExpenseDate());

        for (ParticipantRequest p : request.getParticipants()) {
            validateMemberOnDate(groupId, p.getUserId(), request.getExpenseDate(),
                    "Participant (user ID: " + p.getUserId() + ") was not an active member on " + request.getExpenseDate());
        }

        List<Long> participantIds = request.getParticipants().stream()
                .map(ParticipantRequest::getUserId)
                .collect(Collectors.toList());
        List<BigDecimal> shareAmounts = computeShares(request);

        Expense expense = new Expense(
                groupId,
                request.getDescription().trim(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                request.getExpenseDate(),
                request.getPaidBy(),
                request.getSplitType(),
                request.getCategory() != null ? request.getCategory().trim() : null,
                currentUserId
        );
        Expense saved = expenseRepository.save(expense);

        List<ExpenseParticipant> participants = new ArrayList<>();
        for (int i = 0; i < participantIds.size(); i++) {
            participants.add(new ExpenseParticipant(saved.getId(), participantIds.get(i), shareAmounts.get(i)));
        }
        List<ExpenseParticipant> savedParticipants = participantRepository.saveAll(participants);

        return buildExpenseResponse(saved, savedParticipants);
    }

    public List<ExpenseSummaryResponse> listExpenses(Long groupId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);

        List<Expense> expenses = expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId);
        if (expenses.isEmpty()) return List.of();

        List<Long> expenseIds = expenses.stream().map(Expense::getId).collect(Collectors.toList());
        List<ExpenseParticipant> allParticipants = participantRepository.findByExpenseIdIn(expenseIds);

        Map<Long, Long> countByExpenseId = allParticipants.stream()
                .collect(Collectors.groupingBy(ExpenseParticipant::getExpenseId, Collectors.counting()));

        List<Long> payerIds = expenses.stream().map(Expense::getPaidBy).distinct().collect(Collectors.toList());
        Map<Long, User> usersById = userRepository.findAllById(payerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return expenses.stream()
                .map(e -> ExpenseSummaryResponse.from(
                        e,
                        usersById.get(e.getPaidBy()).getDisplayName(),
                        countByExpenseId.getOrDefault(e.getId(), 0L).intValue()
                ))
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpense(Long groupId, Long expenseId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        Expense expense = findExpenseOrThrow(groupId, expenseId);
        List<ExpenseParticipant> participants = participantRepository.findByExpenseId(expenseId);
        return buildExpenseResponse(expense, participants);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long groupId, Long expenseId,
                                         CreateExpenseRequest request, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        Expense expense = findExpenseOrThrow(groupId, expenseId);

        validateMemberOnDate(groupId, request.getPaidBy(), request.getExpenseDate(),
                "The person who paid was not an active member on " + request.getExpenseDate());
        for (ParticipantRequest p : request.getParticipants()) {
            validateMemberOnDate(groupId, p.getUserId(), request.getExpenseDate(),
                    "Participant (user ID: " + p.getUserId() + ") was not an active member on " + request.getExpenseDate());
        }

        expense.setDescription(request.getDescription().trim());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency().toUpperCase());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaidBy(request.getPaidBy());
        expense.setSplitType(request.getSplitType());
        expense.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        Expense saved = expenseRepository.save(expense);

        List<Long> participantIds = request.getParticipants().stream()
                .map(ParticipantRequest::getUserId).collect(Collectors.toList());
        List<BigDecimal> shareAmounts = computeShares(request);

        participantRepository.deleteByExpenseId(expenseId);
        participantRepository.flush();

        List<ExpenseParticipant> newParticipants = new ArrayList<>();
        for (int i = 0; i < participantIds.size(); i++) {
            newParticipants.add(new ExpenseParticipant(expenseId, participantIds.get(i), shareAmounts.get(i)));
        }
        List<ExpenseParticipant> savedParticipants = participantRepository.saveAll(newParticipants);

        return buildExpenseResponse(saved, savedParticipants);
    }

    @Transactional
    public void deleteExpense(Long groupId, Long expenseId, Long currentUserId) {
        groupService.requireMembership(groupId, currentUserId);
        Expense expense = findExpenseOrThrow(groupId, expenseId);
        expenseRepository.delete(expense);
    }

    private List<BigDecimal> computeShares(CreateExpenseRequest request) {
        return switch (request.getSplitType()) {
            case EQUAL -> computeEqualShares(request.getAmount(), request.getParticipants().size());
            case EXACT -> computeExactShares(request);
            case PERCENTAGE -> computePercentageShares(request);
        };
    }

    private List<BigDecimal> computeEqualShares(BigDecimal total, int count) {
        BigDecimal perPerson = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal distributed = perPerson.multiply(BigDecimal.valueOf(count - 1));
        BigDecimal lastShare = total.subtract(distributed);

        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < count - 1; i++) {
            shares.add(perPerson);
        }
        shares.add(lastShare);
        return shares;
    }

    private List<BigDecimal> computeExactShares(CreateExpenseRequest request) {
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (ParticipantRequest p : request.getParticipants()) {
            if (p.getShareAmount() == null) {
                throw new ValidationException("shareAmount is required for each participant when using EXACT split");
            }
            shares.add(p.getShareAmount().setScale(2, RoundingMode.HALF_UP));
            sum = sum.add(p.getShareAmount());
        }

        BigDecimal diff = request.getAmount().subtract(sum).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException(
                    "The sum of participant share amounts (" + sum + ") must equal the expense amount (" + request.getAmount() + ")");
        }

        return shares;
    }

    private List<BigDecimal> computePercentageShares(CreateExpenseRequest request) {
        BigDecimal percentageSum = BigDecimal.ZERO;
        for (ParticipantRequest p : request.getParticipants()) {
            if (p.getPercentage() == null) {
                throw new ValidationException("percentage is required for each participant when using PERCENTAGE split");
            }
            percentageSum = percentageSum.add(p.getPercentage());
        }

        BigDecimal diff = new BigDecimal("100").subtract(percentageSum).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException(
                    "Percentages must sum to 100 (got " + percentageSum + ")");
        }

        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal distributed = BigDecimal.ZERO;
        int last = request.getParticipants().size() - 1;

        for (int i = 0; i < request.getParticipants().size(); i++) {
            if (i == last) {
                shares.add(request.getAmount().subtract(distributed));
            } else {
                BigDecimal share = request.getAmount()
                        .multiply(request.getParticipants().get(i).getPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
                shares.add(share);
                distributed = distributed.add(share);
            }
        }
        return shares;
    }

    private void validateMemberOnDate(Long groupId, Long userId, java.time.LocalDate date, String errorMessage) {
        groupMemberRepository.findActiveMemberOnDate(groupId, userId, date)
                .orElseThrow(() -> new ValidationException(errorMessage));
    }

    private Expense findExpenseOrThrow(Long groupId, Long expenseId) {
        return expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense " + expenseId + " not found in this group"));
    }

    private ExpenseResponse buildExpenseResponse(Expense expense, List<ExpenseParticipant> participants) {
        List<Long> userIds = new ArrayList<>(participants.stream()
                .map(ExpenseParticipant::getUserId).collect(Collectors.toList()));
        if (!userIds.contains(expense.getPaidBy())) userIds.add(expense.getPaidBy());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ExpenseParticipantResponse> participantResponses = participants.stream()
                .map(p -> ExpenseParticipantResponse.from(p, usersById.get(p.getUserId())))
                .collect(Collectors.toList());

        String paidByName = usersById.get(expense.getPaidBy()).getDisplayName();
        return ExpenseResponse.from(expense, paidByName, participantResponses);
    }
}
