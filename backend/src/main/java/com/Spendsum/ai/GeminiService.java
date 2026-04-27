package com.Spendsum.ai;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate;

    public String generateInsight(String prompt) {
        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Constructing the payload based on Gemini API structure
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, String> parts = new HashMap<>();

            parts.put("text", prompt);
            contents.put("parts", Collections.singletonList(parts));
            requestBody.put("contents", Collections.singletonList(contents));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse response
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, String>> responseParts = (List<Map<String, String>>) content.get("parts");
                    if (responseParts != null && !responseParts.isEmpty()) {
                        return responseParts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini API failed: {}", e.getMessage());
        }

        return null; // Fallback handled by the caller
    }
}
