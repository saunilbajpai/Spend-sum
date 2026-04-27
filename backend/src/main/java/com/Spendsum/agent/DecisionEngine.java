package com.Spendsum.agent;

import com.Spendsum.model.AIInsight;
import com.Spendsum.model.ActionType;
import com.Spendsum.model.Budget;
import com.Spendsum.model.InsightSource;
import com.Spendsum.model.Severity;
import com.Spendsum.model.User;

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
                    .build());
        }

        // Rule 3: Budget checks
        if (budgets != null) {
            for (Budget budget : budgets) {
                String catName = budget.getCategory().getName();
                double spent = categorySpending.getOrDefault(catName, 0.0);
                if (spent > budget.getLimitAmount()) {
                    newInsights.add(AIInsight.builder()
                            .insightText("You exceeded your budget in " + catName + "!")
                            .action(ActionType.ALERT)
                            .severity(Severity.HIGH)
                            .executed(false)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                    .source(InsightSource.RULE_BASED)
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
                            .build());
                }
            }
        }

        return newInsights;
    }
}
