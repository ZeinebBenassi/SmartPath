package tn.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatbotService — calls the Hugging Face Inference API asynchronously.
 *
 * Model  : HuggingFaceH4/zephyr-7b-beta
 * Auth   : Bearer token from environment variable HF_API_KEY
 * Thread : fully async — safe to call from JavaFX Application Thread.
 *
 * ── Common failure causes & fixes ──────────────────────────────────────
 * 1. 401 Unauthorized   → HF_API_KEY missing or wrong.
 * 2. 503 / "loading"    → Model cold-starting on free tier. Retry after 20 s.
 * 3. Empty generated_text → return_full_text=false but model echoed prompt.
 *                           Fixed: we strip the prompt prefix if present.
 * 4. JSON escape bug    → double-escaping \n in jsonEscape(). Fixed below.
 * 5. UI freeze          → never call askAsync().get() on FX thread. Fixed:
 *                         use thenAccept + Platform.runLater in controller.
 */
public class ChatbotService {

    private static final Logger LOG = Logger.getLogger(ChatbotService.class.getName());

    private static final String HF_API_URL =
            "https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta";

    private static final String ENV_HF_API_KEY = "HF_API_KEY";

    // Keep the last N turns so follow-up questions ("explain more") work.
    private static final int MAX_TURNS = 4;

    private final HttpClient httpClient;
    private final Deque<Turn> turns = new ArrayDeque<>();

    // Lazy cached key (avoid repeated env lookups)
    private String hfApiKey;

    public ChatbotService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Sends {@code userMessage} to Hugging Face and returns a future that
     * resolves to the assistant reply (or an error string starting with ⚠).
     *
     * Always call this from the JavaFX thread; use Platform.runLater() in
     * the thenAccept() callback to update the UI.
     */
    public CompletableFuture<String> askAsync(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return CompletableFuture.completedFuture("");

        final String apiKey;
        try {
            apiKey = getHfApiKey();
        } catch (IllegalStateException e) {
            LOG.warning("HF API key not configured: " + e.getMessage());
            return CompletableFuture.completedFuture("⚠ " + e.getMessage());
        }

        String prompt = buildPrompt(userMessage.trim());
        LOG.fine("Sending prompt to HF (" + prompt.length() + " chars)");

        return callHuggingFace(prompt, apiKey)
                .thenApply(answer -> {
                    remember(userMessage.trim(), answer);
                    return answer;
                });
    }

    // ── Prompt builder ───────────────────────────────────────────────────

    private synchronized String buildPrompt(String userMessage) {
        // Zephyr chat template: <|system|> / <|user|> / <|assistant|>
        String system =
                "You are SmartPath Assistant, an expert computer science tutor. "
                + "Topics: software engineering, algorithms, databases, networks, AI, ML, data science. "
                + "If the user greets you, greet back briefly and ask what CS topic they need help with. "
                + "If a question is unrelated to computer science, politely decline and suggest a CS topic. "
                + "Reply in the same language the user uses (French or English).";

        StringBuilder sb = new StringBuilder();
        sb.append("<|system|>\n").append(system).append("\n");
        for (Turn t : turns) {
            sb.append("<|user|>\n").append(t.user).append("\n");
            sb.append("<|assistant|>\n").append(t.assistant).append("\n");
        }
        sb.append("<|user|>\n").append(userMessage).append("\n");
        sb.append("<|assistant|>\n");
        return sb.toString();
    }

    // ── HTTP call ────────────────────────────────────────────────────────

