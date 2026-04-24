package tn.esprit.services;

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
 * Modele : mistralai/Mistral-7B-Instruct-v0.2 (gratuit, stable, FR+EN)
 * Auth   : config.properties -> hf.api_key=hf_...
 */
public class ChatbotService {

    private static final Logger LOG = Logger.getLogger(ChatbotService.class.getName());

    private static final String HF_API_URL =
            "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";

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
        String prompt = buildPrompt(effectiveMsg);

        return callHuggingFace(prompt, apiKey)
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

    private synchronized String buildPrompt(String userMessage) {

        String system =
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
            "- Maximum 300 mots sauf si demande approfondie\n" +
            "- Naturel, comme un tuteur humain";

        StringBuilder sb = new StringBuilder();

        if (turns.isEmpty()) {
            sb.append("<s>[INST] ").append(system)
              .append("\n\n").append(userMessage).append(" [/INST]");
        } else {
            sb.append("<s>[INST] ").append(system).append(" [/INST] ")
              .append("Compris ! Je suis SmartPath AI. Je garde le contexte et continue")
              .append(" toujours le meme sujet si tu demandes plus d'explications.</s>\n");

            for (Turn t : turns) {
                sb.append("[INST] ").append(t.user).append(" [/INST] ")
                  .append(t.assistant).append("</s>\n");
            }
            sb.append("[INST] ").append(userMessage).append(" [/INST]");
        }

        return sb.toString();
    }

    // ── Appel HTTP ───────────────────────────────────────────────────────────

    private CompletableFuture<String> callHuggingFace(String prompt, String apiKey) {

        String jsonBody = "{"
                + "\"inputs\":" + jsonString(prompt) + ","
                + "\"parameters\":{"
                + "\"max_new_tokens\":512,"
                + "\"temperature\":0.6,"
                + "\"top_p\":0.9,"
                + "\"do_sample\":true,"
                + "\"return_full_text\":false"
                + "},"
                + "\"options\":{"
                + "\"wait_for_model\":true,"
                + "\"use_cache\":false"
                + "}"
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
                    LOG.fine("HF body: " + response.body());
                    return switch (status) {
                        case 401 -> "Cle API invalide. Verifie hf.api_key dans config.properties.";
                        case 403 -> "Acces refuse. Va sur huggingface.co/mistralai/Mistral-7B-Instruct-v0.2 et accepte les conditions.";
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

    // ── Parsing reponse ──────────────────────────────────────────────────────

    private String parseResponse(String raw) {
        if (raw == null || raw.isBlank()) return "Reponse vide de l'API.";

        try {
            if (raw.contains("\"error\"")) {
                String error = extractJsonString(raw, "error");
                if (error.toLowerCase().contains("loading") || raw.contains("estimated_time"))
                    return "Le modele demarre (cold start). Reessaie dans ~20 secondes.";
                return "Erreur API : " + error;
            }

            int keyIdx = raw.indexOf("\"generated_text\"");
            if (keyIdx == -1) {
                LOG.warning("Pas de generated_text dans: " + raw);
                return "Format de reponse inattendu.";
            }

            int colonIdx = raw.indexOf(':', keyIdx);
            int qStart   = raw.indexOf('"', colonIdx + 1) + 1;
            int qEnd     = findClosingQuote(raw, qStart);
            String text  = unescapeJson(raw.substring(qStart, qEnd)).trim();

            // Nettoyer les artefacts Mistral residuels
            text = text.replaceAll("(?s)<s>|</s>|\\[INST]|\\[/INST]", "").trim();

            return text.isEmpty() ? "Le modele a retourne une reponse vide. Reessaie." : text;

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
