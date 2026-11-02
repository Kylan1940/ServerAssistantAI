package org.kylan1940.serverassistantai.ai.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Shared plumbing for providers that talk to a JSON HTTP API.
 */
abstract class AbstractHttpProvider implements AIProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_LOGGED_ERROR_LENGTH = 300;

    protected final HttpClient httpClient;
    protected final Logger logger;
    protected final String apiKey;
    protected final String model;
    protected final int maxTokens;

    protected AbstractHttpProvider(HttpClient httpClient, Logger logger, String apiKey, String model, int maxTokens) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !apiKey.startsWith("YOUR_");
    }

    /**
     * Adds the provider specific headers (authentication, versions, ...).
     */
    protected abstract void addHeaders(HttpRequest.Builder builder);

    protected abstract URI getEndpoint();

    protected abstract String buildRequestBody(String systemPrompt, String userPrompt);

    protected abstract String parseResponse(JsonObject response);

    @Override
    public String generate(String systemPrompt, String userPrompt) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(getEndpoint())
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json");

        addHeaders(builder);

        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(systemPrompt, userPrompt)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warning(getName() + " API returned HTTP " + response.statusCode()
                    + extractErrorMessage(response.body()));
            throw new IOException(getName() + " API request failed.");
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonObject()) {
            throw new IllegalStateException("AI returned an invalid response.");
        }

        String text = parseResponse(parsed.getAsJsonObject()).trim();

        if (text.isEmpty()) {
            throw new IllegalStateException("AI returned an empty response.");
        }

        return text;
    }

    /**
     * Pulls the human readable error message out of an error body so the console shows
     * why a request failed (invalid key, unknown model, quota, ...). The API key is never logged.
     */
    private String extractErrorMessage(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);

            if (parsed.isJsonObject() && parsed.getAsJsonObject().has("error")) {
                JsonElement error = parsed.getAsJsonObject().get("error");
                String message = null;

                if (error.isJsonObject() && error.getAsJsonObject().has("message")) {
                    message = error.getAsJsonObject().get("message").getAsString();
                } else if (error.isJsonPrimitive()) {
                    message = error.getAsString();
                }

                if (message != null && !message.isBlank()) {
                    if (message.length() > MAX_LOGGED_ERROR_LENGTH) {
                        message = message.substring(0, MAX_LOGGED_ERROR_LENGTH) + "...";
                    }
                    if (!apiKey.isBlank()) {
                        message = message.replace(apiKey, "***");
                    }
                    return ": " + message;
                }
            }
        } catch (Exception ignored) {
            // Body was not JSON, nothing useful to add.
        }
        return "";
    }

}
