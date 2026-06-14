package com.sharedexpenses.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    List<ExpenseSplit> findByExpenseIdIn(List<Long> expenseIds);

    @Modifying
    @Query("DELETE FROM ExpenseSplit p WHERE p.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);
}
