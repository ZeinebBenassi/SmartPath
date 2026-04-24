package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatbotService — SmartPath AI intelligent et context-aware.
 *
 * Fonctionnalités :
 *  - Mémoire conversationnelle (MAX_TURNS tours)
 *  - Détection des demandes de clarification ("explique plus", "je comprends pas"...)
 *    -> continue le dernier sujet au lieu de traiter comme nouvelle question
 *  - Prompt systeme riche et pedagogique (tuteur CS)
 *  - Specialise CS uniquement (redirige les hors-sujets)
 *  - Bilingue FR / EN, adaptatif au niveau de l'utilisateur
 *
 * Modele : mistralai/Mistral-7B-Instruct-v0.2 (Inference Providers)
 * Auth   : config.properties -> hf.api_key=hf_...
 */
public class ChatbotService {

    private static final Logger LOG = Logger.getLogger(ChatbotService.class.getName());

    // Endpoint OpenAI-compatible (selection provider "auto" cote serveur)
    private static final String HF_API_URL = "https://router.huggingface.co/v1/chat/completions";

    // Override possible via config.properties: hf.model=... (ou env: HF_MODEL)
    private static final String DEFAULT_MODEL = "Qwen/Qwen2.5-72B-Instruct";

    private static final int MAX_TURNS = 6;

    // ── Triggers de clarification (HashSet = pas de doublon possible) ────────
    // Note : toutes les apostrophes sont des apostrophes droites ASCII (')
    // La normalisation dans isClarificationRequest() gere les variantes Unicode
    private static final Set<String> CLARIFICATION_TRIGGERS;
    static {
        Set<String> s = new HashSet<>(Arrays.asList(
            "explique plus",
            "explique encore",
            "plus de details",
            "plus de detail",
            "je comprends pas",
            "je comprends toujours pas",
            "je ne comprends pas",
            "j'ai pas compris",
            "pas compris",
            "j'ai pas compris",
            "je n'ai pas compris",
            "plus simple",
            "simplifie",
            "simplifie moi",
            "simplifie-moi",
            "reformule",
            "c'est quoi exactement",
            "c'est quoi",
            "c est quoi",
            "donne un exemple",
            "donne des exemples",
            "exemple concret",
            "continue",
            "continue l'explication",
            "et alors",
            "et ensuite",
            "more details",
            "explain more",
            "i don't understand",
            "i dont understand",
            "simplify",
            "give an example",
            "go on",
            "continue please",
            "vas-y",
            "vas y",
            "je vois pas",
            "je vois toujours pas",
            "encore",
            "repete",
            "repete moi ca",
            "repete ca"
        ));
        CLARIFICATION_TRIGGERS = Collections.unmodifiableSet(s);
    }

    private final HttpClient  httpClient;
    private final Deque<Turn> turns     = new ArrayDeque<>();
    private       String      cachedApiKey;
    private       String      lastTopic = null;

    public ChatbotService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ── API publique ─────────────────────────────────────────────────────────

    public CompletableFuture<String> askAsync(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return CompletableFuture.completedFuture("");

        final String apiKey;
        try {
            apiKey = resolveApiKey();
        } catch (IllegalStateException e) {
            LOG.warning("HF API key not configured: " + e.getMessage());
            return CompletableFuture.completedFuture(
                    "Cle API Hugging Face manquante.\n"
                    + "Ouvre config.properties et remplace :\n"
                    + "  hf.api_key=hf_REMPLACE_PAR_TON_TOKEN\n"
                    + "par ton vrai token (gratuit sur huggingface.co/settings/tokens)");
        }

        String  trimmed      = userMessage.trim();
        boolean isClarif     = isClarificationRequest(trimmed);
        String  effectiveMsg = (isClarif && lastTopic != null)
                               ? buildClarificationMessage(trimmed)
                               : trimmed;

        LOG.info("Message effectif -> " + effectiveMsg);
        return callHuggingFace(effectiveMsg, apiKey)
                .thenApply(answer -> {
                    remember(trimmed, answer);
                    if (!isClarif) lastTopic = trimmed;
                    return answer;
                });
    }

    // ── Detection de clarification ───────────────────────────────────────────

    private boolean isClarificationRequest(String msg) {
        // Normaliser : minuscules + apostrophes courbes -> droites
        String lower = normalize(msg);

        // Correspondance exacte
        if (CLARIFICATION_TRIGGERS.contains(lower)) return true;

        // Correspondance partielle (commence par un trigger)
        for (String trigger : CLARIFICATION_TRIGGERS) {
            if (lower.startsWith(trigger + " ") || lower.equals(trigger)) return true;
        }

        // Message tres court (1-2 mots) avec un sujet actif = probablement clarification
        if (lastTopic != null && lower.split("\\s+").length <= 2 && !lower.isEmpty())
            return true;

        return false;
    }

