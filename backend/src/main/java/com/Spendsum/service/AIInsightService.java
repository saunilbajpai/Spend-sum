package com.Spendsum.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.Spendsum.model.AIInsight;
import com.Spendsum.model.ActionType;
import com.Spendsum.model.Severity;
import com.Spendsum.model.User;
import com.Spendsum.repository.AIInsightRepository;

@Service
@RequiredArgsConstructor
public class AIInsightService {

    private final AIInsightRepository aiInsightRepository;
    private final TransactionService transactionService;
    private final BudgetService budgetService;

    // ✅ Generate Insights for User
    public List<AIInsight> generateInsights(User user) {

        List<AIInsight> insights = new ArrayList<>();

        Long userId = user.getId();

        // 🔥 1. Top Spending Category
        String topCategory = transactionService.getTopSpendingCategory(userId);

        AIInsight insight1 = AIInsight.builder()
                .insightText("You are spending the most on " + topCategory + ". Try reducing it.")
                .action(ActionType.SUGGESTION)
                .severity(Severity.LOW)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        insights.add(insight1);

        // 🔥 2. Savings Insight
        Double savings = transactionService.getSavings(userId);

        if (savings < 0) {
            insights.add(AIInsight.builder()
                    .insightText("Your expenses exceed your income. You are in deficit!")
                    .action(ActionType.WARNING)
                    .severity(Severity.HIGH)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build());
        } else {
            insights.add(AIInsight.builder()
                    .insightText("Good job! You are saving money this period.")
                    .action(ActionType.SUGGESTION)
                    .severity(Severity.LOW)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build());
        }

        // 🔥 3. Budget Alerts (for each category)
        Map<String, Double> categorySpending =
                transactionService.getCategoryWiseSpending(userId);

        for (String categoryName : categorySpending.keySet()) {

            // NOTE: This assumes budget exists — can improve later
            try {
                // You can improve by passing month/year dynamically
                boolean exceeded = false; // placeholder

                if (exceeded) {
                    insights.add(AIInsight.builder()
                            .insightText("You exceeded your budget in " + categoryName)
                            .action(ActionType.ALERT)
                            .severity(Severity.HIGH)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .build());
                }
            } catch (Exception e) {
                // ignore if no budget exists
            }
        }

        // Save all insights
        return aiInsightRepository.saveAll(insights);
    }

    // ✅ Get user insights
    public List<AIInsight> getInsightsByUser(Long userId) {
        return aiInsightRepository.findByUserId(userId);
    }

    // ✅ Set Helpful Status (Feedback Loop)
    public AIInsight setHelpfulStatus(Long insightId, Boolean isHelpful) {
        AIInsight insight = aiInsightRepository.findById(insightId)
                .orElseThrow(() -> new RuntimeException("Insight not found"));
        insight.setIsHelpful(isHelpful);
        return aiInsightRepository.save(insight);
    }

    // ✅ Delete insight
    public void deleteInsight(Long id) {
        aiInsightRepository.deleteById(id);
    }
}