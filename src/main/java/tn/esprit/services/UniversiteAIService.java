package tn.esprit.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UniversiteAIService — Appel direct à l'API Groq.
 *
 * Aucune base de données. Pour une filière donnée, retourne
 * directement la liste des universités tunisiennes via l'IA.
 */
public class UniversiteAIService {

    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT_MS = 60_000;

    private final String apiKey;

    public UniversiteAIService() {
        this.apiKey = ConfigLoader.get("GROQ_API_KEY");
    }

    // ------------------------------------------------------------------ //
    //  Point d'entrée : retourne les universités pour une filière        //
    //  PAS de base de données — résultat direct de l'API                 //
    // ------------------------------------------------------------------ //

    /**
     * Interroge Groq et retourne directement les universités tunisiennes
     * pour la filière donnée. Chaque Map contient :
     *   nom, ville, type, description, siteWeb,
     *   fraisAnnuels, acces, tauxInsertion, diplomes
     */
    public List<Map<String, String>> getUniversitesPourFiliere(String filiereNom) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("[UniversiteAIService] Clé GROQ_API_KEY absente dans config.properties");
            return new ArrayList<>();
        }
        try {
            String requestJson = buildRequestJson(filiereNom);
            String rawResponse = postJson(requestJson);
            String content     = extractContent(rawResponse);
            return parseUniversites(content);
        } catch (Exception e) {
            System.err.println("[UniversiteAIService] Erreur pour '" + filiereNom + "': " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------ //
    //  Construction de la requête                                         //
    // ------------------------------------------------------------------ //

    private String buildRequestJson(String filiereNom) {
        String prompt = ("Liste 5 universités et facultés tunisiennes qui proposent la filière \""
                + filiereNom + "\".\n"
                + "Retourne UNIQUEMENT ce JSON valide, sans texte avant ou après :\n"
                + "{\n"
                + "  \"universites\": [\n"
                + "    {\n"
                + "      \"nom\": \"Nom de l'université ou faculté\",\n"
                + "      \"ville\": \"Ville\",\n"
                + "      \"type\": \"Public ou Privé\",\n"
                + "      \"description\": \"Courte description\",\n"
                + "      \"siteWeb\": \"https://site.tn\",\n"
                + "      \"fraisAnnuels\": 2500,\n"
                + "      \"acces\": \"Bac\",\n"
                + "      \"tauxInsertion\": 80,\n"
                + "      \"diplomes\": [\"Licence\", \"Master\"]\n"
                + "    }\n"
                + "  ]\n"
                + "}")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        return "{"
            + "\"model\":\"" + GROQ_MODEL + "\","
            + "\"temperature\":0.2,"
            + "\"max_tokens\":2000,"
            + "\"messages\":["
            +   "{\"role\":\"system\","
            +    "\"content\":\"Tu es un expert en enseignement supérieur tunisien. "
            +    "Tu connais toutes les universités et facultés de Tunisie. "
            +    "Réponds UNIQUEMENT en JSON valide.\"},"
            +   "{\"role\":\"user\",\"content\":\"" + prompt + "\"}"
            + "]}";
    }

    // ------------------------------------------------------------------ //
    //  HTTP POST                                                          //
    // ------------------------------------------------------------------ //

    private String postJson(String jsonBody) throws IOException {
        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            // Lire le message d'erreur
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errSb = new StringBuilder();
            String line;
            while ((line = errReader.readLine()) != null) errSb.append(line);
            throw new IOException("Groq API HTTP " + status + " : " + errSb);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Extraction du content depuis la réponse Groq                      //
    // ------------------------------------------------------------------ //

    private String extractContent(String groqResponse) {
        // Cherche "content":"..." dans le JSON de réponse Groq
        // (choices[0].message.content)
        int idx = groqResponse.indexOf("\"content\":");
        if (idx < 0) throw new RuntimeException("Champ 'content' introuvable dans la réponse Groq");

        int start = groqResponse.indexOf('"', idx + 10) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < groqResponse.length(); i++) {
            char c = groqResponse.charAt(i);
            if (c == '\\' && i + 1 < groqResponse.length()) {
                char next = groqResponse.charAt(i + 1);
                if      (next == '"')  { sb.append('"');  i++; }
                else if (next == 'n')  { sb.append('\n'); i++; }
                else if (next == 't')  { sb.append('\t'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else                   { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Parsing JSON minimaliste (sans dépendance externe)                //
    // ------------------------------------------------------------------ //

    public List<Map<String, String>> parseUniversites(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        // Nettoyer les balises markdown
        json = json.replaceAll("(?m)^```json\\s*", "")
                   .replaceAll("(?m)^```\\s*", "")
                   .trim();

        // Trouver le tableau [ ... ]
        int arrStart = json.indexOf('[');
        int arrEnd   = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0) return result;

        String array = json.substring(arrStart + 1, arrEnd);
        for (String obj : splitJsonObjects(array)) {
            Map<String, String> map = parseJsonObject(obj);
            if (!map.isEmpty()) result.add(map);
        }
        return result;
    }

    /** Découpe une chaîne contenant des objets JSON {...} en liste. */
    private List<String> splitJsonObjects(String text) {
        List<String> list = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if      (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    list.add(text.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return list;
    }

    /** Parse les paires clé/valeur d'un objet JSON simple. */
    private Map<String, String> parseJsonObject(String obj) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        while (i < obj.length()) {
            // Clé
            int ks = obj.indexOf('"', i); if (ks < 0) break;
            int ke = obj.indexOf('"', ks + 1); if (ke < 0) break;
            String key = obj.substring(ks + 1, ke);
            i = ke + 1;
            // ':'
            int colon = obj.indexOf(':', i); if (colon < 0) break;
            i = colon + 1;
            // Sauter espaces
            while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
            if (i >= obj.length()) break;
            char first = obj.charAt(i);
            if (first == '"') {
                // Valeur string
                StringBuilder val = new StringBuilder();
                i++;
                while (i < obj.length()) {
                    char c = obj.charAt(i);
                    if (c == '\\' && i + 1 < obj.length()) {
                        char nx = obj.charAt(i + 1);
                        if (nx == '"') val.append('"');
                        else if (nx == 'n') val.append('\n');
                        else val.append(nx);
                        i += 2;
                    } else if (c == '"') { i++; break; }
                    else { val.append(c); i++; }
                }
                map.put(key, val.toString());
            } else if (first == '[') {
                // Tableau → stocker tel quel
                int depth = 0, start = i;
                while (i < obj.length()) {
                    if (obj.charAt(i) == '[') depth++;
                    else if (obj.charAt(i) == ']') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                // Nettoyer : ["Licence","Master"] → Licence, Master
                String raw = obj.substring(start, i);
                raw = raw.replaceAll("[\\[\\]\"]", "").replace(",", ", ").trim();
                map.put(key, raw);
            } else if (first == '{') {
                // Objet imbriqué → ignorer
                int depth = 0, start = i;
                while (i < obj.length()) {
                    if (obj.charAt(i) == '{') depth++;
                    else if (obj.charAt(i) == '}') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                map.put(key, obj.substring(start, i));
            } else {
                // Nombre ou booléen
                int end = i;
                while (end < obj.length()
                       && obj.charAt(end) != ','
                       && obj.charAt(end) != '\n'
                       && obj.charAt(end) != '}') end++;
                map.put(key, obj.substring(i, end).trim());
                i = end;
            }
        }
        return map;
    }
}
