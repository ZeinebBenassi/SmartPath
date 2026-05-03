package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.services.ConfigLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.SocketException;
import java.time.Duration;

/**
 * Groq AI Service for generating academic recommendations and insights.
 * Uses Groq Cloud API with Llama models.
 */
public class GroqAiService {

    private static final String DEFAULT_API_URL = firstNonBlank(
        System.getenv("GROQ_API_URL"),
        ConfigLoader.get("GROQ_API_URL"),
        "https://api.groq.com/openai/v1/chat/completions");
    private static final String DEFAULT_MODEL = firstNonBlank(
        System.getenv("GROQ_MODEL"),
        ConfigLoader.get("GROQ_MODEL"),
        "llama-3.3-70b-versatile");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqAiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .proxy(buildProxySelector())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a request to the Groq API.
     */
    public String ask(String systemPrompt, String userPrompt, int maxTokens) {
        String[] apiKeys = getApiKeys();
        if (apiKeys.length == 0) {
            return "### AI UNAVAILABLE ###\n\nThe Groq AI API key is not configured.";
        }

        String payload;
        try {
            payload = buildPayload(envOrDefault("GROQ_MODEL", DEFAULT_MODEL), systemPrompt, userPrompt, maxTokens);
        } catch (IOException e) {
            return "### SYSTEM ERROR ###\n\nUnable to build Groq request payload: " + e.getMessage();
        }

        String apiUrl = envOrDefault("GROQ_API_URL", DEFAULT_API_URL);
        String lastAuthFailure = null;

        for (String apiKey : apiKeys) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 401) {
                    lastAuthFailure = "The provided GROQ_API_KEY is invalid or expired.";
                    continue;
                }

                if (response.statusCode() >= 400) {
                    return "### GROQ API ERROR (" + response.statusCode() + ") ###\n\n" + response.body();
                }

                return parseTextResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "### SYSTEM ERROR ###\n\nOperation was interrupted: " + e.getMessage();
        } catch (IOException e) {
            return formatNetworkError(e);
        }
        }

        if (lastAuthFailure != null) {
            return "### INVALID API KEY ###\n\n" + lastAuthFailure + "\n\nChecked all configured Groq keys.";
        }

        return "### AI UNAVAILABLE ###\n\nThe Groq AI API key is not configured.";
    }

    private String formatNetworkError(IOException e) {
        String message = (e.getMessage() == null) ? "" : e.getMessage();
        StringBuilder out = new StringBuilder();
        out.append("### NETWORK ERROR ###\n\n");
        out.append("Could not connect to Groq AI: ")
                .append(e.getClass().getSimpleName())
                .append(message.isBlank() ? "" : ": " + message);

        // Common Windows/AV/firewall symptom when sockets are blocked.
        boolean looksLikeGetsockopt = message.toLowerCase().contains("getsockopt")
                || message.toLowerCase().contains("permission denied")
                || (e instanceof SocketException);

        if (looksLikeGetsockopt) {
            out.append("\n\nPossible causes:\n")
               .append("- Firewall/antivirus is blocking outbound HTTPS\n")
               .append("- A corporate proxy is required\n")
               .append("- Network access is restricted for this process\n\n")
               .append("If you are behind a proxy, set HTTPS_PROXY (or HTTP_PROXY) like: http://host:port and restart the app.");
        }

        return out.toString();
    }

    private ProxySelector buildProxySelector() {
        // Prefer explicit environment variables, otherwise fall back to system proxy selector.
        String proxy = firstNonBlank(
                System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"),
                System.getenv("HTTP_PROXY"), System.getenv("http_proxy"));

        if (proxy == null || proxy.isBlank()) {
            return ProxySelector.getDefault();
        }

        try {
            URI uri = new URI(proxy.trim());
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || host.isBlank() || port <= 0) {
                return ProxySelector.getDefault();
            }
            return ProxySelector.of(new InetSocketAddress(host, port));
        } catch (URISyntaxException ex) {
            return ProxySelector.getDefault();
        }
    }

    public boolean isConfigured() {
        return getApiKeys().length > 0;
    }

    private String[] getApiKeys() {
        return new String[] {
                firstNonBlank(System.getenv("GROQ_API_KEY"), ConfigLoader.get("GROQ_API_KEY"), ConfigLoader.get("groq.api.key")),
                firstNonBlank(System.getenv("GROQ_API_KEY_2"), ConfigLoader.get("GROQ_API_KEY_2"), ConfigLoader.get("groq.api.key.2"))
        };
    }

    private String buildPayload(String model, String systemPrompt, String userPrompt, int maxTokens) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.4);
        root.put("max_tokens", maxTokens);

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        return objectMapper.writeValueAsString(root);
    }

    private String parseTextResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "### EMPTY RESPONSE ###\n\nGroq AI returned no suggestions.";
        }
        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            return "### EMPTY CONTENT ###\n\nGroq AI returned an empty response body.";
        }
        return content.trim();
    }

    private String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String configValue = ConfigLoader.get(key);
        return (configValue == null || configValue.isBlank()) ? fallback : configValue.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
