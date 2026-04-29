package com.Spendsum.integration;

import com.Spendsum.config.TestcontainersConfig;
import com.Spendsum.model.*;
import com.Spendsum.repository.*;
import com.Spendsum.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test for the Transaction flow.
 *
 * Uses @SpringBootTest to load the ENTIRE application context and
 * @Import(TestcontainersConfig.class) to wire in the real MySQL container.
 *
 * AgentService is mocked because it's @Async and calls GeminiService
 * (external HTTP). We want deterministic, fast integration tests without
 * network calls.
 *
 * Flow tested:
 *   POST /api/transactions → persists to MySQL → GET retrieves it
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Mock AgentService to prevent actual Gemini API calls during integration tests
    @MockBean
    private com.Spendsum.agent.AgentService agentService;

    private ObjectMapper objectMapper;
    private User savedUser;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Set up real test data in the MySQL container
        savedUser = userRepository.save(
                User.builder().username("integ_user").email("integ@test.com").password("pass").build());
        savedCategory = categoryRepository.save(
                Category.builder().name("Food").type("EXPENSE").user(savedUser).build());
    }

    @AfterEach
    void tearDown() {
        // Clean up in FK order
        transactionRepository.deleteAllByUserId(savedUser.getId());
        categoryRepository.deleteById(savedCategory.getId());
        userRepository.deleteById(savedUser.getId());
    }

    // ─── Create Transaction Integration ──────────────────────────────────────

    @Test
    @DisplayName("POST /api/transactions → persists to MySQL and returns created transaction")
    void createTransaction_persistedToRealDB() throws Exception {
        Transaction tx = Transaction.builder()
                .amount(1500.0)
                .type("EXPENSE")
                .description("Grocery shopping")
                .date(LocalDate.of(2025, 4, 20))
                .user(savedUser)
                .category(savedCategory)
                .build();

        String responseBody = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andReturn().getResponse().getContentAsString();

        // Verify it's actually in the database
        List<Transaction> savedTxs = transactionRepository.findByUserId(savedUser.getId());
        assertThat(savedTxs).hasSize(1);
        assertThat(savedTxs.get(0).getAmount()).isEqualTo(1500.0);
        assertThat(savedTxs.get(0).getDescription()).isEqualTo("Grocery shopping");
    }

    // ─── Retrieve Transaction by User ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/user/{userId} → returns transactions from MySQL")
    void getTransactionsByUser_returnsFromRealDB() throws Exception {
        // Seed data directly into MySQL container
        transactionRepository.save(buildExpense(500.0, "Coffee"));
        transactionRepository.save(buildExpense(2000.0, "Dinner"));

        mockMvc.perform(get("/api/transactions/user/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ─── Monthly Summary Integration ──────────────────────────────────────────

    @Test
    @DisplayName("GET /summary → computes correct totals from real MySQL data")
    void monthlySummary_computedFromRealDB() throws Exception {
        transactionRepository.save(
                Transaction.builder().amount(10000.0).type("INCOME").description("Salary")
                        .date(LocalDate.of(2025, 4, 1)).user(savedUser).category(savedCategory).build());
        transactionRepository.save(
                Transaction.builder().amount(3000.0).type("EXPENSE").description("Rent")
                        .date(LocalDate.of(2025, 4, 15)).user(savedUser).category(savedCategory).build());

        mockMvc.perform(get("/api/transactions/summary/" + savedUser.getId() + "/4/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(10000.0))
                .andExpect(jsonPath("$.expense").value(3000.0))
                .andExpect(jsonPath("$.savings").value(7000.0));
    }

    // ─── Delete Integration ───────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/transactions/{id} → removes from MySQL")
    void deleteTransaction_removedFromRealDB() throws Exception {
        Transaction saved = transactionRepository.save(buildExpense(100.0, "Test"));

        mockMvc.perform(delete("/api/transactions/" + saved.getId()))
                .andExpect(status().isOk());

        assertThat(transactionRepository.findById(saved.getId())).isEmpty();
    }

    // ─── Edge Case: Get non-existent transaction ──────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/{id} → 500 when ID does not exist in DB")
    void getById_nonExistentId_returns500() throws Exception {
        mockMvc.perform(get("/api/transactions/99999"))
                .andExpect(status().is5xxServerError());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Transaction buildExpense(Double amount, String description) {
        return Transaction.builder()
                .amount(amount)
                .type("EXPENSE")
                .description(description)
                .date(LocalDate.of(2025, 4, 10))
                .user(savedUser)
                .category(savedCategory)
                .build();
    }
}
