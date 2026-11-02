package org.kylan1940.serverassistantai.ai.provider;

/**
 * A single AI backend (Gemini, Claude, OpenAI, ...).
 * <p>
 * Instances are immutable snapshots of the configuration at the time they were created,
 * so they are safe to use from an asynchronous thread.
 */
public interface AIProvider {

    /**
     * Display name used in logs, e.g. "Gemini".
     */
    String getName();

    /**
     * @return false when the API key is missing or still the template placeholder.
     */
    boolean isConfigured();

    /**
     * Sends a request to the AI and returns the plain text answer.
     *
     * @param systemPrompt trusted instructions and server information
     * @param userPrompt   untrusted player question
     * @return the AI answer, never blank
     * @throws Exception when the request fails or the response is empty / blocked
     */
    String generate(String systemPrompt, String userPrompt) throws Exception;

}
