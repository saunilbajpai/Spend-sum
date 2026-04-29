package com.Spendsum.controller;

import com.Spendsum.model.*;
import com.Spendsum.service.AIInsightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for AIInsightController.
 * Tests validate:
 *   - POST /api/insights/generate
 *   - GET  /api/insights/user/{userId}
 *   - DELETE /api/insights/{id}
 *   - POST /api/insights/{id}/feedback
 */
@WebMvcTest(AIInsightController.class)
class AIInsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIInsightService aiInsightService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("a@a.com").password("pass").build();
    }

    // ─── POST /api/insights/generate ─────────────────────────────────────────

    @Test
    @DisplayName("POST /generate → 200 with list of AI insights")
    void generate_returns200WithInsights() throws Exception {
        AIInsight insight1 = buildInsight(1L, "Reduce Food spending", ActionType.SUGGESTION, Severity.LOW);
        AIInsight insight2 = buildInsight(2L, "You are in deficit!", ActionType.WARNING, Severity.HIGH);
        when(aiInsightService.generateInsights(any(User.class))).thenReturn(List.of(insight1, insight2));

        mockMvc.perform(post("/api/insights/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].insightText", is("Reduce Food spending")))
                .andExpect(jsonPath("$[0].action", is("SUGGESTION")))
                .andExpect(jsonPath("$[1].severity", is("HIGH")));
    }

    @Test
    @DisplayName("POST /generate → 200 with empty list when no insights generated")
    void generate_returns200WithEmptyList() throws Exception {
        when(aiInsightService.generateInsights(any(User.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/insights/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /generate → 500 when service throws exception")
    void generate_returns500_onError() throws Exception {
        when(aiInsightService.generateInsights(any(User.class)))
                .thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/api/insights/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().is5xxServerError());
    }

    // ─── GET /api/insights/user/{userId} ─────────────────────────────────────

    @Test
    @DisplayName("GET /user/{userId} → 200 with user's insights")
    void getByUser_returns200() throws Exception {
        AIInsight insight = buildInsight(1L, "Save more this month", ActionType.SUGGESTION, Severity.LOW);
        when(aiInsightService.getInsightsByUser(1L)).thenReturn(List.of(insight));

        mockMvc.perform(get("/api/insights/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].insightText", is("Save more this month")));
    }

    @Test
    @DisplayName("GET /user/{userId} → 200 with empty list when user has no insights")
    void getByUser_returnsEmpty() throws Exception {
        when(aiInsightService.getInsightsByUser(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/insights/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─── DELETE /api/insights/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/insights/{id} → 200 with success message")
    void delete_returns200() throws Exception {
        doNothing().when(aiInsightService).deleteInsight(1L);

        mockMvc.perform(delete("/api/insights/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));
    }

    // ─── POST /api/insights/{id}/feedback ────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/feedback with isHelpful=true → 200 with updated insight")
    void feedback_helpful_true() throws Exception {
        AIInsight updatedInsight = buildInsight(1L, "Good tip", ActionType.SUGGESTION, Severity.LOW);
        updatedInsight.setIsHelpful(true);
        when(aiInsightService.setHelpfulStatus(1L, true)).thenReturn(updatedInsight);

        mockMvc.perform(post("/api/insights/1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isHelpful", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHelpful", is(true)));
    }

    @Test
    @DisplayName("POST /{id}/feedback with isHelpful=false → 200 with updated insight")
    void feedback_helpful_false() throws Exception {
        AIInsight updatedInsight = buildInsight(2L, "Bad tip", ActionType.WARNING, Severity.HIGH);
        updatedInsight.setIsHelpful(false);
        when(aiInsightService.setHelpfulStatus(2L, false)).thenReturn(updatedInsight);

        mockMvc.perform(post("/api/insights/2/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isHelpful", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHelpful", is(false)));
    }

    @Test
    @DisplayName("POST /{id}/feedback → 500 when insight does not exist")
    void feedback_notFound_returns500() throws Exception {
        when(aiInsightService.setHelpfulStatus(999L, true))
                .thenThrow(new RuntimeException("Insight not found"));

        mockMvc.perform(post("/api/insights/999/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isHelpful", true))))
                .andExpect(status().is5xxServerError());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AIInsight buildInsight(Long id, String text, ActionType action, Severity severity) {
        return AIInsight.builder()
                .id(id)
                .insightText(text)
                .action(action)
                .severity(severity)
                .source(InsightSource.RULE_BASED)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
    }
}
