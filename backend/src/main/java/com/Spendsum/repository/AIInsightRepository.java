package com.Spendsum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Spendsum.model.AIInsight;

import org.springframework.transaction.annotation.Transactional;

public interface AIInsightRepository extends JpaRepository<AIInsight, Long> {

    // Get insights of a user
    List<AIInsight> findByUserId(Long userId);
    
    @Transactional
    void deleteAllByUserId(Long userId);
}