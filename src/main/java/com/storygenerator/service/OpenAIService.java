package com.storygenerator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Check if OpenAI is enabled (API key is configured and not empty)
     */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_API_KEY");
    }

    /**
     * Generate a story using OpenAI API
     * Returns null if generation fails (so caller can fallback)
     */
    public String generateStory(String characterName, String theme, String language, String ageGroup) {
        if (!isEnabled()) {
            log.debug("OpenAI not enabled, skipping API call");
            return null;
        }

        try {
            String langInstruction = switch (language) {
                case "hi" -> "Write the story entirely in Hindi (Devanagari script).";
                case "hinglish" -> "Write the story in Hinglish (a mix of Hindi and English written in Roman script).";
                case "bn" -> "Write the story entirely in Bengali (Bengali script).";
                default -> "Write the story in English.";
            };

            String ageInstruction = switch (ageGroup != null ? ageGroup : "6-8") {
                case "3-5" -> "The child is 3-5 years old. Use very simple, short sentences. Use lots of repetition, onomatopoeia, and sensory words. Keep the story under 150 words. Use very basic vocabulary.";
                case "9-12" -> "The child is 9-12 years old. Use richer vocabulary and more complex plot elements. Include descriptive language and character development. The story can be 300-400 words.";
                default -> "The child is 6-8 years old. Use moderate vocabulary with some descriptive words. Keep sentences clear and engaging. The story should be 200-300 words.";
            };

            String systemPrompt = "You are a creative children's story writer. Create engaging, age-appropriate, magical stories for kids. "
                    + "Always keep stories positive, fun, and educational. Never include violence, scary content, or inappropriate themes. "
                    + "Include emojis sparingly to make the story fun. " + langInstruction;

            String userPrompt = "Create a " + theme + " story for a child. The main character's name is " + characterName + ". "
                    + ageInstruction + " "
                    + "Make it magical, engaging, and end with a positive moral. Include vivid descriptions.";

            String requestBody = String.format("""
                    {
                      "model": "%s",
                      "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                      ],
                      "max_tokens": 800,
                      "temperature": 0.8
                    }""",
                    model,
                    escapeJson(systemPrompt),
                    escapeJson(userPrompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Simple JSON parsing to extract content
                String content = extractContentFromResponse(body);
                if (content != null && !content.isBlank()) {
                    log.info("OpenAI story generated successfully for theme={}, lang={}, age={}", theme, language, ageGroup);
                    return content;
                }
            } else {
                log.warn("OpenAI API returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
        }

        return null; // fallback to local generation
    }

    /**
     * Extract the message content from OpenAI's JSON response.
     * Uses simple string parsing to avoid adding a JSON library dependency.
     */
    private String extractContentFromResponse(String json) {
        try {
            // Look for "content": "..." in the response
            String marker = "\"content\":";
            int idx = json.lastIndexOf(marker);
            if (idx == -1) return null;

            int start = json.indexOf("\"", idx + marker.length());
            if (start == -1) return null;
            start++; // skip opening quote

            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    switch (c) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> { sb.append('\\'); sb.append(c); }
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break; // end of content string
                } else {
                    sb.append(c);
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