    /**
     * Normalise une chaine : minuscules + apostrophes typographiques -> ASCII.
     */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replace('\u2019', '\'')  // apostrophe courbe droite '
                .replace('\u2018', '\'')  // apostrophe courbe gauche '
                .replace('\u02BC', '\'')  // modificateur lettre apostrophe
                .trim();
    }

    private String buildClarificationMessage(String userMsg) {
        return "A propos de \"" + lastTopic + "\" : " + userMsg
               + ". Continue l'explication du meme sujet avec plus de details,"
               + " ne change pas de sujet.";
    }

    // ── Construction du prompt ───────────────────────────────────────────────

    private static String systemPrompt() {
        return
            "Tu es SmartPath AI, un assistant intelligent et pedagogique integre dans une application " +
            "pour etudiants en informatique a Esprit School of Engineering (Tunisie).\n\n" +

            "## Role\n" +
            "Tu es un tuteur expert en computer science. Tu expliques, guides et aides.\n\n" +

            "## Domaines autorises UNIQUEMENT\n" +
            "- Programmation (Java, Python, C, C++, JavaScript, etc.)\n" +
            "- Algorithmes et structures de donnees\n" +
            "- Bases de donnees (SQL, NoSQL, modelisation)\n" +
            "- Reseaux informatiques\n" +
            "- Intelligence Artificielle et Machine Learning\n" +
            "- Big Data (Hadoop, Spark, etc.)\n" +
            "- Cybersecurite\n" +
            "- Genie logiciel (MVC, Design Patterns, etc.)\n\n" +

            "## Regles de comportement\n" +
            "1. CONTEXTE : Tu gardes le contexte de toute la conversation.\n" +
            "2. CLARIFICATION : Si l'utilisateur dit 'explique plus', 'je comprends pas', " +
               "'plus simple', 'donne un exemple' -> tu CONTINUES le meme sujet avec plus de details. " +
               "Tu ne changes JAMAIS de sujet sur une demande de clarification.\n" +
            "3. HORS SUJET : Si la question n'est pas liee a l'informatique, redirige poliment.\n" +
            "4. SALUTATIONS : Reponds chaleureusement et propose de l'aide en informatique.\n" +
            "5. PEDAGOGIE : Adapte ton niveau. Commence simple, puis approfondis.\n\n" +

            "## Style de reponse\n" +
            "- Langue de l'utilisateur (francais ou anglais)\n" +
            "- Concis et clair, pas de blabla\n" +
            "- Exemples concrets\n" +
            "- Structure avec points ou etapes si necessaire\n" +
            "- Maximum 80 mots. Sois ultra-concis et direct.\n" +
            "- Pas d'introduction, pas de conclusion, pas de blabla.\n" +
            "- Va droit au but : reponds uniquement a ce qui est demande.\n" +
            "- Naturel, comme un tuteur humain";
    }

        private synchronized String buildMessagesJson(String userMessage) {
                StringBuilder sb = new StringBuilder(1024);
                sb.append('[');

                // system
                sb.append("{\"role\":\"system\",\"content\":")
                    .append(jsonString(systemPrompt()))
                    .append('}');

                // history (user/assistant)
                for (Turn t : turns) {
                        sb.append(',')
                            .append("{\"role\":\"user\",\"content\":")
                            .append(jsonString(t.user))
                            .append('}');
                        sb.append(',')
                            .append("{\"role\":\"assistant\",\"content\":")
                            .append(jsonString(t.assistant))
                            .append('}');
                }

                // current user
                sb.append(',')
                    .append("{\"role\":\"user\",\"content\":")
                    .append(jsonString(userMessage))
                    .append('}');

                sb.append(']');
                return sb.toString();
        }

        private synchronized String resolveModelId() {
                String model = ConfigLoader.get("hf.model");
                if (model == null || model.isBlank()) model = System.getenv("HF_MODEL");
                if (model == null || model.isBlank()) model = DEFAULT_MODEL;
                return model.trim();
        }

    // ── Appel HTTP ───────────────────────────────────────────────────────────

    private CompletableFuture<String> callHuggingFace(String userMessage, String apiKey) {

        String model = resolveModelId();
        String jsonBody = "{"
            + "\"model\":" + jsonString(model) + ","
            + "\"stream\":false,"
            + "\"temperature\":0.6,"
            + "\"top_p\":0.9,"
            + "\"max_tokens\":256,"
            + "\"messages\":" + buildMessagesJson(userMessage)
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    LOG.info("HF status: " + status);
                    if (status != 200) {
                        String body = response.body();
                        if (body != null && !body.isBlank()) {
                            String snippet = body.replaceAll("\\s+", " ").trim();
                            if (snippet.length() > 350) snippet = snippet.substring(0, 350) + "…";
                            LOG.info("HF body (snippet): " + snippet);
                        }
                    }
                    return switch (status) {
                        case 401 -> "Cle API invalide. Verifie hf.api_key dans config.properties.";
                        case 403 -> "Acces refuse. Verifie ton token et/ou les conditions du modele sur Hugging Face.";
                        case 400 -> "Requete invalide (400). " + extractBadRequestMessage(response.body());
                        case 404 -> "Endpoint/model introuvable (404). Verifie l'URL d'inference et le nom du modele.";
                        case 429 -> "Trop de requetes. Attends quelques secondes et reessaie.";
                        default  -> status >= 500
                                ? "Serveur Hugging Face indisponible (" + status + "). Reessaie dans un moment."
                                : parseResponse(response.body());
                    };
                })
                .exceptionally(ex -> {
                    LOG.log(Level.WARNING, "HF network error", ex);
                    return "Erreur reseau : " + ex.getMessage();
                });
    }

    private String extractBadRequestMessage(String raw) {
        if (raw == null || raw.isBlank()) return "Aucun detail fourni par l'API.";
        try {
            Object parsed = new JSONTokener(raw).nextValue();
            if (parsed instanceof JSONObject obj) {
                if (obj.has("error")) {
                    Object err = obj.get("error");
                    if (err instanceof JSONObject eObj) {
                        String msg = eObj.optString("message", "");
                        if (msg != null && !msg.isBlank()) return msg;
                    }
                    String msg = String.valueOf(err);
                    if (msg != null && !msg.isBlank()) return msg;
                }
                if (obj.has("message")) {
                    String msg = obj.optString("message", "");
                    if (msg != null && !msg.isBlank()) return msg;
                }
            }
        } catch (Exception ignored) {}

        String snippet = raw.replaceAll("\\s+", " ").trim();
        if (snippet.length() > 180) snippet = snippet.substring(0, 180) + "…";
        return snippet;
    }

    // ── Parsing reponse ──────────────────────────────────────────────────────

    private String parseResponse(String raw) {
        if (raw == null || raw.isBlank()) return "Reponse vide de l'API.";

        try {
            Object parsed = new JSONTokener(raw).nextValue();

            if (parsed instanceof JSONObject obj) {
                if (obj.has("error")) {
                    Object err = obj.get("error");
                    String msg = (err instanceof JSONObject eObj)
                            ? eObj.optString("message", eObj.toString())
                            : String.valueOf(err);
                    if (msg.toLowerCase().contains("loading") || raw.contains("estimated_time"))
                        return "Le modele demarre (cold start). Reessaie dans ~20 secondes.";
                    return "Erreur API : " + msg;
                }

                // OpenAI-compatible chat completions
                JSONArray choices = obj.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject c0 = choices.optJSONObject(0);
                    if (c0 != null) {
                        JSONObject message = c0.optJSONObject("message");
                        if (message != null) {
                            String content = message.optString("content", "").trim();
                            if (!content.isEmpty()) return content;
                        }
                        // fallback (some providers may return a flat "text")
                        String text = c0.optString("text", "").trim();
                        if (!text.isEmpty()) return text;
                    }
                }

                // Text-generation style (generated_text)
                String gen = obj.optString("generated_text", "").trim();
                if (!gen.isEmpty()) return gen;
            }

            // HF legacy: array response
            if (parsed instanceof JSONArray arr && arr.length() > 0) {
                JSONObject first = arr.optJSONObject(0);
                if (first != null) {
                    String gen = first.optString("generated_text", "").trim();
                    if (!gen.isEmpty()) return gen;
                }
            }

            LOG.warning("Format de reponse inattendu: " + raw);
            return "Format de reponse inattendu.";

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Echec parsing: " + raw, e);
            return "Impossible de parser la reponse : " + e.getMessage();
        }
    }

    // ── Memoire ──────────────────────────────────────────────────────────────

    private synchronized void remember(String user, String assistant) {
        turns.addLast(new Turn(user, assistant));
        while (turns.size() > MAX_TURNS) turns.removeFirst();
    }

    // ── Resolution cle API ───────────────────────────────────────────────────

    private synchronized String resolveApiKey() {
        if (cachedApiKey != null) return cachedApiKey;

        String key = ConfigLoader.get("hf.api_key");
        if (key == null || key.isBlank() || key.startsWith("hf_REMPLACE"))
            key = System.getenv("HF_API_KEY");

        if (key == null || key.isBlank() || key.startsWith("hf_REMPLACE"))
            throw new IllegalStateException(
                    "Cle HF manquante -> config.properties : hf.api_key=hf_...");

        cachedApiKey = key.trim();
        return cachedApiKey;
    }

    // ── Utilitaires JSON ─────────────────────────────────────────────────────

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 32).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> { if (c < 0x20) sb.append(String.format("\\u%04x",(int)c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '\\') { i++; continue; }
            if (s.charAt(i) == '"')  return i;
        }
        return s.length();
    }

    private static String extractJsonString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "";
        int col = json.indexOf(':', idx);
        int qs  = json.indexOf('"', col + 1) + 1;
        return unescapeJson(json.substring(qs, findClosingQuote(json, qs)));
    }

    private static String unescapeJson(String s) {
        return s.replace("\\n",  "\n")
                .replace("\\t",  "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/",  "/");
    }

    // ── Inner class ──────────────────────────────────────────────────────────

    private static final class Turn {
        final String user, assistant;
        Turn(String u, String a) { user = u; assistant = a; }
    }
}
