package com.Spendsum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Spendsum.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Get all categories of a user
    List<Category> findByUserId(Long userId);

    // Prevent duplicate category per user
    Optional<Category> findByNameAndUserId(String name, Long userId);
}