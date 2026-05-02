package tn.esprit.services;

import tn.esprit.utils.MyDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

/**
 * Service d'analyse IA du relevé de notes.
 * Équivalent du ReleveAnalyserService Symfony.
 *
 * Utilise l'API Groq (llama-3.3-70b) pour :
 *   1. Extraire le texte d'une image via vision (llama-4-scout)
 *   2. Analyser le relevé et recommander une filière
 *
 * ⚠️  Remplacez GROQ_API_KEY par votre clé réelle, ou définissez
 *     la variable d'environnement GROQ_API_KEY.
 */
public class ReleveAnalyserService {

    // ── Config Groq ────────────────────────────────────────────────────────
    private static final String GROQ_URL        = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL_TEXT = "llama-3.3-70b-versatile";
    private static final String GROQ_MODEL_VIS  = "llama-3.2-11b-vision-preview";

    // Clé chargée de façon lazy (évite le crash au démarrage si config absente)
    private String groqApiKey = null;

    private String getGroqApiKey() {
        if (groqApiKey != null) return groqApiKey;
        // 1. Variable d'environnement système
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) { groqApiKey = envKey; return groqApiKey; }
        // 2. Lire depuis config.properties à la racine du projet
        String[] configPaths = {
            "config.properties",
            System.getProperty("user.dir") + java.io.File.separator + "config.properties",
            new java.io.File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath())
                .getParentFile().getParentFile().getAbsolutePath() + java.io.File.separator + "config.properties"
        };
        for (String path : configPaths) {
            try {
                java.io.File configFile = new java.io.File(path);
                if (configFile.exists()) {
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                        props.load(fis);
                    }
                    String val = props.getProperty("GROQ_API_KEY");
                    if (val != null && !val.isBlank()) { groqApiKey = val.trim(); return groqApiKey; }
                }
            } catch (Exception ignored) {}
        }
        throw new RuntimeException(
            "Clé GROQ_API_KEY introuvable !\n" +
            "Vérifiez que config.properties à la racine du projet contient GROQ_API_KEY=gsk_...");
    }

    private final HttpClient http       = HttpClient.newHttpClient();
    private final Connection connection = MyDatabase.getInstance().getConnection();

    // ══════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE : analyser un fichier
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Extrait le texte du fichier puis l'analyse avec l'IA.
     * @return Map contenant : notesDetectees, moyenneGenerale, pointsForts,
     *                         pointsFaibles, scoreParFiliere, filiereRecommandee, conseil
     */
    // Mots-clés qui doivent apparaître dans un vrai relevé de notes
    private static final List<String> MOTS_CLES_RELEVE = List.of(
        "note", "notes", "matière", "matiere", "moyenne", "résultat", "resultat",
        "examen", "semestre", "coefficient", "coeff", "module", "filière", "filiere",
        "étudiant", "etudiant", "université", "universite", "année", "annee",
        "bac", "lycée", "lycee", "relevé", "releve", "bulletin", "session",
        "mention", "admis", "ajourné", "note/20", "/20", "trimestre"
    );

    public Map<String, Object> analyserFichier(File fichier, String fileType) throws Exception {
        String texte = extraireTexte(fichier, fileType);
        if (texte == null || texte.isBlank())
            throw new RuntimeException("Impossible d'extraire le texte du fichier.");
        // ── Validation : vérifier que c'est bien un relevé de notes ──
        validerReleveDeNotes(texte);
        return analyserTexte(texte);
    }

    /**
     * Vérifie que le texte extrait ressemble à un relevé de notes.
     * Lève une exception claire si ce n'est pas le cas.
     */
    private void validerReleveDeNotes(String texte) throws Exception {
        String texteLower = texte.toLowerCase();
        long nbMotsCles = MOTS_CLES_RELEVE.stream()
                .filter(texteLower::contains)
                .count();
        // Si moins de 2 mots-clés trouvés → probablement pas un relevé
        if (nbMotsCles < 2) {
            throw new RuntimeException(
                "❌ Le fichier uploadé ne semble pas être un relevé de notes.\n\n" +
                "Veuillez uploader un document contenant vos notes académiques\n" +
                "(bulletin scolaire, relevé universitaire, etc.).\n\n" +
                "Format accepté : PDF, JPG, PNG, WEBP"
            );
        }
    }

    public String extraireTexte(File fichier, String fileType) throws Exception {
        if ("pdf".equalsIgnoreCase(fileType)) {
            return extraireTextePDF(fichier);
        } else {
            byte[] bytes = Files.readAllBytes(fichier.toPath());
            return extraireTexteImage(bytes, detectMimeType(fichier));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EXTRACTION TEXTE
    // ══════════════════════════════════════════════════════════════════════

    /** Extraction depuis image via Groq vision. */
    private String extraireTexteImage(byte[] imageBytes, String mimeType) throws Exception {
        byte[] finalBytes = imageBytes;
        String finalMime = mimeType;
        
        // Toujours essayer de convertir en JPEG et redimensionner si nécessaire pour Groq
        // Groq préfère le JPEG et a des limites de taille (4MB base64 ~= 3MB raw)
        if (imageBytes.length > 2 * 1024 * 1024 || !"image/jpeg".equals(mimeType)) {
            finalBytes = prepareImageForGroq(imageBytes);
            finalMime = "image/jpeg";
        }

        String base64 = Base64.getEncoder().encodeToString(finalBytes);
        String dataUrl = "data:" + finalMime + ";base64," + base64;

        JSONObject body = new JSONObject();
        body.put("model", GROQ_MODEL_VIS);
        body.put("max_tokens", 2048);
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        
        JSONArray content = new JSONArray();
        
        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", "Extrais intégralement toutes les notes et matières visibles sur ce relevé de notes. Retourne uniquement le texte brut.");
        content.put(textContent);
        
        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", dataUrl);
        imageContent.put("image_url", imageUrl);
        content.put(imageContent);
        
        message.put("content", content);
        messages.put(message);
        body.put("messages", messages);

        return callGroq(body.toString());
    }

    private byte[] prepareImageForGroq(byte[] originalData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalData)) {
            BufferedImage original = ImageIO.read(bais);
            if (original == null) return originalData;

            int width = original.getWidth();
            int height = original.getHeight();
            int maxDimension = 1600;

            if (width > maxDimension || height > maxDimension || original.getType() != BufferedImage.TYPE_INT_RGB) {
                double ratio = Math.min((double) maxDimension / width, (double) maxDimension / height);
                if (ratio > 1.0) ratio = 1.0;
                
                int newWidth = (int) (width * ratio);
                int newHeight = (int) (height * ratio);

                BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                // Fond blanc pour les PNG transparents
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, newWidth, newHeight);
                g.drawImage(original, 0, 0, newWidth, newHeight, null);
                g.dispose();
                
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(resized, "jpg", baos);
                    return baos.toByteArray();
                }
            } else {
                // Pas besoin de redimensionner, mais on convertit quand même en JPEG si ce n'était pas le cas
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(original, "jpg", baos);
                    return baos.toByteArray();
                }
            }
        }
    }

    /** Extraction depuis PDF via PDFBox. */
    private String extraireTextePDF(File fichier) throws Exception {
        try (PDDocument document = PDDocument.load(fichier)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texte = stripper.getText(document).trim();

            // Si le PDF est un scan (peu de texte), on rend la première page en image
            if (texte.length() < 100 && document.getNumberOfPages() > 0) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image = renderer.renderImageWithDPI(0, 150); // 150 DPI suffisant pour OCR
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "jpg", baos);
                    return extraireTexteImage(baos.toByteArray(), "image/jpeg");
                }
            }
            return texte;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANALYSE IA
    // ══════════════════════════════════════════════════════════════════════

    public Map<String, Object> analyserTexte(String texteReleve) throws Exception {
        List<Map<String, Object>> matieres = chargerMatieres();
        List<Map<String, Object>> filieres = chargerFilieres();

        String prompt = buildPrompt(texteReleve, matieres, filieres);

        String body = """
            {
              "model": "%s",
              "temperature": 0.1,
              "max_tokens": 4000,
              "messages": [
                {"role": "system", "content": "Réponds UNIQUEMENT en JSON valide sans markdown ni explication."},
                {"role": "user",   "content": %s}
              ]
            }
            """.formatted(GROQ_MODEL_TEXT, jsonEscape(prompt));

        String reponse = callGroq(body);
        return parseJson(reponse);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DU PROMPT
    // ══════════════════════════════════════════════════════════════════════

    private String buildPrompt(String texte,
                               List<Map<String, Object>> matieres,
                               List<Map<String, Object>> filieres) {
        return """
Tu es un conseiller d'orientation académique. Analyse ce relevé de notes et retourne UNIQUEMENT ce JSON valide, sans markdown, sans explication :

Relevé de notes de l'étudiant :
%s

Matières disponibles dans le système :
%s

Filières disponibles :
%s

Retourne STRICTEMENT ce format JSON et rien d'autre :
{
  "notesDetectees": [
    {"matiereReleve": "Nom matière", "matiereSysteme": "Nom dans système", "note": 14.5, "noteMax": 20, "coefficient": 2, "domaine": "informatique"}
  ],
  "moyenneGenerale": 13.5,
  "pointsForts": ["Algorithmique", "Mathématiques"],
  "pointsFaibles": ["Anglais"],
  "scoreParFiliere": [
    {"filiereId": 1, "filiereNom": "Informatique", "score": 85.5, "compatible": true, "explication": "Bonne compatibilité"}
  ],
  "filiereRecommandee": "Informatique",
  "conseil": "Conseil personnalisé pour l'étudiant"
}
""".formatted(texte, toJson(matieres), toJson(filieres));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CHARGEMENT BDD
    // ══════════════════════════════════════════════════════════════════════

    private List<Map<String, Object>> chargerMatieres() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT titre, coefficient, domaine, note_min_requise, note_max FROM matiere")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("titre",          rs.getString("titre"));
                m.put("coefficient",    rs.getDouble("coefficient"));
                m.put("domaine",        rs.getString("domaine"));
                m.put("noteMinRequise", rs.getDouble("note_min_requise"));
                m.put("noteMax",        rs.getDouble("note_max"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de charger les matières : " + e.getMessage());
        }
        return list;
    }

    private List<Map<String, Object>> chargerFilieres() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nom, description FROM filiere")) {
            while (rs.next()) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("id",          rs.getInt("id"));
                f.put("nom",         rs.getString("nom"));
                f.put("description", rs.getString("description"));
                list.add(f);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de charger les filières : " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  APPEL API GROQ
    // ══════════════════════════════════════════════════════════════════════

    private String callGroq(String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getGroqApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Groq HTTP " + resp.statusCode() + " : " + resp.body());

        String body = resp.body();
        // Extraire choices[0].message.content
        String marker = "\"content\":";
        int idx = body.lastIndexOf(marker);
        if (idx == -1) throw new RuntimeException("Réponse Groq inattendue : " + body);
        int start = body.indexOf('"', idx + marker.length()) + 1;
        int end   = findStringEnd(body, start);
        String content = body.substring(start, end);
        // Décoder les séquences JSON (\n, \", \\)
        content = content.replace("\\n", "\n")
                         .replace("\\\"", "\"")
                         .replace("\\\\", "\\")
                         .replace("\\/", "/");
        return content;
    }

    /** Trouve la fin d'une chaîne JSON (gère les échappements). */
    private int findStringEnd(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"')  return i;
        }
        return s.length();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PARSING JSON MINIMAL (sans dépendance externe)
    // ══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJson(String json) {
        json = json.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();
        return (Map<String, Object>) parseValue(json, new int[]{0});
    }

    private Object parseValue(String s, int[] pos) {
        skipWs(s, pos);
        if (pos[0] >= s.length()) return null;
        char c = s.charAt(pos[0]);
        if (c == '{') return parseObject(s, pos);
        if (c == '[') return parseArray(s, pos);
        if (c == '"') return parseString(s, pos);
        if (c == 't') { pos[0] += 4; return Boolean.TRUE; }
        if (c == 'f') { pos[0] += 5; return Boolean.FALSE; }
        if (c == 'n') { pos[0] += 4; return null; }
        return parseNumber(s, pos);
    }

    private Map<String, Object> parseObject(String s, int[] pos) {
        Map<String, Object> map = new LinkedHashMap<>();
        pos[0]++;
        skipWs(s, pos);
        while (pos[0] < s.length() && s.charAt(pos[0]) != '}') {
            String key = parseString(s, pos);
            skipWs(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ':') pos[0]++;
            Object val = parseValue(s, pos);
            map.put(key, val);
            skipWs(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ',') pos[0]++;
            skipWs(s, pos);
        }
        if (pos[0] < s.length()) pos[0]++;
        return map;
    }

    private List<Object> parseArray(String s, int[] pos) {
        List<Object> list = new ArrayList<>();
        pos[0]++;
        skipWs(s, pos);
        while (pos[0] < s.length() && s.charAt(pos[0]) != ']') {
            list.add(parseValue(s, pos));
            skipWs(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ',') pos[0]++;
            skipWs(s, pos);
        }
        if (pos[0] < s.length()) pos[0]++;
        return list;
    }

    private String parseString(String s, int[] pos) {
        if (pos[0] >= s.length() || s.charAt(pos[0]) != '"') return "";
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == '\\' && pos[0] + 1 < s.length()) {
                pos[0]++;
                char esc = s.charAt(pos[0]);
                switch (esc) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default   -> sb.append(esc);
                }
            } else if (c == '"') {
                pos[0]++;
                return sb.toString();
            } else {
                sb.append(c);
            }
            pos[0]++;
        }
        return sb.toString();
    }

    private Number parseNumber(String s, int[] pos) {
        int start = pos[0];
        while (pos[0] < s.length() && "0123456789.-+eE".indexOf(s.charAt(pos[0])) >= 0) pos[0]++;
        String num = s.substring(start, pos[0]);
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E"))
                return Double.parseDouble(num);
            return Long.parseLong(num);
        } catch (NumberFormatException e) { return 0; }
    }

    private void skipWs(String s, int[] pos) {
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════

    private String detectMimeType(File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    private String toJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : list.get(i).entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":");
                Object v = e.getValue();
                if (v instanceof String str)
                    sb.append("\"").append(str.replace("\"", "\\\"")).append("\"");
                else sb.append(v);
                first = false;
            }
            sb.append("}");
        }
        return sb.append("]").toString();
    }

    private String jsonEscape(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}