package org.kylan1940.serverassistantai.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.logging.Logger;

public class GeminiProvider extends AbstractHttpProvider {

    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    public GeminiProvider(HttpClient httpClient, Logger logger, String apiKey, String model, int maxTokens) {
        super(httpClient, logger, apiKey, model, maxTokens);
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    protected URI getEndpoint() {
        return URI.create(ENDPOINT.formatted(model));
    }

    @Override
    protected void addHeaders(HttpRequest.Builder builder) {
        // Sent as a header instead of "?key=" so the key never ends up in a URL.
        builder.header("x-goog-api-key", apiKey);
    }

    @Override
    protected String buildRequestBody(String systemPrompt, String userPrompt) {

        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt);

        JsonArray systemParts = new JsonArray();
        systemParts.add(systemPart);

        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", systemParts);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userPrompt);

        JsonArray userParts = new JsonArray();
        userParts.add(userPart);

        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userParts);

        JsonArray contents = new JsonArray();
        contents.add(userContent);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", maxTokens);

        JsonObject body = new JsonObject();
        body.add("systemInstruction", systemInstruction);
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        return body.toString();
    }

    @Override
    protected String parseResponse(JsonObject response) {

        if (!response.has("candidates") || response.getAsJsonArray("candidates").isEmpty()) {
            if (response.has("promptFeedback")) {
                throw new IllegalStateException("AI request was blocked by prompt feedback.");
            }
            throw new IllegalStateException("AI returned an empty response.");
        }

        JsonObject candidate = response.getAsJsonArray("candidates").get(0).getAsJsonObject();

        if (!candidate.has("content")) {
            if (candidate.has("finishReason")) {
                throw new IllegalStateException(
                        "AI response finished with reason: " + candidate.get("finishReason").getAsString());
            }
            throw new IllegalStateException("AI response does not contain content.");
        }

        JsonObject content = candidate.getAsJsonObject("content");

        if (!content.has("parts") || content.getAsJsonArray("parts").isEmpty()) {
            throw new IllegalStateException("AI response does not contain any parts.");
        }

        StringBuilder text = new StringBuilder();

        // Newer Gemini models can return several parts (for example thought summaries), keep only real text.
        for (var element : content.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();

            if (part.has("thought") && part.get("thought").getAsBoolean()) {
                continue;
            }
            if (part.has("text")) {
                text.append(part.get("text").getAsString());
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("AI response does not contain text.");
        }

        return text.toString();
    }

}
