package com.Spendsum.service;

import com.Spendsum.model.*;
import com.Spendsum.repository.AIInsightRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIInsightService.
 *
 * AI output is inherently dynamic — every external service (GeminiService,
 * TransactionService, BudgetService) is mocked. We validate:
 *   1. The correct insights are generated based on financial data
 *   2. Insights are saved via the repository
 *   3. The feedback loop (setHelpfulStatus) works correctly
 *   4. Edge cases: deficit, no spending data, missing insight
 */
@ExtendWith(MockitoExtension.class)
class AIInsightServiceTest {

    @Mock
    private AIInsightRepository aiInsightRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private AIInsightService aiInsightService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("t@t.com").password("pass").build();
    }

    // ─── generateInsights — positive savings path ────────────────────────────

    @Test
    @DisplayName("generateInsights: should create top-category and positive-savings insights")
    void generateInsights_positiveSavings_createsCorrectInsights() {
        // Mock: user is saving money
        when(transactionService.getTopSpendingCategory(1L)).thenReturn("Food");
        when(transactionService.getSavings(1L)).thenReturn(2000.0);
        when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of("Food", 3000.0));
        // saveAll returns whatever is passed in
        when(aiInsightRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AIInsight> insights = aiInsightService.generateInsights(user);

        // Must produce at least 2 insights: top-category + positive savings
        assertThat(insights).hasSizeGreaterThanOrEqualTo(2);

        // Top-category insight
        assertThat(insights)
                .anyMatch(i -> i.getInsightText().contains("Food"))
                .anyMatch(i -> i.getAction() == ActionType.SUGGESTION);

        // Positive savings insight — NOT the deficit WARNING
        assertThat(insights)
                .noneMatch(i -> i.getAction() == ActionType.WARNING
                                && i.getInsightText().contains("deficit"));

        verify(aiInsightRepository).saveAll(anyList());
    }

    // ─── generateInsights — deficit path ────────────────────────────────────

    @Test
    @DisplayName("generateInsights: should emit HIGH-severity WARNING when savings are negative")
    void generateInsights_negativeSavings_emitsDeficitWarning() {
        when(transactionService.getTopSpendingCategory(1L)).thenReturn("Rent");
        when(transactionService.getSavings(1L)).thenReturn(-500.0); // deficit!
        when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of("Rent", 8000.0));
        when(aiInsightRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AIInsight> insights = aiInsightService.generateInsights(user);

        // Verify the deficit warning was generated
        assertThat(insights)
                .anyMatch(i -> i.getAction() == ActionType.WARNING
                               && i.getSeverity() == Severity.HIGH
                               && i.getInsightText().contains("deficit"));
    }

    // ─── generateInsights — zero savings boundary ────────────────────────────

    @Test
    @DisplayName("generateInsights: savings exactly zero should produce positive (not deficit) insight")
    void generateInsights_zeroSavings_treatedAsPositive() {
        // savings == 0.0 is NOT < 0, so the positive path applies
        when(transactionService.getTopSpendingCategory(1L)).thenReturn("No data");
        when(transactionService.getSavings(1L)).thenReturn(0.0);
        when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of());
        when(aiInsightRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AIInsight> insights = aiInsightService.generateInsights(user);

        assertThat(insights).noneMatch(i ->
                i.getAction() == ActionType.WARNING && i.getInsightText().contains("deficit"));
    }

    // ─── generateInsights — all insights have required fields ────────────────

    @Test
    @DisplayName("generateInsights: all returned insights have non-null user and createdAt")
    void generateInsights_allInsightsHaveRequiredFields() {
        when(transactionService.getTopSpendingCategory(1L)).thenReturn("Food");
        when(transactionService.getSavings(1L)).thenReturn(1000.0);
        when(transactionService.getCategoryWiseSpending(1L)).thenReturn(Map.of("Food", 2000.0));
        when(aiInsightRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AIInsight> insights = aiInsightService.generateInsights(user);

        for (AIInsight insight : insights) {
            assertThat(insight.getUser()).isNotNull();
            assertThat(insight.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(insight.getInsightText()).isNotBlank();
        }
    }

    // ─── getInsightsByUser ───────────────────────────────────────────────────

    @Test
    @DisplayName("getInsightsByUser: should delegate to repository and return results")
    void getInsightsByUser_delegatesCorrectly() {
        AIInsight insight = AIInsight.builder()
                .id(1L)
                .insightText("Test insight")
                .user(user)
                .build();
        when(aiInsightRepository.findByUserId(1L)).thenReturn(List.of(insight));

        List<AIInsight> results = aiInsightService.getInsightsByUser(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getInsightText()).isEqualTo("Test insight");
    }

    // ─── setHelpfulStatus — feedback loop ────────────────────────────────────

    @Test
    @DisplayName("setHelpfulStatus: should update and persist isHelpful=true")
    void setHelpful_true_savedCorrectly() {
        AIInsight insight = buildInsight(1L);
        when(aiInsightRepository.findById(1L)).thenReturn(Optional.of(insight));
        when(aiInsightRepository.save(insight)).thenReturn(insight);

        AIInsight result = aiInsightService.setHelpfulStatus(1L, true);

        assertThat(result.getIsHelpful()).isTrue();
        verify(aiInsightRepository).save(insight);
    }

    @Test
    @DisplayName("setHelpfulStatus: should update and persist isHelpful=false")
    void setHelpful_false_savedCorrectly() {
        AIInsight insight = buildInsight(2L);
        when(aiInsightRepository.findById(2L)).thenReturn(Optional.of(insight));
        when(aiInsightRepository.save(insight)).thenReturn(insight);

        AIInsight result = aiInsightService.setHelpfulStatus(2L, false);

        assertThat(result.getIsHelpful()).isFalse();
    }

    @Test
    @DisplayName("setHelpfulStatus: should throw RuntimeException when insight not found")
    void setHelpful_notFound_throws() {
        when(aiInsightRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiInsightService.setHelpfulStatus(999L, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insight not found");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AIInsight buildInsight(Long id) {
        return AIInsight.builder()
                .id(id)
                .insightText("Sample insight")
                .action(ActionType.SUGGESTION)
                .severity(Severity.LOW)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
