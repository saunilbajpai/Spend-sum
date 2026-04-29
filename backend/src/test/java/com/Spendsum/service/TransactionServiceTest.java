package com.Spendsum.service;

import com.Spendsum.agent.AgentService;
import com.Spendsum.model.*;
import com.Spendsum.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 * AgentService is mocked so async AI processing does not trigger in unit tests.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AgentService agentService;

    @InjectMocks
    private TransactionService transactionService;

    // ─── Shared fixtures ─────────────────────────────────────────────────────

    private User user;
    private Category foodCategory;
    private Category rentCategory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("t@t.com").password("pass").build();
        foodCategory = Category.builder().id(10L).name("Food").type("EXPENSE").user(user).build();
        rentCategory = Category.builder().id(11L).name("Rent").type("EXPENSE").user(user).build();
    }

    // ─── createTransaction ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransaction()")
    class CreateTransaction {

        @Test
        @DisplayName("should save transaction and trigger agent processing")
        void create_savesAndTriggersAgent() {
            Transaction tx = buildExpense(500.0, foodCategory, LocalDate.of(2025, 4, 10));
            when(transactionRepository.save(tx)).thenReturn(tx);

            Transaction result = transactionService.createTransaction(tx);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(500.0);
            // Agent must be triggered after each new transaction
            verify(agentService).processUser(1L);
        }

        @Test
        @DisplayName("edge: should not call agentService when transaction has no user")
        void create_noUser_skipsAgent() {
            Transaction tx = Transaction.builder()
                    .amount(100.0)
                    .type("EXPENSE")
                    .date(LocalDate.now())
                    .category(foodCategory)
                    .build(); // user = null
            when(transactionRepository.save(tx)).thenReturn(tx);

            transactionService.createTransaction(tx);

            verify(agentService, never()).processUser(anyLong());
        }

        @Test
        @DisplayName("edge: very large transaction amount is saved without truncation")
        void create_largeAmount_savedCorrectly() {
            Transaction tx = buildExpense(999_999_999.99, foodCategory, LocalDate.now());
            when(transactionRepository.save(tx)).thenReturn(tx);

            Transaction result = transactionService.createTransaction(tx);

            assertThat(result.getAmount()).isEqualTo(999_999_999.99);
        }
    }

    // ─── getTransactionById ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getTransactionById()")
    class GetTransactionById {

        @Test
        @DisplayName("should return transaction when found")
        void found() {
            Transaction tx = buildExpense(200.0, foodCategory, LocalDate.now());
            tx.setId(1L);
            when(transactionRepository.findById(1L)).thenReturn(java.util.Optional.of(tx));

            assertThat(transactionService.getTransactionById(1L)).isEqualTo(tx);
        }

        @Test
        @DisplayName("should throw RuntimeException for missing ID")
        void notFound_throws() {
            when(transactionRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }

    // ─── getMonthlySummary ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonthlySummary()")
    class GetMonthlySummary {

        @Test
        @DisplayName("should correctly sum income, expense, and savings for the given month")
        void summary_correctCalculation() {
            List<Transaction> transactions = List.of(
                    buildIncome(10000.0, LocalDate.of(2025, 4, 5)),
                    buildExpense(3000.0, foodCategory, LocalDate.of(2025, 4, 15)),
                    buildExpense(1500.0, rentCategory, LocalDate.of(2025, 4, 20)),
                    // Different month — should be excluded
                    buildExpense(500.0, foodCategory, LocalDate.of(2025, 3, 10))
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            Map<String, Double> summary = transactionService.getMonthlySummary(1L, 4, 2025);

            assertThat(summary.get("income")).isEqualTo(10000.0);
            assertThat(summary.get("expense")).isEqualTo(4500.0);
            assertThat(summary.get("savings")).isEqualTo(5500.0);
        }

        @Test
        @DisplayName("should return zeros when user has no transactions")
        void summary_noTransactions_returnsZeros() {
            when(transactionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

            Map<String, Double> summary = transactionService.getMonthlySummary(1L, 4, 2025);

            assertThat(summary.get("income")).isZero();
            assertThat(summary.get("expense")).isZero();
            assertThat(summary.get("savings")).isZero();
        }

        @Test
        @DisplayName("edge: savings is negative when expenses exceed income")
        void summary_negativeSavings() {
            List<Transaction> transactions = List.of(
                    buildIncome(1000.0, LocalDate.of(2025, 4, 1)),
                    buildExpense(3000.0, foodCategory, LocalDate.of(2025, 4, 2))
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            Map<String, Double> summary = transactionService.getMonthlySummary(1L, 4, 2025);

            assertThat(summary.get("savings")).isNegative(); // 1000 - 3000 = -2000
        }
    }

    // ─── getCategoryWiseSpending ─────────────────────────────────────────────

    @Nested
    @DisplayName("getCategoryWiseSpending()")
    class GetCategoryWiseSpending {

        @Test
        @DisplayName("should sum expenses grouped by category")
        void categorySpending_groupedCorrectly() {
            List<Transaction> transactions = List.of(
                    buildExpense(1000.0, foodCategory, LocalDate.now()),
                    buildExpense(500.0, foodCategory, LocalDate.now()),
                    buildExpense(2000.0, rentCategory, LocalDate.now()),
                    buildIncome(5000.0, LocalDate.now()) // income — must be excluded
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            Map<String, Double> result = transactionService.getCategoryWiseSpending(1L);

            assertThat(result).containsEntry("Food", 1500.0);
            assertThat(result).containsEntry("Rent", 2000.0);
            assertThat(result).doesNotContainKey(null); // income entry excluded
        }

        @Test
        @DisplayName("edge: no EXPENSE transactions returns empty map")
        void categorySpending_onlyIncome_returnsEmpty() {
            when(transactionRepository.findByUserId(1L))
                    .thenReturn(List.of(buildIncome(5000.0, LocalDate.now())));

            Map<String, Double> result = transactionService.getCategoryWiseSpending(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("edge: negative transaction amounts are included in sum")
        void categorySpending_negativeAmounts_included() {
            // Negative amounts may represent reversals — they should be summed, not filtered
            List<Transaction> transactions = List.of(
                    buildExpense(1000.0, foodCategory, LocalDate.now()),
                    buildExpense(-200.0, foodCategory, LocalDate.now()) // reversal
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            Map<String, Double> result = transactionService.getCategoryWiseSpending(1L);

            assertThat(result.get("Food")).isEqualTo(800.0);
        }
    }

    // ─── getSavings / getIncomeVsExpense ─────────────────────────────────────

    @Nested
    @DisplayName("getSavings()")
    class GetSavings {

        @Test
        @DisplayName("should return income minus expense")
        void savings_correctValue() {
            List<Transaction> transactions = List.of(
                    buildIncome(8000.0, LocalDate.now()),
                    buildExpense(3000.0, foodCategory, LocalDate.now())
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            Double savings = transactionService.getSavings(1L);

            assertThat(savings).isEqualTo(5000.0);
        }

        @Test
        @DisplayName("should return zero when user has no transactions")
        void savings_zero_whenEmpty() {
            when(transactionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

            assertThat(transactionService.getSavings(1L)).isZero();
        }
    }

    // ─── getTopSpendingCategory ──────────────────────────────────────────────

    @Nested
    @DisplayName("getTopSpendingCategory()")
    class GetTopSpendingCategory {

        @Test
        @DisplayName("should return category with highest expense")
        void topCategory_correctlyIdentified() {
            List<Transaction> transactions = List.of(
                    buildExpense(1000.0, foodCategory, LocalDate.now()),
                    buildExpense(5000.0, rentCategory, LocalDate.now())
            );
            when(transactionRepository.findByUserId(1L)).thenReturn(transactions);

            String top = transactionService.getTopSpendingCategory(1L);

            assertThat(top).isEqualTo("Rent");
        }

        @Test
        @DisplayName("should return 'No data' when no expense transactions exist")
        void topCategory_noData_returnsNoData() {
            when(transactionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

            assertThat(transactionService.getTopSpendingCategory(1L)).isEqualTo("No data");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Transaction buildExpense(Double amount, Category category, LocalDate date) {
        return Transaction.builder()
                .amount(amount)
                .type("EXPENSE")
                .date(date)
                .category(category)
                .user(user)
                .build();
    }

    private Transaction buildIncome(Double amount, LocalDate date) {
        return Transaction.builder()
                .amount(amount)
                .type("INCOME")
                .date(date)
                .category(foodCategory) // income category is irrelevant for expense tests
                .user(user)
                .build();
    }
}
