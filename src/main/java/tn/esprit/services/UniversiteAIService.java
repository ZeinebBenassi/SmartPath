package tn.esprit.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════════
 *  UniversiteAIService — Deux APIs dans un seul service
 *
 *  Les deux URLs sont configurées dans config.properties :
 *
 *  GROQ_API_KEY=gsk_...
 *  NOMINATIM_URL=https://nominatim.openstreetmap.org/search
 *
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  API 1 — GROQ (LLM llama-3.3-70b-versatile)                    │
 *  │  URL    : GROQ_URL (fixe)                                       │
 *  │  Auth   : Bearer GROQ_API_KEY (depuis config.properties)        │
 *  │  Usage  : générer la liste des universités pour une filière     │
 *  ├─────────────────────────────────────────────────────────────────┤
 *  │  API 2 — NOMINATIM (OpenStreetMap Geocoding)                    │
 *  │  URL    : NOMINATIM_URL (depuis config.properties)              │
 *  │  Auth   : aucune (API publique et gratuite)                     │
 *  │  Usage  : convertir nom université → latitude / longitude       │
 *  └─────────────────────────────────────────────────────────────────┘
 * ══════════════════════════════════════════════════════════════════════
 */
public class UniversiteAIService {

    // ── API 1 : Groq ────────────────────────────────────────────────────
    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT_MS = 60_000;

    // ── API 2 : Nominatim ───────────────────────────────────────────────
    private static final String NOMINATIM_UA = "SmartPathApp/1.0 (contact@smartpath.tn)";

    // ── Clés/URLs lues depuis config.properties ─────────────────────────
    private final String groqApiKey;    // GROQ_API_KEY  → utilisée par ReleveAnalyserService
    private final String groqApiKey2;   // GROQ_API_KEY_2 → utilisée par UniversiteAIService
    private final String nominatimUrl;  // NOMINATIM_URL

    public UniversiteAIService() {
        this.groqApiKey   = ConfigLoader.get("GROQ_API_KEY");
        this.groqApiKey2  = ConfigLoader.get("GROQ_API_KEY_2");
        this.nominatimUrl = ConfigLoader.get("NOMINATIM_URL");
        System.out.println("[Config] GROQ_API_KEY   = " + (groqApiKey   != null ? "✅ chargée" : "❌ manquante"));
        System.out.println("[Config] GROQ_API_KEY_2 = " + (groqApiKey2  != null ? "✅ chargée" : "❌ manquante"));
        System.out.println("[Config] NOMINATIM_URL  = " + (nominatimUrl != null ? nominatimUrl : "❌ manquante"));
    }

    /** Retourne la clé active pour UniversiteAIService.
     *  Priorité : GROQ_API_KEY_2, sinon GROQ_API_KEY comme fallback. */
    private String getActiveKey() {
        if (groqApiKey2 != null && !groqApiKey2.isEmpty()
                && !groqApiKey2.startsWith("VOTRE")) return groqApiKey2;
        return groqApiKey;
    }

    // ════════════════════════════════════════════════════════════════════
    //  API 1 : GROQ — Liste des universités pour une filière
    // ════════════════════════════════════════════════════════════════════

