package com.Spendsum.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Spendsum.model.Transaction;

import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long>{
        List<Transaction> findByUserId(Long userId);
        
        @Transactional
        void deleteAllByUserId(Long userId);
}
