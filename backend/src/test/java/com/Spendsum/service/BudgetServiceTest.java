package com.Spendsum.service;

import com.Spendsum.model.*;
import com.Spendsum.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BudgetService.
 * All dependencies (BudgetRepository, TransactionService) are mocked —
 * no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private BudgetService budgetService;

    // ─── Shared test fixtures ────────────────────────────────────────────────

    private User user;
    private Category category;
    private Budget budget;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("test@example.com").password("pass").build();
        category = Category.builder().id(10L).name("Food").type("EXPENSE").user(user).build();
        // Default budget: ₹5000 limit, April 2025
        budget = Budget.builder()
                .id(100L)
                .limitAmount(5000.0)
                .month(4)
                .year(2025)
                .user(user)
                .category(category)
                .build();
    }

    // ─── createBudget ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBudget()")
    class CreateBudget {

        @Test
        @DisplayName("should save and return the budget")
        void createBudget_success() {
            when(budgetRepository.save(budget)).thenReturn(budget);

            Budget result = budgetService.createBudget(budget);

            assertThat(result).isNotNull();
            assertThat(result.getLimitAmount()).isEqualTo(5000.0);
            verify(budgetRepository).save(budget);
        }
    }

    // ─── getBudgetById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBudgetById()")
    class GetBudgetById {

        @Test
        @DisplayName("should return budget when found")
        void getBudgetById_found() {
            when(budgetRepository.findById(100L)).thenReturn(Optional.of(budget));

            Budget result = budgetService.getBudgetById(100L);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw RuntimeException when not found")
        void getBudgetById_notFound() {
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.getBudgetById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Budget not found");
        }
    }

    // ─── isBudgetExceeded ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isBudgetExceeded()")
    class IsBudgetExceeded {

        @Test
        @DisplayName("should return true when spending exceeds limit")
        void exceeded_whenSpentMoreThanLimit() {
            // Spent ₹6000 > limit ₹5000
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 6000.0));

            boolean exceeded = budgetService.isBudgetExceeded(1L, 10L, 4, 2025);

            assertThat(exceeded).isTrue();
        }

        @Test
        @DisplayName("should return false when spending is below limit")
        void notExceeded_whenSpentLessThanLimit() {
            // Spent ₹3000 < limit ₹5000
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 3000.0));

            boolean exceeded = budgetService.isBudgetExceeded(1L, 10L, 4, 2025);

            assertThat(exceeded).isFalse();
        }

        @Test
        @DisplayName("boundary: should return false when spending equals the limit exactly")
        void notExceeded_whenSpentEqualsLimit() {
            // Exactly at limit — NOT exceeded (service uses strict >)
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 5000.0));

            boolean exceeded = budgetService.isBudgetExceeded(1L, 10L, 4, 2025);

            assertThat(exceeded).isFalse();
        }

        @Test
        @DisplayName("should return false when category has no transactions yet")
        void notExceeded_whenCategoryMissingFromSpending() {
            stubBudgetLookup();
            // No "Food" entry — getOrDefault returns 0.0
            when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of());

            boolean exceeded = budgetService.isBudgetExceeded(1L, 10L, 4, 2025);

            assertThat(exceeded).isFalse();
        }

        @Test
        @DisplayName("should throw RuntimeException when budget does not exist")
        void exceeded_throwsWhenBudgetMissing() {
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                    anyLong(), anyLong(), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.isBudgetExceeded(1L, 99L, 4, 2025))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Budget not found");
        }
    }

    // ─── getRemainingBudget ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getRemainingBudget()")
    class GetRemainingBudget {

        @Test
        @DisplayName("should return positive remaining when under budget")
        void remaining_positive() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 2000.0));

            double remaining = budgetService.getRemainingBudget(1L, 10L, 4, 2025);

            assertThat(remaining).isEqualTo(3000.0); // 5000 - 2000
        }

        @Test
        @DisplayName("should return negative value when budget overrun")
        void remaining_negative_whenOverBudget() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 7000.0));

            double remaining = budgetService.getRemainingBudget(1L, 10L, 4, 2025);

            assertThat(remaining).isNegative(); // 5000 - 7000 = -2000
        }
    }

    // ─── getBudgetStatus ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBudgetStatus() — alert level logic")
    class GetBudgetStatus {

        @Test
        @DisplayName("should return EXCEEDED message at 100%+ usage")
        void status_exceeded_at100Percent() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 5000.0)); // exactly 100%

            String status = budgetService.getBudgetStatus(1L, 10L, 4, 2025);

            assertThat(status).contains("exceeded");
        }

        @Test
        @DisplayName("should return WARNING message between 80–99% usage")
        void status_warning_at80To99Percent() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 4200.0)); // 84%

            String status = budgetService.getBudgetStatus(1L, 10L, 4, 2025);

            assertThat(status).containsIgnoringCase("Warning");
        }

        @Test
        @DisplayName("should return OK message below 80% usage")
        void status_ok_below80Percent() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 1000.0)); // 20%

            String status = budgetService.getBudgetStatus(1L, 10L, 4, 2025);

            assertThat(status).containsIgnoringCase("within budget");
        }

        @Test
        @DisplayName("boundary: exactly 80% usage should trigger WARNING, not OK")
        void status_warning_atExactly80Percent() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L))
                    .thenReturn(Map.of("Food", 4000.0)); // exactly 80%

            String status = budgetService.getBudgetStatus(1L, 10L, 4, 2025);

            assertThat(status).containsIgnoringCase("Warning");
        }

        @Test
        @DisplayName("should use ZERO spending when category not in map (no NPE)")
        void status_noNpe_whenCategoryMissing() {
            stubBudgetLookup();
            when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of());

            assertThatCode(() -> budgetService.getBudgetStatus(1L, 10L, 4, 2025))
                    .doesNotThrowAnyException();
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private void stubBudgetLookup() {
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 10L, 4, 2025))
                .thenReturn(Optional.of(budget));
    }
}
