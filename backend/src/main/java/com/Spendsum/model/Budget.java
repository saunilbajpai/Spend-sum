package com.Spendsum.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double limitAmount;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    // 🔗 Many budgets → One user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔗 Many budgets → One category
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}