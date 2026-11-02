package org.kylan1940.serverassistantai.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.logging.Logger;

public class ClaudeProvider extends AbstractHttpProvider {

    public static final String DEFAULT_MODEL = "claude-haiku-5-5";

    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private static final String API_VERSION = "2023-06-01";

    public ClaudeProvider(HttpClient httpClient, Logger logger, String apiKey, String model, int maxTokens) {
        super(httpClient, logger, apiKey, model, maxTokens);
    }

    @Override
    public String getName() {
        return "Claude";
    }

    @Override
    protected URI getEndpoint() {
        return ENDPOINT;
    }

    @Override
    protected void addHeaders(HttpRequest.Builder builder) {
        builder.header("x-api-key", apiKey);
        builder.header("anthropic-version", API_VERSION);
    }

    @Override
    protected String buildRequestBody(String systemPrompt, String userPrompt) {

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("system", systemPrompt);
        body.add("messages", messages);

        return body.toString();
    }

    @Override
    protected String parseResponse(JsonObject response) {

        if (!response.has("content") || response.getAsJsonArray("content").isEmpty()) {
            String reason = response.has("stop_reason") && !response.get("stop_reason").isJsonNull()
                    ? response.get("stop_reason").getAsString()
                    : "unknown";
            throw new IllegalStateException("AI response does not contain content (stop reason: " + reason + ").");
        }

        StringBuilder text = new StringBuilder();

        for (var element : response.getAsJsonArray("content")) {
            JsonObject block = element.getAsJsonObject();

            if (block.has("type") && block.get("type").getAsString().equals("text") && block.has("text")) {
                text.append(block.get("text").getAsString());
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("AI response does not contain text.");
        }

        return text.toString();
    }

}
