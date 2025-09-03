package com.example.bfhl_qualifier.runner;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.bfhl_qualifier.config.BfhlProps;
import com.example.bfhl_qualifier.dto.FinalQueryPayload;
import com.example.bfhl_qualifier.dto.GenerateWebhookRequest;
import com.example.bfhl_qualifier.dto.GenerateWebhookResponse;

@Component
public class QualifierRunner implements CommandLineRunner {

    private final WebClient webClient;
    private final BfhlProps props;

    public QualifierRunner(WebClient webClient, BfhlProps props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public void run(String... args) throws Exception {
        String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        System.out.println("Calling generateWebhook...");

        var req = new GenerateWebhookRequest(props.getName(), props.getRegNo(), props.getEmail());
        GenerateWebhookResponse resp = webClient.post()
                .uri(generateUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(GenerateWebhookResponse.class)
                .block();

        if (resp == null || resp.webhook() == null || resp.accessToken() == null) {
            throw new IllegalStateException("generateWebhook returned null fields.");
        }

        System.out.println("Received webhook: " + resp.webhook());

        // Load final SQL: prefer bfhl.finalQuery; else read solution.sql from resources
        String finalQuery = props.getFinalQuery();
        if (finalQuery == null || finalQuery.isBlank()) {
            var resource = new ClassPathResource("solution.sql");
            if (!resource.exists()) {
                throw new IllegalStateException("solution.sql not found in resources");
            }
            finalQuery = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
        }
        System.out.println("Loaded final SQL:\n" + finalQuery);

        // First try EXACTLY as brief: Authorization: <accessToken>
        System.out.println("Posting finalQuery to webhook (Authorization: <token>)...");
        try {
            String submitResp = webClient.post()
                    .uri(resp.webhook())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.set("Authorization", resp.accessToken()))
                    .bodyValue(new FinalQueryPayload(finalQuery))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Submission response: " + submitResp);
            return;
        } catch (WebClientResponseException e) {
            // If 401, retry with Bearer prefix (some envs expect this)
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.out.println("401 received. Retrying with Authorization: Bearer <token>...");
                String submitResp = webClient.post()
                        .uri(resp.webhook())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(h -> h.set("Authorization", "Bearer " + resp.accessToken()))
                        .bodyValue(new FinalQueryPayload(finalQuery))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                System.out.println("Submission response: " + submitResp);
            } else {
                throw e;
            }
        }
    }
}
