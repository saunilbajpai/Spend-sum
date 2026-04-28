package com.Spendsum.agent;

import com.Spendsum.model.AIInsight;
import com.Spendsum.model.ActionType;
import com.Spendsum.model.Budget;
import com.Spendsum.model.InsightSource;
import com.Spendsum.model.Severity;
import com.Spendsum.model.User;

import com.Spendsum.model.AnomalyType;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecisionEngine {

    public List<AIInsight> evaluateUserFinancials(
            User user,
            Double savings,
            String topCategory,
            Map<String, Double> categorySpending,
            List<Budget> budgets) {

        List<AIInsight> newInsights = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        int lengthOfMonth = YearMonth.of(today.getYear(), today.getMonth()).lengthOfMonth();
        double monthElapsedRatio = (double) dayOfMonth / lengthOfMonth;

        // Rule 1: Negative Savings
        if (savings < 0) {
            newInsights.add(AIInsight.builder()
                    .insightText("Your expenses exceed your income. You are in deficit!")
                    .action(ActionType.WARNING)
                    .severity(Severity.HIGH)
                    .executed(false)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .source(InsightSource.RULE_BASED)
                    .anomalyType(AnomalyType.DEFICIT)
                    .confidenceScore(0.95)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build());
        }

        // Rule 2: Top Spending Category
        if (topCategory != null && !topCategory.equals("No data")) {
            newInsights.add(AIInsight.builder()
                    .insightText("You are spending the most on " + topCategory + ". Try reducing it.")
                    .action(ActionType.SUGGESTION)
                    .severity(Severity.LOW)
                    .executed(false)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .source(InsightSource.RULE_BASED)
                    .anomalyType(AnomalyType.NONE)
                    .confidenceScore(0.80)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build());
        }

        // Rule 3: Budget checks
        if (budgets != null) {
            for (Budget budget : budgets) {
                String catName = budget.getCategory().getName();
                double spent = categorySpending.getOrDefault(catName, 0.0);
                double budgetUsedRatio = spent / budget.getLimitAmount();
                
                Integer estimatedDaysToExhaustion = null;
                if (dayOfMonth > 0 && spent > 0) {
                    double dailyVelocity = spent / dayOfMonth;
                    double remainingBudget = budget.getLimitAmount() - spent;
                    if (remainingBudget > 0) {
                        estimatedDaysToExhaustion = (int) (remainingBudget / dailyVelocity);
                    } else {
                        estimatedDaysToExhaustion = 0;
                    }
                }

                if (spent > budget.getLimitAmount()) {
                    newInsights.add(AIInsight.builder()
                            .insightText("You exceeded your budget in " + catName + "!")
                            .action(ActionType.ALERT)
                            .severity(Severity.HIGH)
                            .executed(false)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .source(InsightSource.RULE_BASED)
                            .anomalyType(AnomalyType.OVER_BUDGET)
                            .estimatedDaysToExhaustion(0)
                            .confidenceScore(1.0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
                } else if (budgetUsedRatio > monthElapsedRatio * 1.5 && estimatedDaysToExhaustion != null && estimatedDaysToExhaustion < (lengthOfMonth - dayOfMonth)) {
                    newInsights.add(AIInsight.builder()
                            .insightText(String.format("Velocity Alert: You are spending too fast in %s. Budget will be exhausted in %d days.", catName, estimatedDaysToExhaustion))
                            .action(ActionType.WARNING)
                            .severity(Severity.HIGH)
                            .executed(false)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .source(InsightSource.RULE_BASED)
                            .anomalyType(AnomalyType.VELOCITY)
                            .estimatedDaysToExhaustion(estimatedDaysToExhaustion)
                            .confidenceScore(0.85)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
                } else if (spent > budget.getLimitAmount() * 0.8) {
                    newInsights.add(AIInsight.builder()
                            .insightText("You are close to exceeding your budget in " + catName + ".")
                            .action(ActionType.WARNING)
                            .severity(Severity.MEDIUM)
                            .executed(false)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .source(InsightSource.RULE_BASED)
                            .anomalyType(AnomalyType.NONE)
                            .estimatedDaysToExhaustion(estimatedDaysToExhaustion)
                            .confidenceScore(0.90)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
                }
            }
        }

        return newInsights;
    }
}
