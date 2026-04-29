package com.Spendsum.repository;

import com.Spendsum.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository slice tests for TransactionRepository.
 *
 * @DataJpaTest spins up an embedded H2 database and only loads JPA/Hibernate,
 * making these tests very fast. No service or controller beans are loaded.
 *
 * NOTE: If you want MySQL-specific query validation, swap @DataJpaTest for
 * @SpringBootTest with @ActiveProfiles("test") using Testcontainers.
 */
@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private User user1;
    private User user2;
    private Category category;

    @BeforeEach
    void setUp() {
        // Use UUID-suffixed emails to avoid unique constraint violations
        // across test methods when @Commit leaves data permanently in H2
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user1 = entityManager.persistAndFlush(
                User.builder().username("alice").email("alice-" + suffix + "@test.com").password("pass").build());
        user2 = entityManager.persistAndFlush(
                User.builder().username("bob").email("bob-" + suffix + "@test.com").password("pass").build());
        category = entityManager.persistAndFlush(
                Category.builder().name("Food").type("EXPENSE").user(user1).build());
    }

    // ─── findByUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: should return only transactions belonging to the given user")
    void findByUserId_returnsCorrectUser() {
        // user1 has 2 transactions, user2 has 1
        entityManager.persistAndFlush(buildExpense(500.0, user1));
        entityManager.persistAndFlush(buildExpense(1000.0, user1));
        entityManager.persistAndFlush(buildExpense(300.0, user2));
        entityManager.clear();

        List<Transaction> results = transactionRepository.findByUserId(user1.getId());

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(t -> t.getUser().getId().equals(user1.getId()));
    }

    @Test
    @DisplayName("findByUserId: should return empty list when user has no transactions")
    void findByUserId_returnsEmpty_forUserWithNoTransactions() {
        List<Transaction> results = transactionRepository.findByUserId(user2.getId());

        assertThat(results).isEmpty();
    }

    // ─── save & reload ───────────────────────────────────────────────────────

    @Test
    @DisplayName("save: transaction should be persisted with all fields intact")
    void save_persistsAllFields() {
        LocalDate txDate = LocalDate.of(2025, 4, 15);
        Transaction tx = Transaction.builder()
                .amount(750.0)
                .type("EXPENSE")
                .description("Dinner at restaurant")
                .date(txDate)
                .user(user1)
                .category(category)
                .build();

        Transaction saved = transactionRepository.save(tx);
        entityManager.clear(); // force reload from DB

        Transaction loaded = transactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getAmount()).isEqualTo(750.0);
        assertThat(loaded.getType()).isEqualTo("EXPENSE");
        assertThat(loaded.getDescription()).isEqualTo("Dinner at restaurant");
        assertThat(loaded.getDate()).isEqualTo(txDate);
        assertThat(loaded.getUser().getId()).isEqualTo(user1.getId());
        assertThat(loaded.getCategory().getName()).isEqualTo("Food");
    }

    // ─── Entity relationship integrity ───────────────────────────────────────

    @Test
    @DisplayName("entity relationship: transaction.user and transaction.category are loaded correctly")
    void entityRelationship_userAndCategoryLoaded() {
        Transaction tx = entityManager.persistAndFlush(buildExpense(200.0, user1));
        Long txId = tx.getId();
        Long userId = user1.getId();
        Long catId = category.getId();
        entityManager.clear();

        // Use entityManager.find so JPA loads within the current test transaction
        Transaction loaded = entityManager.find(Transaction.class, txId);

        // Verify ManyToOne relationships are populated
        assertThat(loaded.getUser()).isNotNull();
        assertThat(loaded.getUser().getId()).isEqualTo(userId);
        assertThat(loaded.getCategory()).isNotNull();
        assertThat(loaded.getCategory().getId()).isEqualTo(catId);
        assertThat(loaded.getCategory().getName()).isEqualTo("Food");
    }

    // ─── deleteAllByUserId ───────────────────────────────────────────────────

    @Test
    @Commit // Flush to DB so the subsequent read can see the delete
    @DisplayName("deleteAllByUserId: should remove all transactions of given user")
    void deleteAllByUserId_removesCorrectTransactions() {
        Transaction t1 = transactionRepository.save(buildExpense(500.0, user1));
        Transaction t2 = transactionRepository.save(buildExpense(700.0, user1));
        Transaction t3 = transactionRepository.save(buildExpense(300.0, user2));
        entityManager.flush();

        transactionRepository.deleteAllByUserId(user1.getId());
        transactionRepository.flush();

        // user1's transactions are gone
        assertThat(transactionRepository.findByUserId(user1.getId())).isEmpty();
        // user2's transaction is still there
        assertThat(transactionRepository.findByUserId(user2.getId())).hasSize(1);
    }

    // ─── Edge Cases ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("edge: save transaction with negative amount (reversal)")
    void save_negativeAmount_allowed() {
        Transaction tx = buildExpense(-100.0, user1);
        Transaction saved = transactionRepository.save(tx);
        entityManager.clear();

        Transaction loaded = transactionRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getAmount()).isEqualTo(-100.0);
    }

    @Test
    @DisplayName("edge: save transaction with very large amount")
    void save_largeAmount_noTruncation() {
        Transaction tx = buildExpense(999_999.99, user1);
        Transaction saved = transactionRepository.save(tx);
        entityManager.clear();

        Transaction loaded = transactionRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getAmount()).isEqualTo(999_999.99);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Transaction buildExpense(Double amount, User owner) {
        return Transaction.builder()
                .amount(amount)
                .type("EXPENSE")
                .description("test")
                .date(LocalDate.of(2025, 4, 1))
                .user(owner)
                .category(category)
                .build();
    }
}
