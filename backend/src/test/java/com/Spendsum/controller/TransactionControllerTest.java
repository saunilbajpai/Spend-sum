package com.Spendsum.controller;

import com.Spendsum.model.*;
import com.Spendsum.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for TransactionController.
 * Uses @WebMvcTest to load ONLY the web layer — service is mocked.
 * No database, no Spring Security complications.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private ObjectMapper objectMapper;
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // for LocalDate serialization
        user = User.builder().id(1L).username("alice").email("a@a.com").password("pass").build();
        category = Category.builder().id(10L).name("Food").type("EXPENSE").user(user).build();
    }

    // ─── GET /api/transactions ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions → 200 with list of transactions")
    void getAll_returns200WithList() throws Exception {
        Transaction tx = buildTransaction(1L, 500.0, "EXPENSE");
        when(transactionService.getAllTransactions()).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount", is(500.0)))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")));
    }

    @Test
    @DisplayName("GET /api/transactions → 200 with empty list when no data")
    void getAll_returns200WithEmptyList() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─── GET /api/transactions/{id} ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/{id} → 200 when found")
    void getById_returns200() throws Exception {
        Transaction tx = buildTransaction(5L, 1000.0, "INCOME");
        when(transactionService.getTransactionById(5L)).thenReturn(tx);

        mockMvc.perform(get("/api/transactions/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.amount", is(1000.0)));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} → 500 when service throws (not found)")
    void getById_returns500_whenNotFound() throws Exception {
        when(transactionService.getTransactionById(999L))
                .thenThrow(new RuntimeException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/999"))
                .andExpect(status().is5xxServerError());
    }

    // ─── POST /api/transactions ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/transactions → 200 with created transaction")
    void create_returns200() throws Exception {
        Transaction tx = buildTransaction(null, 750.0, "EXPENSE");
        Transaction saved = buildTransaction(1L, 750.0, "EXPENSE");
        when(transactionService.createTransaction(any(Transaction.class))).thenReturn(saved);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(750.0)));
    }

    @Test
    @DisplayName("POST /api/transactions → edge: negative amount is accepted (reversal)")
    void create_negativeAmount_accepted() throws Exception {
        Transaction tx = buildTransaction(null, -200.0, "EXPENSE");
        Transaction saved = buildTransaction(2L, -200.0, "EXPENSE");
        when(transactionService.createTransaction(any(Transaction.class))).thenReturn(saved);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(-200.0)));
    }

    // ─── PUT /api/transactions/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/transactions/{id} → 200 with updated transaction")
    void update_returns200() throws Exception {
        Transaction updated = buildTransaction(1L, 999.0, "INCOME");
        when(transactionService.updateTransaction(eq(1L), any(Transaction.class))).thenReturn(updated);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(999.0)));
    }

    // ─── DELETE /api/transactions/{id} ───────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/transactions/{id} → 200 with success message")
    void delete_returns200() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));
    }

    // ─── GET /api/transactions/summary/{userId}/{month}/{year} ───────────────

    @Test
    @DisplayName("GET /summary → 200 with income/expense/savings breakdown")
    void monthlySummary_returns200() throws Exception {
        when(transactionService.getMonthlySummary(1L, 4, 2025))
                .thenReturn(Map.of("income", 10000.0, "expense", 4000.0, "savings", 6000.0));

        mockMvc.perform(get("/api/transactions/summary/1/4/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income", is(10000.0)))
                .andExpect(jsonPath("$.expense", is(4000.0)))
                .andExpect(jsonPath("$.savings", is(6000.0)));
    }

    // ─── GET /api/transactions/category-wise/{userId} ────────────────────────

    @Test
    @DisplayName("GET /category-wise → 200 with category:amount map")
    void categoryWise_returns200() throws Exception {
        when(transactionService.getCategoryWiseSpending(1L))
                .thenReturn(Map.of("Food", 3000.0, "Rent", 5000.0));

        mockMvc.perform(get("/api/transactions/category-wise/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Food", is(3000.0)))
                .andExpect(jsonPath("$.Rent", is(5000.0)));
    }

    // ─── GET /api/transactions/top-category/{userId} ──────────────────────────

    @Test
    @DisplayName("GET /top-category → 200 with category name")
    void topCategory_returns200() throws Exception {
        when(transactionService.getTopSpendingCategory(1L)).thenReturn("Rent");

        mockMvc.perform(get("/api/transactions/top-category/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Rent"));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Transaction buildTransaction(Long id, Double amount, String type) {
        return Transaction.builder()
                .id(id)
                .amount(amount)
                .type(type)
                .description("Test transaction")
                .date(LocalDate.of(2025, 4, 15))
                .user(user)
                .category(category)
                .build();
    }
}
