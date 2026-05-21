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

    // SQL query for Question 1 (odd regNo last two digits = 47)
    // For each employee, count how many employees in the same department are younger
    // Younger = later DOB (higher date value)
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

        String webhookUrl = null;
        String accessToken = null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("name", "John Doe");
            requestBody.put("regNo", "REG12347");
            requestBody.put("email", "john@example.com");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    GENERATE_WEBHOOK_URL, entity, String.class);

            System.out.println("Generate Webhook Response Status: " + response.getStatusCode());
            System.out.println("Generate Webhook Response Body: " + response.getBody());

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            webhookUrl = responseJson.path("webhook").asText();
            accessToken = responseJson.path("accessToken").asText();

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token received: " + (accessToken != null && !accessToken.isEmpty()));

        } catch (Exception e) {
            System.err.println("Failed to generate webhook: " + e.getMessage());
            throw e;
        }

        submitSolution(webhookUrl, accessToken);
    }

    private void submitSolution(String webhookUrl, String accessToken) {
        int maxRetries = 4;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            System.out.println("Submission attempt " + attempt + " of " + maxRetries);

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", accessToken);

                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("finalQuery", FINAL_SQL_QUERY);

                HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl, entity, String.class);

                System.out.println("Submission Response Status: " + response.getStatusCode());
                System.out.println("Submission Response Body: " + response.getBody());

                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("=== Solution submitted successfully! ===");
                    return;
                }

            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        System.err.println("=== All " + maxRetries + " submission attempts failed ===");
    }
}
