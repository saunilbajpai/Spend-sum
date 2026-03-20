package com.Spendsum.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Spendsum.model.Transaction;
import com.Spendsum.repository.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    // ✅ Create
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    // ✅ Get all
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // ✅ Get by ID
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    // ✅ Get by User
    public List<Transaction> getTransactionsByUser(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    // ✅ Update
    public Transaction updateTransaction(Long id, Transaction updated) {
        Transaction existing = getTransactionById(id);

        existing.setAmount(updated.getAmount());
        existing.setDescription(updated.getDescription());
        existing.setDate(updated.getDate());
        existing.setCategory(updated.getCategory());
        existing.setType(updated.getType());

        return transactionRepository.save(existing);
    }

    // ✅ Delete
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    // 🔥 1. Monthly Summary
    public Map<String, Double> getMonthlySummary(Long userId, int month, int year) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        double income = 0;
        double expense = 0;

        for (Transaction t : transactions) {
            if (t.getDate().getMonthValue() == month &&
                t.getDate().getYear() == year) {

                if (t.getType().equalsIgnoreCase("INCOME")) {
                    income += t.getAmount();
                } else {
                    expense += t.getAmount();
                }
            }
        }

        Map<String, Double> summary = new HashMap<>();
        summary.put("income", income);
        summary.put("expense", expense);
        summary.put("savings", income - expense);

        return summary;
    }

    // 🔥 2. Category-wise Spending
    public Map<String, Double> getCategoryWiseSpending(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // 🔥 3. Income vs Expense
    public Map<String, Double> getIncomeVsExpense(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        double income = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("INCOME"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("EXPENSE"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        Map<String, Double> result = new HashMap<>();
        result.put("income", income);
        result.put("expense", expense);

        return result;
    }

    // 🔥 4. Savings
    public Double getSavings(Long userId) {
        Map<String, Double> data = getIncomeVsExpense(userId);
        return data.get("income") - data.get("expense");
    }

    // 🔥 5. Top Spending Category
    public String getTopSpendingCategory(Long userId) {
        Map<String, Double> categoryMap = getCategoryWiseSpending(userId);

        return categoryMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No data");
    }
}