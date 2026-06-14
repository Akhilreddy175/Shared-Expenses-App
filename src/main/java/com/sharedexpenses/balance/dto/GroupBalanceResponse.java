package com.sharedexpenses.balance.dto;

import com.sharedexpenses.group.Group;

import java.math.BigDecimal;
import java.util.List;


public class GroupBalanceResponse {

    private final Long groupId;
    private final String groupName;
    private final BigDecimal totalExpenses;
    private final List<UserBalanceResponse> memberBalances;

    private GroupBalanceResponse(Long groupId, String groupName,
                                 BigDecimal totalExpenses, List<UserBalanceResponse> memberBalances) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.totalExpenses = totalExpenses;
        this.memberBalances = memberBalances;
    }

    public static GroupBalanceResponse of(Group group, BigDecimal totalExpenses,
                                          List<UserBalanceResponse> memberBalances) {
        return new GroupBalanceResponse(group.getId(), group.getName(), totalExpenses, memberBalances);
    }

    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public List<UserBalanceResponse> getMemberBalances() { return memberBalances; }
}