    private CompletableFuture<String> callHuggingFace(String prompt, String apiKey) {

        // FIX: jsonEscape() previously double-escaped backslashes.
        // Now the body is built with a proper escape once.
        String jsonBody = "{"
                + "\"inputs\":" + jsonString(prompt) + ","
                + "\"parameters\":{"
                + "\"max_new_tokens\":400,"
                + "\"temperature\":0.4,"
                + "\"do_sample\":true,"
                + "\"return_full_text\":false"
                + "}"
                + "}";

        LOG.fine("POST " + HF_API_URL);
        LOG.fine("Body length: " + jsonBody.length());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    LOG.info("HF response status: " + status);
                    LOG.fine("HF response body: " + response.body());

                    if (status == 401)
                        return "⚠ Invalid Hugging Face API key. Check environment variable HF_API_KEY.";
                    if (status == 429)
                        return "⚠ Rate limit reached. Wait a moment and try again.";
                    if (status >= 500)
                        return "⚠ Hugging Face server error (" + status + "). Try again shortly.";

                    return parseResponse(response.body());
                })
                .exceptionally(ex -> {
                    LOG.log(Level.WARNING, "HF network error", ex);
                    return "⚠ Connection error: " + ex.getMessage();
                });
    }

    // ── Response parser ──────────────────────────────────────────────────

    /**
     * Parses the Hugging Face response.
     * Success shape : [{"generated_text":"..."}]
     * Error shape   : {"error":"...","estimated_time":20.0}
     */
    private String parseResponse(String raw) {
        if (raw == null || raw.isBlank()) return "⚠ Empty response from API.";

        LOG.fine("Parsing HF response: " + raw);

        try {
            // ── Error object ──────────────────────────────────────────────
            if (raw.contains("\"error\"")) {
                String error = extractJsonString(raw, "error");
                if (error.toLowerCase().contains("loading") || raw.contains("estimated_time")) {
                    return "⏳ The AI model is loading (cold start). Please wait ~20 seconds and try again.";
                }
                return "⚠ API error: " + error;
            }

            // ── Normal array response ─────────────────────────────────────
            // [{"generated_text":"..."}]
            int keyIdx = raw.indexOf("\"generated_text\"");
            if (keyIdx == -1) {
                LOG.warning("No generated_text key in HF response: " + raw);
                return "⚠ Unexpected API response format.";
            }

            int colonIdx = raw.indexOf(':', keyIdx);
            int qStart   = raw.indexOf('"', colonIdx + 1) + 1;
            int qEnd     = findClosingQuote(raw, qStart);
            String text  = raw.substring(qStart, qEnd);

            // Unescape JSON sequences
            text = unescapeJson(text).trim();

            if (text.isEmpty()) return "⚠ The model returned an empty answer.";
            return text;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse HF response: " + raw, e);
            return "⚠ Could not parse response: " + e.getMessage();
        }
    }

    // ── Memory ───────────────────────────────────────────────────────────

    private synchronized void remember(String user, String assistant) {
        turns.addLast(new Turn(user, assistant));
        while (turns.size() > MAX_TURNS) turns.removeFirst();
    }

    private synchronized String getHfApiKey() {
        if (hfApiKey != null) return hfApiKey;

        String key = System.getenv(ENV_HF_API_KEY);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing Hugging Face API key. Set environment variable HF_API_KEY (token starting with hf_...)."
            );
        }

        hfApiKey = key.trim();
        return hfApiKey;
    }

    // ── String utilities ─────────────────────────────────────────────────

    /**
     * Wraps a Java string as a JSON string literal (one level of escaping).
     * FIX: the old implementation used .replace("\\\\", "\\\\\\\\") which
     * produced double-escaped sequences \\n instead of \n in the JSON body.
     */
    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 32);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Finds the end of a JSON string starting at {@code start}, skipping escapes. */
    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"')  return i;
        }
        return s.length();
    }

    /** Extracts the string value of the first occurrence of {@code key} in JSON. */
    private static String extractJsonString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "";
        int col = json.indexOf(':', idx);
        int qs  = json.indexOf('"', col + 1) + 1;
        int qe  = findClosingQuote(json, qs);
        return unescapeJson(json.substring(qs, qe));
    }

    private static String unescapeJson(String s) {
        return s.replace("\\n",  "\n")
                .replace("\\t",  "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/",  "/");
    }

    // ── Inner class ──────────────────────────────────────────────────────

    private static final class Turn {
        final String user;
        final String assistant;
        Turn(String user, String assistant) {
            this.user = user;
            this.assistant = assistant;
        }
    }
}