    public List<Map<String, String>> getUniversitesPourFiliere(String filiereNom) {
        if (getActiveKey() == null || getActiveKey().isEmpty()) {
            System.err.println("[API 1 Groq] Aucune clé valide dans config.properties");
            return new ArrayList<>();
        }
        try {
            System.out.println("[API 1 Groq] Recherche universités pour : " + filiereNom);
            String requestJson = buildGroqRequest(filiereNom);
            String rawResponse = groqPost(requestJson);
            String content     = extractGroqContent(rawResponse);
            List<Map<String, String>> result = parseUniversites(content);
            System.out.println("[API 1 Groq] " + result.size() + " université(s) trouvée(s)");
            return result;
        } catch (Exception e) {
            System.err.println("[API 1 Groq] Erreur : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String buildGroqRequest(String filiereNom) {
        String prompt = ("Liste 5 universités et facultés tunisiennes pour la filière \""
                + filiereNom + "\".\n"
                + "Retourne UNIQUEMENT ce JSON valide :\n"
                + "{\"universites\":[{"
                + "\"nom\":\"...\",\"ville\":\"...\",\"adresse\":\"...\","
                + "\"type\":\"Public ou Privé\",\"description\":\"...\","
                + "\"siteWeb\":\"https://...\",\"fraisAnnuels\":2500,"
                + "\"acces\":\"Bac\",\"tauxInsertion\":80,"
                + "\"diplomes\":[\"Licence\",\"Master\"]"
                + "}]}")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        return "{"
            + "\"model\":\"" + GROQ_MODEL + "\","
            + "\"temperature\":0.2,"
            + "\"max_tokens\":2000,"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":\"Tu es un expert en enseignement supérieur"
            +    " tunisien. Réponds UNIQUEMENT en JSON valide.\"},"
            +   "{\"role\":\"user\",\"content\":\"" + prompt + "\"}"
            + "]}";
    }

    private String groqPost(String jsonBody) throws IOException {
        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + getActiveKey());
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        if (status != 200) {
            BufferedReader err = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sb.append(line);
            throw new IOException("[API 1 Groq] HTTP " + status + " : " + sb);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String extractGroqContent(String groqResponse) {
        int idx = groqResponse.indexOf("\"content\":");
        if (idx < 0) throw new RuntimeException("Champ 'content' introuvable");
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

    // ════════════════════════════════════════════════════════════════════
    //  API 2 : NOMINATIM — Géocodage (nom université → lat/lon)
    //  URL lue depuis config.properties : NOMINATIM_URL
    // ════════════════════════════════════════════════════════════════════

    /** Résultat du géocodage Nominatim */
    public static class GeoResult {
        public final double lat;
        public final double lon;
        public final String displayName;

        public GeoResult(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }

        public boolean isValid() { return lat != 0 && lon != 0; }
    }

    /**
     * Appelle l'API Nominatim pour convertir un nom d'université
     * en coordonnées GPS (latitude / longitude).
     *
     * URL lue depuis config.properties → NOMINATIM_URL
     */
    public GeoResult geocoderUniversite(String query) {
        if (nominatimUrl == null || nominatimUrl.isEmpty()) {
            System.err.println("[API 2 Nominatim] NOMINATIM_URL absente dans config.properties");
            return null;
        }
        System.out.println("[API 2 Nominatim] Géocodage : " + query);

        // Tentative 1 : query exacte + Tunisie
        GeoResult r = nominatimGet(query, "tn");
        if (r != null && r.isValid()) {
            System.out.println("[API 2 Nominatim] Trouvé (TN) → " + r.lat + ", " + r.lon);
            return r;
        }

        // Tentative 2 : simplifier le nom (prendre les 3 premiers mots)
        String[] mots = query.split(" ");
        if (mots.length > 3) {
            String querySimple = mots[0] + " " + mots[1] + " " + mots[2] + " Tunisie";
            r = nominatimGet(querySimple, "tn");
            if (r != null && r.isValid()) {
                System.out.println("[API 2 Nominatim] Trouvé (simplifié) → " + r.lat + ", " + r.lon);
                return r;
            }
        }

        // Tentative 3 : juste la ville
        String ville = mots[mots.length - 2]; // avant-dernier mot = ville
        r = nominatimGet(ville + " Tunisie", "tn");
        if (r != null && r.isValid()) {
            System.out.println("[API 2 Nominatim] Trouvé (ville) → " + r.lat + ", " + r.lon);
            return r;
        }

        // Tentative 4 : sans restriction pays
        r = nominatimGet(query, null);
        if (r != null && r.isValid()) {
            System.out.println("[API 2 Nominatim] Trouvé (global) → " + r.lat + ", " + r.lon);
            return r;
        }

        System.err.println("[API 2 Nominatim] Introuvable : " + query);
        return null;
    }

    private GeoResult nominatimGet(String query, String countryCode) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // Construction de l'URL depuis config.properties
            String urlStr = nominatimUrl
                    + "?q=" + encoded
                    + "&format=json&limit=1&accept-language=fr"
                    + (countryCode != null ? "&countrycodes=" + countryCode : "");

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",      NOMINATIM_UA);
            conn.setRequestProperty("Accept-Language", "fr");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String body = sb.toString().trim();
            if (body.equals("[]") || body.isEmpty()) return null;

            double lat = extraireDouble(body, "\"lat\"");
            double lon = extraireDouble(body, "\"lon\"");
            String displayName = extraireString(body, "\"display_name\"");

            if (lat == 0 && lon == 0) return null;
            return new GeoResult(lat, lon, displayName);

        } catch (Exception e) {
            System.err.println("[API 2 Nominatim] Erreur HTTP : " + e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Parsing JSON (Groq + Nominatim)
    // ════════════════════════════════════════════════════════════════════

    public List<Map<String, String>> parseUniversites(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        json = json.replaceAll("(?m)^```json\\s*", "")
                   .replaceAll("(?m)^```\\s*", "")
                   .trim();
        int arrStart = json.indexOf('[');
        int arrEnd   = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0) return result;
        for (String obj : splitJsonObjects(json.substring(arrStart + 1, arrEnd))) {
            Map<String, String> map = parseJsonObject(obj);
            if (!map.isEmpty()) result.add(map);
        }
        return result;
    }

    private double extraireDouble(String json, String key) {
        try {
            int idx = json.indexOf(key); if (idx == -1) return 0;
            int s = json.indexOf('"', idx + key.length() + 1) + 1;
            int e = json.indexOf('"', s);
            return Double.parseDouble(json.substring(s, e).trim());
        } catch (Exception e) { return 0; }
    }

    private String extraireString(String json, String key) {
        try {
            int idx = json.indexOf(key); if (idx == -1) return "";
            int s = json.indexOf('"', idx + key.length() + 1) + 1;
            int e = json.indexOf('"', s);
            return json.substring(s, e);
        } catch (Exception e) { return ""; }
    }

    private List<String> splitJsonObjects(String text) {
        List<String> list = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if      (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) { list.add(text.substring(start, i + 1)); start = -1; }
            }
        }
        return list;
    }

    private Map<String, String> parseJsonObject(String obj) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        while (i < obj.length()) {
            int ks = obj.indexOf('"', i); if (ks < 0) break;
            int ke = obj.indexOf('"', ks + 1); if (ke < 0) break;
            String key = obj.substring(ks + 1, ke);
            i = ke + 1;
            int colon = obj.indexOf(':', i); if (colon < 0) break;
            i = colon + 1;
            while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
            if (i >= obj.length()) break;
            char first = obj.charAt(i);
            if (first == '"') {
                StringBuilder val = new StringBuilder(); i++;
                while (i < obj.length()) {
                    char c = obj.charAt(i);
                    if (c == '\\' && i + 1 < obj.length()) {
                        char nx = obj.charAt(i + 1);
                        if (nx == '"') val.append('"'); else if (nx == 'n') val.append('\n'); else val.append(nx);
                        i += 2;
                    } else if (c == '"') { i++; break; } else { val.append(c); i++; }
                }
                map.put(key, val.toString());
            } else if (first == '[') {
                int depth = 0, start = i;
                while (i < obj.length()) {
                    if (obj.charAt(i) == '[') depth++;
                    else if (obj.charAt(i) == ']') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                map.put(key, obj.substring(start, i).replaceAll("[\\[\\]\"]", "").replace(",", ", ").trim());
            } else if (first == '{') {
                int depth = 0, start = i;
                while (i < obj.length()) {
                    if (obj.charAt(i) == '{') depth++;
                    else if (obj.charAt(i) == '}') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                map.put(key, obj.substring(start, i));
            } else {
                int end = i;
                while (end < obj.length() && obj.charAt(end) != ',' && obj.charAt(end) != '\n' && obj.charAt(end) != '}') end++;
                map.put(key, obj.substring(i, end).trim());
                i = end;
            }
        }
        return map;
    }
}
