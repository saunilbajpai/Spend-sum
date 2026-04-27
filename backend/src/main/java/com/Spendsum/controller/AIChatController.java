package com.Spendsum.controller;

import com.Spendsum.agent.AgentService;
import com.Spendsum.ai.GeminiService;
import com.Spendsum.model.User;
import com.Spendsum.repository.UserRepository;
import com.Spendsum.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    private final GeminiService geminiService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final AgentService agentService;

    // ✅ Test Gemini API directly
    @PostMapping("/generate")
    public ResponseEntity<String> generateInsight(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        log.info("Directly testing Gemini API for user ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch Data
        Double savings = transactionService.getSavings(userId);
        String topCategory = transactionService.getTopSpendingCategory(userId);
        Map<String, Double> incomeExpense = transactionService.getIncomeVsExpense(userId);
        Double income = incomeExpense.getOrDefault("income", 0.0);
        Double expense = incomeExpense.getOrDefault("expense", 0.0);

        String prompt = String.format(
                "User spending data: Total income: %.2f, Total expense: %.2f, Top category: %s, Savings: %.2f. " +
                "Give personalized financial advice in 2-3 lines.",
                income, expense, topCategory, savings);

        String response = geminiService.generateInsight(prompt);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body("Failed to generate insight from Gemini API.");
        }
    }

    // ✅ Test Agent pipeline manually
    @PostMapping("/trigger-agent")
    public ResponseEntity<String> triggerAgent(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("userId is required");
        }
        
        log.info("Manually triggering Agent pipeline for user ID: {}", userId);
        agentService.processUser(userId);
        return ResponseEntity.ok("Agent processing triggered for user " + userId);
    }
}
