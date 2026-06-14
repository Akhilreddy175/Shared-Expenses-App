package com.sharedexpenses.expense;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByExpenseDateDesc(Long groupId);

    // Used by balance engine — order doesn't matter when summing amounts
    List<Expense> findByGroupId(Long groupId);

    // Used to verify an expense belongs to the group before returning it —
    // prevents a member of group B from fetching an expense from group A by guessing IDs
    Optional<Expense> findByIdAndGroupId(Long id, Long groupId);
}
