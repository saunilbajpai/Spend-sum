package com.Spendsum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Spendsum.model.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Find budget by user + category + month + year
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
            Long userId,
            Long categoryId,
            int month,
            int year
    );

    // Find budgets by user
    List<Budget> findByUserId(Long userId);
}