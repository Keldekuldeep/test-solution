package com.bajaj.webhooksolution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebhookStartupRunner implements ApplicationRunner {

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    // Question 1 (regNo REG12347 -> last two digits 47 -> odd)
    // Count employees younger (DOB > current employee's DOB) in same department
    private static final String FINAL_SQL_QUERY =
            "SELECT e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME, " +
            "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
            "FROM EMPLOYEE e " +
            "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
            "LEFT JOIN EMPLOYEE e2 ON e2.DEPARTMENT = e.DEPARTMENT AND e2.DOB > e.DOB " +
            "GROUP BY e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME " +
            "ORDER BY e.EMP_ID DESC";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("=== Starting Webhook Flow ===");

        int maxRetries = 4;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.println("Attempt " + attempt + " of " + maxRetries);
            try {
                // Step 1: Get fresh webhook URL and token on every attempt
                JsonNode webhookResponse = generateWebhook();
                String webhookUrl = webhookResponse.path("webhook").asText();
                String accessToken = webhookResponse.path("accessToken").asText();

                System.out.println("Webhook URL: " + webhookUrl);
                System.out.println("Access Token received: " + !accessToken.isEmpty());

                // Step 2: Submit SQL solution
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", accessToken);

                ObjectNode body = objectMapper.createObjectNode();
                body.put("finalQuery", FINAL_SQL_QUERY);

                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

                System.out.println("Submission Status: " + response.getStatusCode());
                System.out.println("Submission Body: " + response.getBody());
                System.out.println("=== Solution submitted successfully! ===");
                return;

            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    Thread.sleep(2000L);
                }
            }
        }

        System.err.println("=== All " + maxRetries + " attempts failed ===");
    }

    private JsonNode generateWebhook() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", "John Doe");
        body.put("regNo", "REG12347");
        body.put("email", "john@example.com");

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(GENERATE_WEBHOOK_URL, entity, String.class);

        System.out.println("Generate Webhook Status: " + response.getStatusCode());
        return objectMapper.readTree(response.getBody());
    }
}
