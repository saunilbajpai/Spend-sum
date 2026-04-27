package com.Spendsum.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.Spendsum.model.Transaction;
import com.Spendsum.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ✅ Create
    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.createTransaction(transaction));
    }

    // ✅ Get all
    @GetMapping
    public ResponseEntity<List<Transaction>> getAll() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // ✅ Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    // ✅ Get by User
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    // ✅ Update
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id,
                                              @RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, transaction));
    }

    // ✅ Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok("Transaction deleted successfully");
    }

    // 🔥 Monthly Summary API
    @GetMapping("/summary/{userId}/{month}/{year}")
    public ResponseEntity<Map<String, Double>> getMonthlySummary(
            @PathVariable Long userId,
            @PathVariable int month,
            @PathVariable int year) {

        return ResponseEntity.ok(
                transactionService.getMonthlySummary(userId, month, year)
        );
    }

    // 🔥 Category-wise Spending API
    @GetMapping("/category-wise/{userId}")
    public ResponseEntity<Map<String, Double>> getCategoryWise(@PathVariable Long userId) {
        return ResponseEntity.ok(
                transactionService.getCategoryWiseSpending(userId)
        );
    }

    // 🔥 Income vs Expense API
    @GetMapping("/income-expense/{userId}")
    public ResponseEntity<Map<String, Double>> getIncomeExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(
                transactionService.getIncomeVsExpense(userId)
        );
    }

    // 🔥 Savings API
    @GetMapping("/savings/{userId}")
    public ResponseEntity<Double> getSavings(@PathVariable Long userId) {
        return ResponseEntity.ok(
                transactionService.getSavings(userId)
        );
    }

    // 🔥 Top Category API
    @GetMapping("/top-category/{userId}")
    public ResponseEntity<String> getTopCategory(@PathVariable Long userId) {
        return ResponseEntity.ok(
                transactionService.getTopSpendingCategory(userId)
        );
    }
}