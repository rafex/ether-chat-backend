package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.domain.MessageRole;
import dev.rafex.chat.chat.port.AiGateway;
import dev.rafex.ether.json.JsonCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DeepseekAiGateway implements AiGateway {
    private final HttpClient client;
    private final JsonCodec json;
    private final String apiKey;
    private final String model;
    private final String endpoint;

    public DeepseekAiGateway(JsonCodec json, String apiKey, String model, String baseUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), json, apiKey, model, baseUrl);
    }

    DeepseekAiGateway(HttpClient client, JsonCodec json, String apiKey, String model, String baseUrl) {
        this.client = Objects.requireNonNull(client, "client");
        this.json = Objects.requireNonNull(json, "json");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
        this.endpoint = normalizeBaseUrl(Objects.requireNonNull(baseUrl, "baseUrl")) + "/chat/completions";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generate(List<Message> history) {
        var messages = history == null ? List.of() : history.stream()
            .map(m -> Map.of("role", toProviderRole(m.role()), "content", m.content()))
            .toList();

        var body = Map.<String, Object>of("model", model, "messages", messages);
        var request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(json.toJsonBytes(body)))
            .build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DeepSeek request failed with status " + response.statusCode());
            }
            var payload = json.readValue(new ByteArrayInputStream(response.body()), Map.class);
            var choices = (List<Map<String, Object>>) payload.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("DeepSeek response did not contain choices");
            }
            var first = choices.get(0);
            var message = (Map<String, Object>) first.get("message");
            var content = message != null ? (String) message.get("content") : null;
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek response did not contain message content");
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("DeepSeek request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek request interrupted", e);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        var trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String toProviderRole(MessageRole role) {
        if (role == MessageRole.agent) {
            return "assistant";
        }
        return role.name();
    }
}
