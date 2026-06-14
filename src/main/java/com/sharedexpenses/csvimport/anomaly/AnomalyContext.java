package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.expense.Expense;
import com.sharedexpenses.group.GroupMember;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class AnomalyContext {

    private final Long groupId;

    
    private final List<Expense> existingExpenses;

    
    private final List<GroupMember> allGroupMembers;

    
    private final Set<Long> allMemberUserIds;

    public AnomalyContext(Long groupId, List<Expense> existingExpenses,
                          List<GroupMember> allGroupMembers) {
        this.groupId = groupId;
        this.existingExpenses = existingExpenses;
        this.allGroupMembers = allGroupMembers;
        this.allMemberUserIds = allGroupMembers.stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());
    }

    public Long getGroupId() { return groupId; }
    public List<Expense> getExistingExpenses() { return existingExpenses; }
    public List<GroupMember> getAllGroupMembers() { return allGroupMembers; }
    public Set<Long> getAllMemberUserIds() { return allMemberUserIds; }

    
    public boolean wasMemberActiveOn(Long userId, LocalDate date) {
        if (date == null) return true; 
        return allGroupMembers.stream()
                .filter(m -> m.getUserId().equals(userId))
                .anyMatch(m -> m.isActiveOn(date));
    }
}
