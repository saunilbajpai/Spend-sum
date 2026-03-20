package com.Spendsum.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Spendsum.model.Budget;
import com.Spendsum.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    // ✅ Create
    @PostMapping
    public ResponseEntity<Budget> create(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.createBudget(budget));
    }

    // ✅ Get all
    @GetMapping
    public ResponseEntity<List<Budget>> getAll() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    // ✅ Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<Budget> getById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    // ✅ Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok("Budget deleted successfully");
    }

    // 🔥 Check if exceeded
    @GetMapping("/exceeded/{userId}/{categoryId}/{month}/{year}")
    public ResponseEntity<Boolean> isExceeded(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @PathVariable int month,
            @PathVariable int year) {

        return ResponseEntity.ok(
                budgetService.isBudgetExceeded(userId, categoryId, month, year)
        );
    }

    // 🔥 Remaining budget
    @GetMapping("/remaining/{userId}/{categoryId}/{month}/{year}")
    public ResponseEntity<Double> getRemaining(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @PathVariable int month,
            @PathVariable int year) {

        return ResponseEntity.ok(
                budgetService.getRemainingBudget(userId, categoryId, month, year)
        );
    }

    // 🔥 Budget status
    @GetMapping("/status/{userId}/{categoryId}/{month}/{year}")
    public ResponseEntity<String> getStatus(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @PathVariable int month,
            @PathVariable int year) {

        return ResponseEntity.ok(
                budgetService.getBudgetStatus(userId, categoryId, month, year)
        );
    }
}