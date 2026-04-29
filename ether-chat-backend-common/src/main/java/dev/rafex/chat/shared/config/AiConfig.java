package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;
import java.util.Locale;
import java.util.Optional;

public record AiConfig(String provider, Optional<String> deepseekApiKey, String deepseekModel, String deepseekBaseUrl) {
    public static AiConfig from(EtherConfig config) {
        var provider = config.get("AI_PROVIDER").orElse("echo").trim().toLowerCase(Locale.ROOT);
        var apiKey = config.get("DEEPSEEK_API_KEY").map(String::trim).filter(s -> !s.isEmpty());
        var model = config.get("DEEPSEEK_MODEL").orElse("deepseek-chat").trim();
        var baseUrl = config.get("DEEPSEEK_BASE_URL").orElse("https://api.deepseek.com/v1").trim();
        return new AiConfig(provider, apiKey, model, baseUrl);
    }
}
