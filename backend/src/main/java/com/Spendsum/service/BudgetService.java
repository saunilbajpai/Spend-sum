package com.Spendsum.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Spendsum.model.Budget;
import com.Spendsum.repository.BudgetRepository;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionService transactionService;

    // ✅ Create Budget
    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    // ✅ Get all budgets
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    // ✅ Get budgets by user
    public List<Budget> getBudgetsByUser(Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    // ✅ Get by ID
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
    }

    // ✅ Delete
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    // 🔥 1. Check if Budget Exceeded
    public boolean isBudgetExceeded(Long userId, Long categoryId, int month, int year) {

        Budget budget = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        Map<String, Double> categorySpending =
                transactionService.getCategoryWiseSpending(userId);

        String categoryName = budget.getCategory().getName();

        double spent = categorySpending.getOrDefault(categoryName, 0.0);

        return spent > budget.getLimitAmount();
    }

    // 🔥 2. Remaining Budget
    public double getRemainingBudget(Long userId, Long categoryId, int month, int year) {

        Budget budget = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        Map<String, Double> categorySpending =
                transactionService.getCategoryWiseSpending(userId);

        String categoryName = budget.getCategory().getName();

        double spent = categorySpending.getOrDefault(categoryName, 0.0);

        return budget.getLimitAmount() - spent;
    }

    // 🔥 3. Budget Status (BEST FEATURE)
    public String getBudgetStatus(Long userId, Long categoryId, int month, int year) {

        Budget budget = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        Map<String, Double> categorySpending =
                transactionService.getCategoryWiseSpending(userId);

        String categoryName = budget.getCategory().getName();

        double spent = categorySpending.getOrDefault(categoryName, 0.0);
        double limit = budget.getLimitAmount();

        double percentage = (spent / limit) * 100;

        if (percentage >= 100) {
            return "⚠️ Budget exceeded!";
        } else if (percentage >= 80) {
            return "⚡ Warning: You have used " + percentage + "% of your budget";
        } else {
            return "✅ You are within budget";
        }
    }
}