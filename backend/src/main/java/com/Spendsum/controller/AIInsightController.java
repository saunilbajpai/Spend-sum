package com.Spendsum.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Spendsum.model.AIInsight;
import com.Spendsum.model.User;
import com.Spendsum.service.AIInsightService;

@RestController
@RequestMapping("/api/insights")
public class AIInsightController {

    @Autowired
    private AIInsightService aiInsightService;

    // 🔥 Generate insights
    @PostMapping("/generate")
    public ResponseEntity<List<AIInsight>> generateInsights(@RequestBody User user) {
        return ResponseEntity.ok(aiInsightService.generateInsights(user));
    }

    // ✅ Get user insights
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AIInsight>> getInsights(@PathVariable Long userId) {
        return ResponseEntity.ok(aiInsightService.getInsightsByUser(userId));
    }

    // ✅ Delete insight
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        aiInsightService.deleteInsight(id);
        return ResponseEntity.ok("Insight deleted successfully");
    }
}