package org.kylan1940.serverassistantai.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.logging.Logger;

/**
 * OpenAI Chat Completions. Also works with any OpenAI-compatible service
 * (OpenRouter, Groq, local servers, ...) by changing {@code base-url} in config.yml.
 */
public class OpenAIProvider extends AbstractHttpProvider {

    public static final String DEFAULT_MODEL = "gpt-5-mini";
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final String baseUrl;

    public OpenAIProvider(HttpClient httpClient, Logger logger, String apiKey, String model, int maxTokens, String baseUrl) {
        super(httpClient, logger, apiKey, model, maxTokens);

        String url = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String getName() {
        return "OpenAI";
    }

    @Override
    protected URI getEndpoint() {
        return URI.create(baseUrl + "/chat/completions");
    }

    @Override
    protected void addHeaders(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + apiKey);
    }

    @Override
    protected String buildRequestBody(String systemPrompt, String userPrompt) {

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(system);
        messages.add(user);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("max_completion_tokens", maxTokens);

        return body.toString();
    }

    @Override
    protected String parseResponse(JsonObject response) {

        if (!response.has("choices") || response.getAsJsonArray("choices").isEmpty()) {
            throw new IllegalStateException("AI returned an empty response.");
        }

        JsonObject choice = response.getAsJsonArray("choices").get(0).getAsJsonObject();

        if (!choice.has("message") || !choice.get("message").isJsonObject()) {
            throw new IllegalStateException("AI response does not contain a message.");
        }

        JsonObject message = choice.getAsJsonObject("message");

        if (message.has("refusal") && !message.get("refusal").isJsonNull()) {
            throw new IllegalStateException("AI refused to answer.");
        }

        if (!message.has("content") || message.get("content").isJsonNull()) {
            String reason = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()
                    ? choice.get("finish_reason").getAsString()
                    : "unknown";
            throw new IllegalStateException("AI response does not contain text (finish reason: " + reason + ").");
        }

        return message.get("content").getAsString();
    }

}
