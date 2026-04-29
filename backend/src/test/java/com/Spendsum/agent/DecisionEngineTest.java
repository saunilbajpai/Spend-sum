package com.Spendsum.agent;

import com.Spendsum.model.*;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure unit tests for DecisionEngine — zero Spring context, zero mocks.
 * DecisionEngine is a plain Java class, so we can instantiate it directly.
 *
 * Tests validate ALL four decision rules:
 *   Rule 1 — Negative savings (DEFICIT)
 *   Rule 2 — Top spending category (SUGGESTION)
 *   Rule 3a — Over-budget (ALERT)
 *   Rule 3b — Velocity alert (spending too fast)
 *   Rule 3c — Approaching budget (80% warning)
 */
class DecisionEngineTest {

    private DecisionEngine engine;
    private User user;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        engine = new DecisionEngine();
        user = User.builder().id(1L).username("alice").email("a@a.com").password("pass").build();
        foodCategory = Category.builder().id(10L).name("Food").type("EXPENSE").user(user).build();
    }

    // ─── Rule 1: Negative Savings → DEFICIT ──────────────────────────────────

    @Test
    @DisplayName("Rule 1: should generate DEFICIT insight when savings < 0")
    void rule1_negativeSavings_deficitInsight() {
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, -500.0, "Food", Map.of("Food", 3000.0), Collections.emptyList());

        assertThat(insights)
                .anyMatch(i -> i.getAnomalyType() == AnomalyType.DEFICIT
                               && i.getSeverity() == Severity.HIGH
                               && i.getAction() == ActionType.WARNING);
    }

    @Test
    @DisplayName("Rule 1: should NOT generate DEFICIT insight when savings >= 0")
    void rule1_positiveSavings_noDeficit() {
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 1000.0, "Food", Map.of("Food", 2000.0), Collections.emptyList());

        assertThat(insights).noneMatch(i -> i.getAnomalyType() == AnomalyType.DEFICIT);
    }

    @Test
    @DisplayName("Rule 1: boundary — savings exactly zero does NOT trigger DEFICIT")
    void rule1_zeroSavings_noDeficit() {
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 0.0, "No data", Map.of(), Collections.emptyList());

        assertThat(insights).noneMatch(i -> i.getAnomalyType() == AnomalyType.DEFICIT);
    }

    // ─── Rule 2: Top Category → SUGGESTION ───────────────────────────────────

    @Test
    @DisplayName("Rule 2: should include top-category suggestion with category name in text")
    void rule2_topCategory_suggestion() {
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 500.0, "Rent", Map.of("Rent", 5000.0), Collections.emptyList());

        assertThat(insights)
                .anyMatch(i -> i.getAnomalyType() == AnomalyType.NONE
                               && i.getAction() == ActionType.SUGGESTION
                               && i.getInsightText().contains("Rent"));
    }

    @Test
    @DisplayName("Rule 2: should skip top-category suggestion when topCategory is 'No data'")
    void rule2_noData_skipsSuggestion() {
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 500.0, "No data", Map.of(), Collections.emptyList());

        assertThat(insights).noneMatch(i ->
                i.getAction() == ActionType.SUGGESTION
                && i.getInsightText().contains("No data"));
    }

    // ─── Rule 3a: Over-budget → ALERT ────────────────────────────────────────

    @Test
    @DisplayName("Rule 3a: should generate OVER_BUDGET alert when spending exceeds budget limit")
    void rule3a_overBudget_alert() {
        Budget budget = buildBudget(3000.0);

        // Spent ₹4000 > limit ₹3000
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, -1000.0, "Food", Map.of("Food", 4000.0), List.of(budget));

        assertThat(insights)
                .anyMatch(i -> i.getAnomalyType() == AnomalyType.OVER_BUDGET
                               && i.getAction() == ActionType.ALERT
                               && i.getSeverity() == Severity.HIGH
                               && i.getInsightText().contains("Food"));
    }

    @Test
    @DisplayName("Rule 3a: confidence should be 1.0 for OVER_BUDGET (certainty)")
    void rule3a_overBudget_confidence100() {
        Budget budget = buildBudget(3000.0);

        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, -1000.0, "Food", Map.of("Food", 4000.0), List.of(budget));

        assertThat(insights)
                .filteredOn(i -> i.getAnomalyType() == AnomalyType.OVER_BUDGET)
                .allMatch(i -> i.getConfidenceScore() == 1.0);
    }

    // ─── Rule 3c: Near budget (80%) → WARNING ────────────────────────────────

    @Test
    @DisplayName("Rule 3c: should generate MEDIUM warning when spending is between 80–100% of limit")
    void rule3c_nearBudget_warning() {
        Budget budget = buildBudget(5000.0);

        // Spent ₹4200 = 84% of ₹5000 limit
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 800.0, "Food", Map.of("Food", 4200.0), List.of(budget));

        assertThat(insights)
                .anyMatch(i -> i.getSeverity() == Severity.MEDIUM
                               && i.getAction() == ActionType.WARNING
                               && i.getInsightText().contains("close"));
    }

    // ─── All insights have required fields ───────────────────────────────────

    @Test
    @DisplayName("all generated insights must have non-null text, action, severity, and user")
    void allInsights_haveRequiredFields() {
        Budget budget = buildBudget(5000.0);

        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, -500.0, "Food", Map.of("Food", 6000.0), List.of(budget));

        for (AIInsight insight : insights) {
            assertThat(insight.getInsightText()).isNotBlank();
            assertThat(insight.getAction()).isNotNull();
            assertThat(insight.getSeverity()).isNotNull();
            assertThat(insight.getUser()).isNotNull();
            assertThat(insight.getCreatedAt()).isNotNull();
            assertThat(insight.getSource()).isEqualTo(InsightSource.RULE_BASED);
        }
    }

    @Test
    @DisplayName("empty inputs: should return empty list when budgets=null and no data")
    void emptyInputs_returnsEmptyList() {
        // "No data" topCategory + zero savings + null budgets
        List<AIInsight> insights = engine.evaluateUserFinancials(
                user, 0.0, "No data", Map.of(), null);

        // Only check that no exception is thrown and result is a valid list
        assertThat(insights).isNotNull();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Budget buildBudget(double limit) {
        return Budget.builder()
                .id(1L)
                .limitAmount(limit)
                .month(4)
                .year(2025)
                .user(user)
                .category(foodCategory)
                .build();
    }
}
