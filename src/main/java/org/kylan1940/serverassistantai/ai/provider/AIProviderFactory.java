package org.kylan1940.serverassistantai.ai.provider;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.http.HttpClient;
import java.util.logging.Logger;

public final class AIProviderFactory {

    private AIProviderFactory() {
    }

    /**
     * Builds the provider selected by {@code ai.provider} using the values currently in the config.
     *
     * @return the provider, or null when {@code ai.provider} is not a supported name
     */
    public static AIProvider create(FileConfiguration config, HttpClient httpClient, Logger logger) {

        String name = config.getString("ai.provider", "gemini").trim().toLowerCase();
        int maxTokens = config.getInt("ai.max-response-length", 500);

        return switch (name) {
            case "gemini" -> new GeminiProvider(
                    httpClient, logger,
                    config.getString("ai.gemini.api-key", ""),
                    config.getString("ai.gemini.model", GeminiProvider.DEFAULT_MODEL),
                    maxTokens
            );
            case "claude", "anthropic" -> new ClaudeProvider(
                    httpClient, logger,
                    config.getString("ai.claude.api-key", ""),
                    config.getString("ai.claude.model", ClaudeProvider.DEFAULT_MODEL),
                    maxTokens
            );
            case "openai", "chatgpt" -> new OpenAIProvider(
                    httpClient, logger,
                    config.getString("ai.openai.api-key", ""),
                    config.getString("ai.openai.model", OpenAIProvider.DEFAULT_MODEL),
                    maxTokens,
                    config.getString("ai.openai.base-url", OpenAIProvider.DEFAULT_BASE_URL)
            );
            default -> null;
        };
    }

}
