package com.Spendsum.integration;

import com.Spendsum.config.TestcontainersConfig;
import com.Spendsum.model.*;
import com.Spendsum.repository.*;
import com.Spendsum.service.BudgetService;
import com.Spendsum.service.TransactionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Full-stack integration test for the Budget + Transaction interaction.
 *
 * Tests the critical business scenario:
 *   1. Create a budget with a spending limit
 *   2. Add transactions that consume the budget
 *   3. Verify isBudgetExceeded and getBudgetStatus reflect the real DB state
 *
 * This tests the complete Service → Repository → MySQL chain without any mocking
 * of the core business logic. Only AgentService (external AI) is mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BudgetIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Mock external AI agent to prevent GeminiService HTTP calls
    @MockBean
    private com.Spendsum.agent.AgentService agentService;

    private User user;
    private Category foodCategory;
    private Budget foodBudget;

    @BeforeEach
    void setUp() {
        // Persist real entities into the Testcontainer MySQL database
        user = userRepository.save(
                User.builder().username("budgetUser").email("budget@test.com").password("pass").build());
        foodCategory = categoryRepository.save(
                Category.builder().name("Food").type("EXPENSE").user(user).build());

        // Budget: ₹5000 limit for Food in April 2025
        foodBudget = budgetService.createBudget(
                Budget.builder()
                        .limitAmount(5000.0)
                        .month(4)
                        .year(2025)
                        .user(user)
                        .category(foodCategory)
                        .build());
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAllByUserId(user.getId());
        budgetRepository.deleteAll(budgetRepository.findByUserId(user.getId()));
        categoryRepository.deleteById(foodCategory.getId());
        userRepository.deleteById(user.getId());
    }

    // ─── Scenario 1: Under budget ─────────────────────────────────────────────

    @Test
    @DisplayName("scenario: budget not exceeded when spending < limit")
    void scenario_underBudget() {
        // Add ₹2000 of Food expense — under the ₹5000 limit
        transactionService.createTransaction(buildFoodExpense(2000.0));

        boolean exceeded = budgetService.isBudgetExceeded(
                user.getId(), foodCategory.getId(), 4, 2025);
        String status = budgetService.getBudgetStatus(
                user.getId(), foodCategory.getId(), 4, 2025);
        double remaining = budgetService.getRemainingBudget(
                user.getId(), foodCategory.getId(), 4, 2025);

        assertThat(exceeded).isFalse();
        assertThat(status).containsIgnoringCase("within budget");
        assertThat(remaining).isEqualTo(3000.0); // 5000 - 2000
    }

    // ─── Scenario 2: 80% warning threshold ───────────────────────────────────

    @Test
    @DisplayName("scenario: WARNING status triggered when spending >= 80% of limit")
    void scenario_atWarningThreshold() {
        // ₹4200 = 84% of ₹5000
        transactionService.createTransaction(buildFoodExpense(4200.0));

        String status = budgetService.getBudgetStatus(
                user.getId(), foodCategory.getId(), 4, 2025);

        assertThat(status).containsIgnoringCase("warning");
        assertThat(status).contains("84.0");
    }

    // ─── Scenario 3: Budget exceeded ─────────────────────────────────────────

    @Test
    @DisplayName("scenario: budget exceeded when cumulative transactions exceed limit")
    void scenario_budgetExceeded_afterMultipleTransactions() {
        // Add 3 separate transactions totalling ₹6000 > ₹5000 limit
        transactionService.createTransaction(buildFoodExpense(2000.0));
        transactionService.createTransaction(buildFoodExpense(2000.0));
        transactionService.createTransaction(buildFoodExpense(2000.0));

        boolean exceeded = budgetService.isBudgetExceeded(
                user.getId(), foodCategory.getId(), 4, 2025);
        String status = budgetService.getBudgetStatus(
                user.getId(), foodCategory.getId(), 4, 2025);
        double remaining = budgetService.getRemainingBudget(
                user.getId(), foodCategory.getId(), 4, 2025);

        assertThat(exceeded).isTrue();
        assertThat(status).containsIgnoringCase("exceeded");
        assertThat(remaining).isNegative(); // -1000.0
    }

    // ─── Scenario 4: Income transactions don't affect budget ─────────────────

    @Test
    @DisplayName("scenario: INCOME transactions should not affect budget spending calculation")
    void scenario_incomeDoesNotAffectBudget() {
        // Add large income — this should NOT count toward budget spending
        transactionService.createTransaction(
                Transaction.builder()
                        .amount(50000.0)
                        .type("INCOME")
                        .description("Salary")
                        .date(LocalDate.of(2025, 4, 1))
                        .user(user)
                        .category(foodCategory)
                        .build());

        // Budget remains unaffected by income
        boolean exceeded = budgetService.isBudgetExceeded(
                user.getId(), foodCategory.getId(), 4, 2025);

        assertThat(exceeded).isFalse();
    }

    // ─── Scenario 5: Exactly at limit ────────────────────────────────────────

    @Test
    @DisplayName("scenario: budget status is EXCEEDED when spending is exactly at limit (100%)")
    void scenario_exactlyAtLimit_exceeded() {
        transactionService.createTransaction(buildFoodExpense(5000.0)); // exactly at limit

        boolean exceeded = budgetService.isBudgetExceeded(
                user.getId(), foodCategory.getId(), 4, 2025);
        String status = budgetService.getBudgetStatus(
                user.getId(), foodCategory.getId(), 4, 2025);

        // Service logic: spent > limit is FALSE at exactly equal — but getBudgetStatus
        // checks >= 100%, so status shows "exceeded" while isBudgetExceeded is false
        assertThat(status).containsIgnoringCase("exceeded");
        assertThat(exceeded).isFalse(); // spent == limit, not spent > limit
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Transaction buildFoodExpense(double amount) {
        return Transaction.builder()
                .amount(amount)
                .type("EXPENSE")
                .description("Food expense")
                .date(LocalDate.of(2025, 4, 15))
                .user(user)
                .category(foodCategory)
                .build();
    }
}
