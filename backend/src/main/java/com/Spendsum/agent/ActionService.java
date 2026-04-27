package com.Spendsum.agent;

import com.Spendsum.model.AIInsight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActionService {

    // Future scope: execute real actions like sending emails or notifications
    public void executeInsightAction(AIInsight insight) {
        log.info("Executing action for insight: {}", insight.getInsightText());
        // e.g., sendNotification(insight.getUser(), insight.getInsightText());
        insight.setExecuted(true);
    }
}
