package com.sharedexpenses.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.sharedexpenses.expense.split.SplitStrategy;
import com.sharedexpenses.expense.split.SplitStrategyFactory;
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
    private final SplitStrategyFactory splitStrategyFactory;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseParticipantRepository participantRepository,
                          GroupMemberRepository groupMemberRepository,
                          GroupService groupService,
                          UserRepository userRepository,
                          SplitStrategyFactory splitStrategyFactory) {
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
        this.splitStrategyFactory = splitStrategyFactory;
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

        List<BigDecimal> shareAmounts = computeSharesUsingStrategy(request);

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
        List<BigDecimal> shareAmounts = computeSharesUsingStrategy(request);

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

    private List<BigDecimal> computeSharesUsingStrategy(CreateExpenseRequest request) {
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        return strategy.split(request.getAmount(), request.getParticipants());
    }

    private void validateMemberOnDate(Long groupId, Long userId, LocalDate date, String errorMessage) {
        groupMemberRepository.findActiveMemberOnDate(groupId, userId, date)
                .orElseThrow(() -> new ValidationException(errorMessage));
    }

    private Expense findExpenseOrThrow(Long groupId, Long expenseId) {
        return expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense " + expenseId + " not found in this group"));
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
