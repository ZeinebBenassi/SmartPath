package tn.esprit.services;

import tn.esprit.entity.Filiere;
import tn.esprit.entity.QuizResult;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.*;

/**
 * QuizStatisticsService — IDENTIQUE à AdminQuizController Symfony.
 *
 * IMPORTANT — Format JSON sauvegardé par QuizAnalyzer.recommendationsToJson() :
 *   [{"filiereNom":"Big Data & Analytics","score":70,"percentage":70}, ...]
 *
 * Le champ clé est "filiereNom" (String), pas "filiere" (int/objet).
 * countRecommendations() fait donc le match par NOM de filière.
 */
public class QuizStatisticsService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── Données brutes ────────────────────────────────────────────────────

    public List<QuizResult> findAllResults() {
        List<QuizResult> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz_result ORDER BY created_at DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResult(rs));
        } catch (SQLException e) {
            System.err.println("[QuizStats] findAll : " + e.getMessage());
        }
        return list;
    }

    public List<Filiere> findAllFilieres() {
        List<Filiere> list = new ArrayList<>();
        String sql = "SELECT * FROM filiere ORDER BY nom ASC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapFiliere(rs));
        } catch (SQLException e) {
            System.err.println("[QuizStats] findAllFilieres : " + e.getMessage());
        }
        return list;
    }

    public int getTotalResults() {
        String sql = "SELECT COUNT(*) FROM quiz_result";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[QuizStats] count : " + e.getMessage());
        }
        return 0;
    }

    // ── statistics() — logique IDENTIQUE Symfony ──────────────────────────

    /**
     * Équivalent Symfony statistics() :
     *   foreach ($filiereRepo->findAll() as $filiere)
     *       $filiereStats[] = ['filiere'=>$filiere, 'recommendations'=>countRecommendations(...)]
     *   usort DESC
     */
    public List<FiliereStatEntry> getFiliereStats() {
        List<Filiere>    filieres = findAllFilieres();
        List<QuizResult> results  = findAllResults();

        // Si la table filière BDD est vide, on reconstruit les filières
        // depuis les noms trouvés dans les recommandations JSON
        if (filieres.isEmpty()) {
            filieres = buildFilieresFromResults(results);
        }

        List<FiliereStatEntry> stats = new ArrayList<>();
        for (Filiere filiere : filieres) {
            int count = countRecommendations(filiere, results);
            stats.add(new FiliereStatEntry(filiere, count));
        }

        // usort DESC par recommendations
        stats.sort((a, b) -> Integer.compare(b.recommendations, a.recommendations));
        return stats;
    }

    /**
     * Équivalent Symfony getProfileTypeStats() :
     *   foreach ($results as $result) $stats[$result->getProfileType()]++
     *   arsort($stats)
     */
    public LinkedHashMap<String, Integer> getProfileStats() {
        List<QuizResult> results = findAllResults();
        Map<String, Integer> raw = new HashMap<>();

        for (QuizResult r : results) {
            String profile = r.getProfileType();
            if (profile != null && !profile.isBlank()) {
                raw.merge(profile, 1, Integer::sum);
            }
        }

        // arsort = tri DESC par valeur
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        raw.entrySet().stream()
           .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
           .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // ── countRecommendations ───────────────────────────────────────────────

    /**
     * Compte combien de résultats quiz recommandent cette filière.
     *
     * Le JSON Java a le format : {"filiereNom":"X","score":70,"percentage":70}
     * → on extrait "filiereNom" et on compare avec filiere.getNom()
     *
     * Fallback : si le JSON contient "filiere" (int ou objet), on compare par ID.
     */
    private int countRecommendations(Filiere filiere, List<QuizResult> results) {
        int count = 0;
        for (QuizResult result : results) {
            if (resultRecommendsFiliere(result.getRecommendations(), filiere)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retourne true si le JSON de recommandations mentionne cette filière.
     * Gère les 3 formats possibles :
     *   Format Java  : {"filiereNom":"Big Data & Analytics","score":70,...}
     *   Format Symfony id : {"filiere":5,"score":70,...}
     *   Format Symfony obj: {"filiere":{"id":5,"nom":"..."},"score":70,...}
     */
    private boolean resultRecommendsFiliere(String json, Filiere filiere) {
        if (json == null || json.isBlank() || json.equals("[]")) return false;

        // Découper le JSON en objets individuels
        // Chaque objet commence par { et finit par }
        String[] parts = json.split("\\},\\s*\\{");
        for (String part : parts) {
            // Nettoyer les crochets et accolades
            String obj = part.replaceAll("^\\[?\\{?", "").replaceAll("\\}?\\]?$", "");

            // ── Format Java : "filiereNom":"NomDeLaFiliere" ──
            String nomFromJson = extractJsonString(obj, "filiereNom");
            if (nomFromJson != null) {
                if (nomFromJson.trim().equalsIgnoreCase(filiere.getNom().trim())) {
                    return true;
                }
                // Correspondance partielle (ex : nom BDD légèrement différent)
                if (normalizeNom(nomFromJson).equals(normalizeNom(filiere.getNom()))) {
                    return true;
                }
                continue; // Format identifié → pas besoin de chercher "filiere"
            }

            // ── Format Symfony : "filiere":5 (entier direct) ──
            Integer filiereId = extractJsonInt(obj, "filiere");
            if (filiereId != null) {
                if (filiereId == filiere.getId()) return true;
                continue;
            }

            // ── Format Symfony : "filiere":{"id":5,...} (objet imbriqué) ──
            if (obj.contains("\"filiere\"") && obj.contains("\"id\"")) {
                // Extraire la sous-partie après "filiere":{
                int fIdx = obj.indexOf("\"filiere\"");
                if (fIdx >= 0) {
                    String sub = obj.substring(fIdx);
                    Integer nestedId = extractJsonInt(sub, "id");
                    if (nestedId != null && nestedId == filiere.getId()) return true;
                }
            }
        }
        return false;
    }

    /**
     * Normalise un nom de filière pour la comparaison :
     * minuscules, sans accents courants, sans espaces multiples.
     */
    private String normalizeNom(String nom) {
        if (nom == null) return "";
        return nom.trim().toLowerCase()
            .replace("é","e").replace("è","e").replace("ê","e")
            .replace("à","a").replace("â","a")
            .replace("î","i").replace("ï","i")
            .replace("ô","o").replace("ù","u").replace("û","u")
            .replace("ç","c")
            .replaceAll("\\s+", " ");
    }

    /**
     * Construit une liste de filières synthétiques à partir des noms
     * trouvés dans les recommandations JSON — utilisé si la table filiere est vide.
     */
    private List<Filiere> buildFilieresFromResults(List<QuizResult> results) {
        Map<String, Filiere> byNom = new LinkedHashMap<>();
        int fakeId = 1;
        for (QuizResult r : results) {
            String json = r.getRecommendations();
            if (json == null || json.isBlank()) continue;
            String[] parts = json.split("\\},\\s*\\{");
            for (String part : parts) {
                String obj = part.replaceAll("^\\[?\\{?", "").replaceAll("\\}?\\]?$", "");
                String nom = extractJsonString(obj, "filiereNom");
                if (nom != null && !nom.isBlank() && !byNom.containsKey(nom.trim())) {
                    Filiere f = new Filiere(fakeId++, nom.trim(), null, null, null, null, null, iconForNom(nom.trim()));
                    byNom.put(nom.trim(), f);
                }
            }
        }
        return new ArrayList<>(byNom.values());
    }

    /** Emoji par défaut selon le nom de filière */
    private String iconForNom(String nom) {
        String n = nom.toLowerCase();
        if (n.contains("data") || n.contains("big"))    return "📊";
        if (n.contains("ia") || n.contains("intellig")) return "🤖";
        if (n.contains("securit") || n.contains("cyber")) return "🔒";
        if (n.contains("reseau") || n.contains("réseau") || n.contains("telecom")) return "🌐";
        if (n.contains("cloud"))    return "☁️";
        if (n.contains("mobile"))   return "📱";
        if (n.contains("design") || n.contains("ux")) return "🎨";
        if (n.contains("systeme") || n.contains("système")) return "⚙️";
        if (n.contains("logiciel") || n.contains("génie")) return "💻";
        return "🎓";
    }

    // ── Extraction JSON minimale ──────────────────────────────────────────

    /** Extrait la valeur entière d'une clé JSON dans un fragment. */
    private Integer extractJsonInt(String fragment, String key) {
        String search = "\"" + key + "\":";
        int idx = fragment.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < fragment.length() && fragment.charAt(start) == ' ') start++;
        if (start >= fragment.length()) return null;
        char first = fragment.charAt(start);
        if (Character.isDigit(first)) {
            int end = start;
            while (end < fragment.length() && Character.isDigit(fragment.charAt(end))) end++;
            try { return Integer.parseInt(fragment.substring(start, end)); } catch (NumberFormatException ignored) {}
        }
        if (first == '"') {
            int end = fragment.indexOf('"', start + 1);
            if (end > 0) {
                String val = fragment.substring(start + 1, end);
                if (val.matches("\\d+")) try { return Integer.parseInt(val); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /** Extrait la valeur string d'une clé JSON dans un fragment. */
    public String extractJsonString(String fragment, String key) {
        String search = "\"" + key + "\":";
        int idx = fragment.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < fragment.length() && fragment.charAt(start) == ' ') start++;
        if (start >= fragment.length()) return null;
        char first = fragment.charAt(start);
        if (first == '"') {
            // Valeur string — gérer les caractères échappés
            StringBuilder sb = new StringBuilder();
            int i = start + 1;
            while (i < fragment.length()) {
                char c = fragment.charAt(i);
                if (c == '\\' && i + 1 < fragment.length()) {
                    char next = fragment.charAt(i + 1);
                    if (next == '"' || next == '\\') { sb.append(next); i += 2; continue; }
                }
                if (c == '"') break;
                sb.append(c);
                i++;
            }
            return sb.toString();
        } else if (first != '{' && first != '[') {
            // Valeur non-string (null, nombre…)
            int end = start;
            while (end < fragment.length() && fragment.charAt(end) != ',' && fragment.charAt(end) != '}') end++;
            String val = fragment.substring(start, end).trim();
            return val.equals("null") ? null : val;
        }
        return null;
    }

    // ── Nom étudiant ──────────────────────────────────────────────────────

    public String getNomEtudiant(int etudiantId) {
        if (etudiantId <= 0) return null;
        String sql = "SELECT nom, prenom FROM user WHERE id=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom    = rs.getString("nom")    != null ? rs.getString("nom").trim()    : "";
                String prenom = rs.getString("prenom") != null ? rs.getString("prenom").trim() : "";
                String full   = (nom + " " + prenom).trim();
                return full.isEmpty() ? null : full;
            }
        } catch (SQLException e) {
            // Tentative avec colonne "name"
            try (PreparedStatement ps2 = cnx.prepareStatement("SELECT name FROM user WHERE id=? LIMIT 1")) {
                ps2.setInt(1, etudiantId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return rs2.getString("name");
            } catch (SQLException ignored) {}
        }
        return null;
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private QuizResult mapResult(ResultSet rs) throws SQLException {
        return new QuizResult(
            rs.getInt("id"),
            rs.getInt("etudiant_id"),
            rs.getString("responses"),
            rs.getString("scores"),
            rs.getString("recommendations"),
            rs.getString("profile_type"),
            rs.getTimestamp("created_at")
        );
    }

    private Filiere mapFiliere(ResultSet rs) throws SQLException {
        Filiere f = new Filiere(
            rs.getInt("id"), rs.getString("nom"), rs.getString("categorie"),
            rs.getString("niveau"), rs.getString("description"),
            rs.getString("debouches"), rs.getString("competences"), rs.getString("icon")
        );
        try { f.setImage(rs.getString("image")); } catch (SQLException ignored) {}
        return f;
    }

    // ── DTO ───────────────────────────────────────────────────────────────

    public static class FiliereStatEntry {
        public final Filiere filiere;
        public final int     recommendations;

        public FiliereStatEntry(Filiere filiere, int recommendations) {
            this.filiere         = filiere;
            this.recommendations = recommendations;
        }
    }
}
