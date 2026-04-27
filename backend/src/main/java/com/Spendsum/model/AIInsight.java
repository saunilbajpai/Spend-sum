package com.Spendsum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_insights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String insightText;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    @Enumerated(EnumType.STRING)
    private InsightSource source;

    private boolean executed;

    private LocalDateTime createdAt;

    // 🔗 Many insights → One user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}