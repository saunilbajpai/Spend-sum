package com.Spendsum.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Spendsum.model.AIInsight;
import com.Spendsum.model.User;
import com.Spendsum.repository.AIInsightRepository;

@Service
public class AIInsightService {

    @Autowired
    private AIInsightRepository aiInsightRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    // ✅ Generate Insights for User
    public List<AIInsight> generateInsights(User user) {

        List<AIInsight> insights = new ArrayList<>();

        Long userId = user.getId();

        // 🔥 1. Top Spending Category
        String topCategory = transactionService.getTopSpendingCategory(userId);

        AIInsight insight1 = AIInsight.builder()
                .insightText("You are spending the most on " + topCategory + ". Try reducing it.")
                .type("SUGGESTION")
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        insights.add(insight1);

        // 🔥 2. Savings Insight
        Double savings = transactionService.getSavings(userId);

        if (savings < 0) {
            insights.add(AIInsight.builder()
                    .insightText("Your expenses exceed your income. You are in deficit!")
                    .type("WARNING")
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build());
        } else {
            insights.add(AIInsight.builder()
                    .insightText("Good job! You are saving money this period.")
                    .type("INFO")
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
                            .type("WARNING")
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

    // ✅ Delete insight
    public void deleteInsight(Long id) {
        aiInsightRepository.deleteById(id);
    }
}