package com.sharedexpenses.expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {

    List<ExpenseParticipant> findByExpenseId(Long expenseId);

    List<ExpenseParticipant> findByExpenseIdIn(List<Long> expenseIds);

    @Modifying
    @Query("DELETE FROM ExpenseParticipant p WHERE p.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);
}
