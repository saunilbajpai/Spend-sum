package com.Spendsum.repository;

import com.Spendsum.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository slice tests for AIInsightRepository.
 * Tests findByUserId, deleteAllByUserId, and insight entity persistence.
 */
@DataJpaTest
@ActiveProfiles("test")
class AIInsightRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AIInsightRepository aiInsightRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = entityManager.persistAndFlush(
                User.builder().username("alice").email("alice@t.com").password("pass").build());
        user2 = entityManager.persistAndFlush(
                User.builder().username("bob").email("bob@t.com").password("pass").build());
    }

    // ─── findByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: should return insights only for the given user")
    void findByUserId_correctIsolation() {
        entityManager.persistAndFlush(buildInsight("Reduce spending", user1));
        entityManager.persistAndFlush(buildInsight("You are in deficit", user1));
        entityManager.persistAndFlush(buildInsight("Bob's tip", user2));
        entityManager.clear();

        List<AIInsight> results = aiInsightRepository.findByUserId(user1.getId());

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(i -> i.getUser().getId().equals(user1.getId()));
    }

    @Test
    @DisplayName("findByUserId: should return empty list for user with no insights")
    void findByUserId_empty() {
        assertThat(aiInsightRepository.findByUserId(user2.getId())).isEmpty();
    }

    // ─── deleteAllByUserId ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAllByUserId: removes all insights for user, leaves others intact")
    void deleteAllByUserId_correctDeletion() {
        entityManager.persistAndFlush(buildInsight("Tip A", user1));
        entityManager.persistAndFlush(buildInsight("Tip B", user1));
        entityManager.persistAndFlush(buildInsight("Bob's tip", user2));
        entityManager.clear();

        aiInsightRepository.deleteAllByUserId(user1.getId());
        entityManager.clear();

        assertThat(aiInsightRepository.findByUserId(user1.getId())).isEmpty();
        assertThat(aiInsightRepository.findByUserId(user2.getId())).hasSize(1);
    }

    // ─── persist & field validation ───────────────────────────────────────────

    @Test
    @DisplayName("save: insight is persisted with all enum fields intact")
    void save_allFieldsPersisted() {
        AIInsight insight = AIInsight.builder()
                .insightText("You exceeded Food budget!")
                .action(ActionType.ALERT)
                .severity(Severity.HIGH)
                .source(InsightSource.RULE_BASED)
                .anomalyType(AnomalyType.OVER_BUDGET)
                .confidenceScore(1.0)
                .processingTimeMs(42L)
                .executed(false)
                .createdAt(LocalDateTime.now())
                .user(user1)
                .build();

        AIInsight saved = aiInsightRepository.save(insight);
        entityManager.clear();

        AIInsight loaded = aiInsightRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getAction()).isEqualTo(ActionType.ALERT);
        assertThat(loaded.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(loaded.getSource()).isEqualTo(InsightSource.RULE_BASED);
        assertThat(loaded.getAnomalyType()).isEqualTo(AnomalyType.OVER_BUDGET);
        assertThat(loaded.getConfidenceScore()).isEqualTo(1.0);
        assertThat(loaded.getInsightText()).isEqualTo("You exceeded Food budget!");
        assertThat(loaded.isExecuted()).isFalse();
    }

    @Test
    @DisplayName("save: isHelpful can be set to true and reloaded correctly")
    void save_isHelpful_true() {
        AIInsight insight = buildInsight("Great tip", user1);
        insight.setIsHelpful(true);

        AIInsight saved = aiInsightRepository.save(insight);
        entityManager.clear();

        AIInsight loaded = aiInsightRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIsHelpful()).isTrue();
    }

    @Test
    @DisplayName("save: isHelpful defaults to null (unreviewed)")
    void save_isHelpful_defaultNull() {
        AIInsight insight = buildInsight("Some insight", user1);
        // isHelpful not set

        AIInsight saved = aiInsightRepository.save(insight);
        entityManager.clear();

        AIInsight loaded = aiInsightRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIsHelpful()).isNull();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AIInsight buildInsight(String text, User owner) {
        return AIInsight.builder()
                .insightText(text)
                .action(ActionType.SUGGESTION)
                .severity(Severity.LOW)
                .source(InsightSource.RULE_BASED)
                .anomalyType(AnomalyType.NONE)
                .executed(false)
                .createdAt(LocalDateTime.now())
                .user(owner)
                .build();
    }
}
