package com.Spendsum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String type; // INCOME or EXPENSE

    private LocalDateTime createdAt;

    // 🔗 Many transactions → One user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔗 Many transactions → One category
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}