package com.Spendsum.controller;

import com.Spendsum.agent.AgentService;
import com.Spendsum.model.*;
import com.Spendsum.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final AIInsightRepository aiInsightRepository;
    private final AgentService agentService;

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData(@RequestParam(defaultValue = "false") boolean reset) {
        Long targetUserId = 1L;

        // 1. Ensure User Exists
        User user = userRepository.findById(targetUserId).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("demo_user");
            newUser.setEmail("demo@spendsum.com");
            newUser.setPassword("password"); // Obviously bad practice outside of local dev
            return userRepository.save(newUser);
        });

        // 2. Handle Reset Mode
        if (reset) {
            transactionRepository.deleteAllByUserId(targetUserId);
            aiInsightRepository.deleteAllByUserId(targetUserId);
        }

        // 3. Ensure Categories Exist
        List<Category> existingCategories = categoryRepository.findByUserId(targetUserId);
        Map<String, Category> categoryMap = new HashMap<>();
        for (Category c : existingCategories) {
            categoryMap.put(c.getName(), c);
        }

        int categoriesCreated = 0;
        String[][] defaultCategories = {
                {"Food", "EXPENSE"},
                {"Travel", "EXPENSE"},
                {"Shopping", "EXPENSE"},
                {"Bills", "EXPENSE"},
                {"Entertainment", "EXPENSE"},
                {"Salary", "INCOME"},
                {"Freelance", "INCOME"}
        };

        for (String[] def : defaultCategories) {
            if (!categoryMap.containsKey(def[0])) {
                Category c = new Category();
                c.setName(def[0]);
                c.setType(def[1]);
                c.setUser(user);
                Category saved = categoryRepository.save(c);
                categoryMap.put(def[0], saved);
                categoriesCreated++;
            }
        }

        // 4. Ensure Budgets Exist for current month
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        List<Budget> existingBudgets = budgetRepository.findByUserId(targetUserId);
        
        int budgetsCreated = 0;
        Map<String, Double> defaultBudgets = Map.of(
                "Food", 8000.0,
                "Travel", 5000.0,
                "Shopping", 6000.0,
                "Bills", 4000.0,
                "Entertainment", 3000.0
        );

        for (Map.Entry<String, Double> entry : defaultBudgets.entrySet()) {
            boolean budgetExists = existingBudgets.stream()
                    .anyMatch(b -> b.getCategory().getName().equals(entry.getKey()) 
                                && b.getMonth() == currentMonth 
                                && b.getYear() == currentYear);
            
            if (!budgetExists) {
                Budget b = new Budget();
                b.setLimitAmount(entry.getValue());
                b.setMonth(currentMonth);
                b.setYear(currentYear);
                b.setUser(user);
                b.setCategory(categoryMap.get(entry.getKey()));
                budgetRepository.save(b);
                budgetsCreated++;
            }
        }

        // 5. Generate Realistic Transactions
        // We only generate if reset is true or if the user has very few transactions, to avoid bloat
        List<Transaction> transactionsToSave = new ArrayList<>();
        Random random = new Random();
        int txCount = reset ? 100 : 0; // If not resetting, we only add data if they explicitly asked for it by resetting.
        
        if (reset || transactionRepository.findByUserId(targetUserId).size() < 10) {
            txCount = 80 + random.nextInt(41); // 80 to 120
            
            for (int i = 0; i < txCount; i++) {
                Transaction t = new Transaction();
                t.setUser(user);
                
                // Random date in current month (1 to 28 to be safe)
                int day = 1 + random.nextInt(28);
                t.setDate(LocalDate.now().withDayOfMonth(day));
                
                boolean isIncome = random.nextInt(100) < 20; // 20% income
                if (isIncome) {
                    t.setType("INCOME");
                    boolean isSalary = random.nextBoolean();
                    if (isSalary) {
                        t.setCategory(categoryMap.get("Salary"));
                        t.setAmount(30000.0 + random.nextInt(50000));
                        t.setDescription("Monthly Salary");
                        t.setDate(LocalDate.now().withDayOfMonth(1)); // Salary on 1st
                    } else {
                        t.setCategory(categoryMap.get("Freelance"));
                        t.setAmount(5000.0 + random.nextInt(15000));
                        t.setDescription("Freelance Project");
                    }
                } else {
                    t.setType("EXPENSE");
                    // Weight categories
                    int catRoll = random.nextInt(100);
                    if (catRoll < 30) {
                        t.setCategory(categoryMap.get("Food"));
                        t.setAmount(200.0 + random.nextInt(600));
                        t.setDescription(day % 2 == 0 ? "Lunch at Cafe" : "Groceries");
                    } else if (catRoll < 50) {
                        t.setCategory(categoryMap.get("Shopping"));
                        t.setAmount(300.0 + random.nextInt(1700));
                        t.setDescription("Online Order");
                    } else if (catRoll < 70) {
                        t.setCategory(categoryMap.get("Travel"));
                        t.setAmount(500.0 + random.nextInt(2500));
                        t.setDescription("Uber Ride");
                    } else if (catRoll < 85) {
                        t.setCategory(categoryMap.get("Bills"));
                        t.setAmount(1000.0 + random.nextInt(3000));
                        t.setDescription("Utility Bill");
                        t.setDate(LocalDate.now().withDayOfMonth(1 + random.nextInt(5))); // Early month
                    } else {
                        t.setCategory(categoryMap.get("Entertainment"));
                        t.setAmount(200.0 + random.nextInt(1300));
                        t.setDescription("Movie Tickets");
                        // Push to weekends theoretically (just random days)
                        t.setDate(LocalDate.now().withDayOfMonth(15 + random.nextInt(10)));
                    }
                }
                transactionsToSave.add(t);
            }
            
            // To ensure anomaly triggers: 
            // 1. Force a huge shopping expense to trigger velocity/over-budget
            Transaction anomaly = new Transaction();
            anomaly.setUser(user);
            anomaly.setDate(LocalDate.now());
            anomaly.setType("EXPENSE");
            anomaly.setCategory(categoryMap.get("Shopping"));
            anomaly.setAmount(7000.0); // Exceeds 6000 budget!
            anomaly.setDescription("New Laptop");
            transactionsToSave.add(anomaly);
            
            transactionRepository.saveAll(transactionsToSave);
        }

        // 6. Automatically Trigger Agent System (Crucial for insights & metrics)
        // By calling this here, we evaluate all transactions at once!
        if (transactionsToSave.size() > 0) {
            agentService.processUser(targetUserId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sample data generated successfully");
        response.put("transactionsCreated", transactionsToSave.size());
        response.put("categoriesCreated", categoriesCreated);
        response.put("budgetsCreated", budgetsCreated);

        return ResponseEntity.ok(response);
    }
}
