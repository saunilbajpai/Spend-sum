package com.Spendsum.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // INCOME or EXPENSE

    // 🔗 Many Categories → One User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}