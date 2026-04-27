package com.Spendsum.agent;

import com.Spendsum.model.User;
import com.Spendsum.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentScheduler {

    private final AgentService agentService;
    private final UserRepository userRepository;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyAgentCheck() {
        log.info("Running daily agent check for all users...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                agentService.processUser(user.getId());
            } catch (Exception e) {
                log.error("Error processing user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
