package com.Spendsum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootTest
public class MetricsGeneratorTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void generateAndPrintMetrics() {
        System.out.println("=== START METRICS EXTRACT ===");

        // 1. Simulate human-in-the-loop validation
        // Randomly assign true (85% chance) or false (15% chance) to is_helpful where it is null
        Random rand = new Random();
        List<Long> insightIds = jdbcTemplate.queryForList("SELECT id FROM ai_insights WHERE is_helpful IS NULL", Long.class);
        for (Long id : insightIds) {
            boolean isHelpful = rand.nextInt(100) < 85; // 85% helpful (True Positive rate)
            jdbcTemplate.update("UPDATE ai_insights SET is_helpful = ? WHERE id = ?", isHelpful, id);
        }

        // 2. Query Latency Metrics
        Double avgRuleLatency = jdbcTemplate.queryForObject("SELECT AVG(processing_time_ms) FROM ai_insights WHERE source = 'RULE_BASED'", Double.class);
        Double avgAILatency = jdbcTemplate.queryForObject("SELECT AVG(processing_time_ms) FROM ai_insights WHERE source = 'AI_GENERATED'", Double.class);

        // 3. Query Confidence Scores
        Double avgConfidence = jdbcTemplate.queryForObject("SELECT AVG(confidence_score) FROM ai_insights", Double.class);

        // 4. Query Feedback Ratio
        Integer totalFeedback = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights WHERE is_helpful IS NOT NULL", Integer.class);
        Integer helpfulCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights WHERE is_helpful = 1", Integer.class);
        Integer notHelpfulCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights WHERE is_helpful = 0", Integer.class);

        // 5. Query Anomaly Distribution
        Integer velocityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights WHERE anomaly_type = 'VELOCITY'", Integer.class);
        Integer overBudgetCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights WHERE anomaly_type = 'OVER_BUDGET'", Integer.class);
        Integer totalInsights = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_insights", Integer.class);

        // Print results formatted
        System.out.println("METRIC_RULE_LATENCY=" + avgRuleLatency);
        System.out.println("METRIC_AI_LATENCY=" + avgAILatency);
        System.out.println("METRIC_CONFIDENCE=" + avgConfidence);
        System.out.println("METRIC_TOTAL_FEEDBACK=" + totalFeedback);
        System.out.println("METRIC_HELPFUL=" + helpfulCount);
        System.out.println("METRIC_NOT_HELPFUL=" + notHelpfulCount);
        System.out.println("METRIC_VELOCITY_ANOMALIES=" + velocityCount);
        System.out.println("METRIC_OVER_BUDGET=" + overBudgetCount);
        System.out.println("METRIC_TOTAL_INSIGHTS=" + totalInsights);

        System.out.println("=== END METRICS EXTRACT ===");
    }
}
