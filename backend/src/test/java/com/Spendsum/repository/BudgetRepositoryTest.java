package com.Spendsum.repository;

import com.Spendsum.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository slice tests for BudgetRepository.
 * Tests the custom query: findByUserIdAndCategoryIdAndMonthAndYear
 * and verifies entity persistence, relationships, and constraint behavior.
 */
@DataJpaTest
@ActiveProfiles("test")
class BudgetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BudgetRepository budgetRepository;

    private User user;
    private Category foodCategory;
    private Category rentCategory;

    @BeforeEach
    void setUp() {
        user = entityManager.persistAndFlush(
                User.builder().username("alice").email("alice@t.com").password("pass").build());
        foodCategory = entityManager.persistAndFlush(
                Category.builder().name("Food").type("EXPENSE").user(user).build());
        rentCategory = entityManager.persistAndFlush(
                Category.builder().name("Rent").type("EXPENSE").user(user).build());
    }

    // ─── findByUserIdAndCategoryIdAndMonthAndYear ─────────────────────────────

    @Test
    @DisplayName("should find existing budget by user + category + month + year")
    void findByCompositeKey_found() {
        Budget budget = entityManager.persistAndFlush(
                buildBudget(5000.0, foodCategory, 4, 2025));
        entityManager.clear();

        Optional<Budget> result = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(
                        user.getId(), foodCategory.getId(), 4, 2025);

        assertThat(result).isPresent();
        assertThat(result.get().getLimitAmount()).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("should return empty when no budget exists for given month")
    void findByCompositeKey_notFound_wrongMonth() {
        entityManager.persistAndFlush(buildBudget(5000.0, foodCategory, 4, 2025));
        entityManager.clear();

        // Query with month=3 — different from stored month=4
        Optional<Budget> result = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(
                        user.getId(), foodCategory.getId(), 3, 2025);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when category ID does not match")
    void findByCompositeKey_notFound_wrongCategory() {
        entityManager.persistAndFlush(buildBudget(5000.0, foodCategory, 4, 2025));
        entityManager.clear();

        Optional<Budget> result = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(
                        user.getId(), rentCategory.getId(), 4, 2025);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when user ID does not match")
    void findByCompositeKey_notFound_wrongUser() {
        entityManager.persistAndFlush(buildBudget(5000.0, foodCategory, 4, 2025));
        entityManager.clear();

        Optional<Budget> result = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(
                        999L, foodCategory.getId(), 4, 2025);

        assertThat(result).isEmpty();
    }

    // ─── findByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: should return all budgets for a user")
    void findByUserId_returnsAllBudgets() {
        entityManager.persistAndFlush(buildBudget(3000.0, foodCategory, 4, 2025));
        entityManager.persistAndFlush(buildBudget(8000.0, rentCategory, 4, 2025));
        entityManager.clear();

        List<Budget> results = budgetRepository.findByUserId(user.getId());

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("findByUserId: returns empty list for user with no budgets")
    void findByUserId_empty() {
        assertThat(budgetRepository.findByUserId(999L)).isEmpty();
    }

    // ─── persist & reload ─────────────────────────────────────────────────────

    @Test
    @DisplayName("save: budget is persisted with correct fields and relationships")
    void save_persistsAllFields() {
        Budget budget = Budget.builder()
                .limitAmount(7500.0)
                .month(6)
                .year(2025)
                .user(user)
                .category(foodCategory)
                .build();

        Budget saved = budgetRepository.save(budget);
        entityManager.clear();

        Budget loaded = budgetRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getLimitAmount()).isEqualTo(7500.0);
        assertThat(loaded.getMonth()).isEqualTo(6);
        assertThat(loaded.getYear()).isEqualTo(2025);
        assertThat(loaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(loaded.getCategory().getName()).isEqualTo("Food");
    }

    // ─── Edge cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("edge: budget with very high limit amount is saved without overflow")
    void save_largeLimitAmount_noTruncation() {
        Budget budget = buildBudget(9_999_999.99, foodCategory, 4, 2025);
        Budget saved = budgetRepository.save(budget);
        entityManager.clear();

        Budget loaded = budgetRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getLimitAmount()).isEqualTo(9_999_999.99);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Budget buildBudget(double limit, Category cat, int month, int year) {
        return Budget.builder()
                .limitAmount(limit)
                .month(month)
                .year(year)
                .user(user)
                .category(cat)
                .build();
    }
}
