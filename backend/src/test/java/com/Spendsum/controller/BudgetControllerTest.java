package com.Spendsum.controller;

import com.Spendsum.model.*;
import com.Spendsum.service.BudgetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for BudgetController.
 * Tests cover create, get, delete, exceeded-check, remaining, and status endpoints.
 */
@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    private Category category;
    private Budget budget;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("a@a.com").password("p").build();
        category = Category.builder().id(10L).name("Food").type("EXPENSE").user(user).build();
        budget = Budget.builder()
                .id(1L).limitAmount(5000.0).month(4).year(2025)
                .user(user).category(category).build();
    }

    // ─── POST /api/budgets ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/budgets → 200 with created budget")
    void create_returns200() throws Exception {
        when(budgetService.createBudget(any(Budget.class))).thenReturn(budget);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budget)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.limitAmount", is(5000.0)));
    }

    // ─── GET /api/budgets ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/budgets → 200 with list")
    void getAll_returns200() throws Exception {
        when(budgetService.getAllBudgets()).thenReturn(List.of(budget));

        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].month", is(4)));
    }

    // ─── GET /api/budgets/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/budgets/{id} → 200 when found")
    void getById_returns200() throws Exception {
        when(budgetService.getBudgetById(1L)).thenReturn(budget);

        mockMvc.perform(get("/api/budgets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount", is(5000.0)));
    }

    @Test
    @DisplayName("GET /api/budgets/{id} → 500 when not found")
    void getById_returns500_whenMissing() throws Exception {
        when(budgetService.getBudgetById(999L))
                .thenThrow(new RuntimeException("Budget not found"));

        mockMvc.perform(get("/api/budgets/999"))
                .andExpect(status().is5xxServerError());
    }

    // ─── DELETE /api/budgets/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/budgets/{id} → 200 with success message")
    void delete_returns200() throws Exception {
        doNothing().when(budgetService).deleteBudget(1L);

        mockMvc.perform(delete("/api/budgets/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));
    }

    // ─── GET /api/budgets/exceeded/{userId}/{categoryId}/{month}/{year} ───────

    @Test
    @DisplayName("GET /exceeded → true when budget exceeded")
    void exceeded_returnsTrue() throws Exception {
        when(budgetService.isBudgetExceeded(1L, 10L, 4, 2025)).thenReturn(true);

        mockMvc.perform(get("/api/budgets/exceeded/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /exceeded → false when budget not exceeded")
    void exceeded_returnsFalse() throws Exception {
        when(budgetService.isBudgetExceeded(1L, 10L, 4, 2025)).thenReturn(false);

        mockMvc.perform(get("/api/budgets/exceeded/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ─── GET /api/budgets/remaining/{userId}/{categoryId}/{month}/{year} ──────

    @Test
    @DisplayName("GET /remaining → 200 with positive remaining amount")
    void remaining_returnsPositive() throws Exception {
        when(budgetService.getRemainingBudget(1L, 10L, 4, 2025)).thenReturn(2500.0);

        mockMvc.perform(get("/api/budgets/remaining/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string("2500.0"));
    }

    @Test
    @DisplayName("GET /remaining → 200 with negative value when overrun")
    void remaining_returnsNegative_whenOverBudget() throws Exception {
        when(budgetService.getRemainingBudget(1L, 10L, 4, 2025)).thenReturn(-1500.0);

        mockMvc.perform(get("/api/budgets/remaining/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string("-1500.0"));
    }

    // ─── GET /api/budgets/status/{userId}/{categoryId}/{month}/{year} ─────────

    @Test
    @DisplayName("GET /status → 200 with EXCEEDED message")
    void status_returns_exceededMessage() throws Exception {
        when(budgetService.getBudgetStatus(1L, 10L, 4, 2025))
                .thenReturn("⚠️ Budget exceeded!");

        mockMvc.perform(get("/api/budgets/status/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("exceeded")));
    }

    @Test
    @DisplayName("GET /status → 200 with WARNING message at 80%")
    void status_returns_warningMessage() throws Exception {
        when(budgetService.getBudgetStatus(1L, 10L, 4, 2025))
                .thenReturn("⚡ Warning: You have used 84.0% of your budget");

        mockMvc.perform(get("/api/budgets/status/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Warning")));
    }

    @Test
    @DisplayName("GET /status → 200 with OK message below 80%")
    void status_returns_okMessage() throws Exception {
        when(budgetService.getBudgetStatus(1L, 10L, 4, 2025))
                .thenReturn("✅ You are within budget");

        mockMvc.perform(get("/api/budgets/status/1/10/4/2025"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("within budget")));
    }
}
