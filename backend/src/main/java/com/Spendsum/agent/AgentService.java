package com.Spendsum.agent;

import com.Spendsum.ai.GeminiService;
import com.Spendsum.model.AIInsight;
import com.Spendsum.model.ActionType;
import com.Spendsum.model.Budget;
import com.Spendsum.model.InsightSource;
import com.Spendsum.model.Severity;
import com.Spendsum.model.User;
import com.Spendsum.repository.AIInsightRepository;
import com.Spendsum.repository.UserRepository;
import com.Spendsum.service.BudgetService;
import com.Spendsum.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final AIInsightRepository aiInsightRepository;
    private final GeminiService geminiService;

    private final DecisionEngine decisionEngine = new DecisionEngine();

    @Async
    public void processUser(Long userId) {
        log.info("Starting background agent processing for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch user data
        Double savings = transactionService.getSavings(userId);
        String topCategory = transactionService.getTopSpendingCategory(userId);
        Map<String, Double> categorySpending = transactionService.getCategoryWiseSpending(userId);
        List<Budget> budgets = budgetService.getBudgetsByUser(userId);

        // Generate insights via Decision Engine
        List<AIInsight> newInsights = decisionEngine.evaluateUserFinancials(
                user, savings, topCategory, categorySpending, budgets);

        // Gemini AI Integration
        Map<String, Double> incomeExpense = transactionService.getIncomeVsExpense(userId);
        Double income = incomeExpense.getOrDefault("income", 0.0);
        Double expense = incomeExpense.getOrDefault("expense", 0.0);

        long activeBudgets = budgets.size();
        long exceededBudgets = budgets.stream().filter(b -> {
            String catName = b.getCategory().getName();
            double spent = categorySpending.getOrDefault(catName, 0.0);
            return spent > b.getLimitAmount();
        }).count();

        // Extract context for Gemini based on detected anomalies
        String anomalyContext = newInsights.stream()
            .filter(i -> i.getAnomalyType() == com.Spendsum.model.AnomalyType.VELOCITY || i.getAnomalyType() == com.Spendsum.model.AnomalyType.OVER_BUDGET)
            .map(AIInsight::getInsightText)
            .collect(Collectors.joining(" "));

        String prompt = String.format(
                "User spending data: Total income: %.2f, Total expense: %.2f, Top category: %s, Savings: %.2f. " +
                "User has %d active budgets, %d of which are exceeded. " +
                "%s" +
                "Provide highly personalized and actionable financial advice in 2-3 lines.",
                income, expense, topCategory, savings, activeBudgets, exceededBudgets,
                anomalyContext.isEmpty() ? "" : "CRITICAL ALERTS: " + anomalyContext + " ");

        log.info("Querying Gemini API for user ID: {}", userId);
        long aiStartTime = System.currentTimeMillis();
        String aiText = geminiService.generateInsight(prompt);
        long aiLatency = System.currentTimeMillis() - aiStartTime;

        if (aiText != null && !aiText.trim().isEmpty()) {
            newInsights.add(AIInsight.builder()
                    .insightText(aiText.trim())
                    .action(ActionType.SUGGESTION)
                    .severity(Severity.MEDIUM)
                    .source(InsightSource.AI_GENERATED)
                    .anomalyType(anomalyContext.isEmpty() ? com.Spendsum.model.AnomalyType.NONE : com.Spendsum.model.AnomalyType.VELOCITY)
                    .processingTimeMs(aiLatency)
                    .confidenceScore(0.85) // LLM confidence proxy
                    .executed(false)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build());
        }

        // Fetch existing insights to avoid duplicates
        List<AIInsight> existingInsights = aiInsightRepository.findByUserId(userId);
        List<String> existingTexts = existingInsights.stream()
                .map(AIInsight::getInsightText)
                .collect(Collectors.toList());

        // Filter out duplicates and save
        List<AIInsight> insightsToSave = newInsights.stream()
                .filter(insight -> !existingTexts.contains(insight.getInsightText()))
                .collect(Collectors.toList());

        if (!insightsToSave.isEmpty()) {
            aiInsightRepository.saveAll(insightsToSave);
            log.info("Saved {} new insights for user ID: {}", insightsToSave.size(), userId);
        } else {
            log.info("No new unique insights generated for user ID: {}", userId);
        }
    }
}
