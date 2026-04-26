package tn.esprit.services;

import tn.esprit.entity.Filiere;
import tn.esprit.entity.QuizResult;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.*;

/**
 * QuizStatisticsService — IDENTIQUE à AdminQuizController Symfony.
 *
 * Symfony statistics() :
 *   foreach ($filiereRepo->findAll() as $filiere)
 *       $filiereStats[] = ['filiere' => $filiere,
 *                          'recommendations' => $this->countRecommendations($filiere, $resultRepo)];
 *   usort($filiereStats, fn($a,$b) => $b['recommendations'] <=> $a['recommendations']);
 *
 *   private function countRecommendations(Filiere $filiere, QuizResultRepository $resultRepo): int
 *       foreach ($resultRepo->findAll() as $result)
 *           foreach ($result->getRecommendations() as $rec)
 *               $id = extractFiliereId($rec['filiere'])
 *               if ($id === $filiere->getId()) { $count++; break; }
 *
 *   private function getProfileTypeStats(): array
 *       foreach ($results as $result)
 *           $stats[$result->getProfileType()]++
 *       arsort($stats); return $stats;
 */
public class QuizStatisticsService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── Données brutes ────────────────────────────────────────────────── //

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

    // ── statistics() — logique IDENTIQUE Symfony ─────────────────────── //

    /**
     * Équivalent exact de Symfony statistics() :
     *   foreach ($filiereRepo->findAll() as $filiere)
     *       $filiereStats[] = ['filiere'=>$filiere, 'recommendations'=>countRecommendations(...)]
     *   usort DESC par recommendations
     *
     * Retourne une liste de FiliereStatEntry triée par count DESC.
     */
    public List<FiliereStatEntry> getFiliereStats() {
        List<Filiere>    filieres = findAllFilieres();
        List<QuizResult> results  = findAllResults();

        List<FiliereStatEntry> stats = new ArrayList<>();
        for (Filiere filiere : filieres) {
            int count = countRecommendations(filiere, results);
            stats.add(new FiliereStatEntry(filiere, count));
        }

        // usort($a,$b) => $b['recommendations'] <=> $a['recommendations']
        stats.sort((a, b) -> Integer.compare(b.recommendations, a.recommendations));
        return stats;
    }

    /**
     * Équivalent exact de Symfony getProfileTypeStats() :
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

    // ── countRecommendations — IDENTIQUE Symfony ──────────────────────── //

    /**
     * Équivalent exact de Symfony countRecommendations(Filiere $filiere, ...) :
     *   foreach ($resultRepo->findAll() as $result)
     *       foreach ($result->getRecommendations() as $rec)
     *           $id = extractFiliereId($rec['filiere'])
     *           if ($id === $filiere->getId()) { $count++; break; }
     */
    private int countRecommendations(Filiere filiere, List<QuizResult> results) {
        int count = 0;
        for (QuizResult result : results) {
            List<Integer> ids = extractFiliereIds(result.getRecommendations());
            for (int id : ids) {
                if (id == filiere.getId()) {
                    count++;
                    break; // break inner comme en Symfony
                }
            }
        }
        return count;
    }

    // ── extractFiliereId — IDENTIQUE Symfony ─────────────────────────── //

    /**
     * Équivalent de Symfony extractFiliereId(mixed $filiere) :
     *   if is_int($filiere) → $filiere
     *   if is_string && is_numeric → (int)
     *   if is_array && isset(['id']) → (int)$filiere['id']
     *   if instanceof Filiere → getId()
     *
     * En Java, le JSON stocké en BDD peut avoir :
     *   {"filiereNom":"X","score":70,"percentage":70}  → pas d'id → on ignore
     *   {"filiere":5,"score":70}                        → id = 5
     *   {"filiere":{"id":5,"nom":"X"},"score":70}       → id = 5
     */
    private List<Integer> extractFiliereIds(String json) {
        List<Integer> ids = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return ids;

        String[] objects = json.split("\\{");
        for (String obj : objects) {
            if (obj.isBlank()) continue;

            // Cas 1 : "filiere":5  (entier direct)
            Integer id = extractJsonInt(obj, "filiere");
            if (id != null) { ids.add(id); continue; }

            // Cas 2 : "filiere":{"id":5,...}  (objet imbriqué)
            // On cherche "id": à l'intérieur du fragment
            if (obj.contains("\"filiere\"") && obj.contains("\"id\"")) {
                Integer nestedId = extractJsonInt(obj, "id");
                if (nestedId != null) { ids.add(nestedId); continue; }
            }

            // Cas 3 : pas d'id explicite → recommandation par nom uniquement
            // (format JavaFX {"filiereNom":"X",...}) — on skip, pas de filière BDD liée
        }
        return ids;
    }

    // ── Extraction JSON minimale ──────────────────────────────────────── //

    /** Extrait la valeur entière d'une clé JSON dans un fragment. */
    private Integer extractJsonInt(String fragment, String key) {
        String search = "\"" + key + "\":";
        int idx = fragment.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < fragment.length() && fragment.charAt(start) == ' ') start++;
        if (start >= fragment.length()) return null;
        char first = fragment.charAt(start);
        // Si la valeur commence par un chiffre → entier
        if (Character.isDigit(first)) {
            int end = start;
            while (end < fragment.length() && Character.isDigit(fragment.charAt(end))) end++;
            try { return Integer.parseInt(fragment.substring(start, end)); } catch (NumberFormatException ignored) {}
        }
        // Si la valeur est une chaîne numérique ("5")
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
            int end = fragment.indexOf('"', start + 1);
            return end > 0 ? fragment.substring(start + 1, end) : null;
        } else {
            int end = start;
            while (end < fragment.length() && fragment.charAt(end) != ',' && fragment.charAt(end) != '}') end++;
            return fragment.substring(start, end).trim();
        }
    }

    // ── Nom étudiant ─────────────────────────────────────────────────── //

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
            try {
                PreparedStatement ps2 = cnx.prepareStatement("SELECT name FROM user WHERE id=? LIMIT 1");
                ps2.setInt(1, etudiantId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return rs2.getString("name");
            } catch (SQLException ignored) {}
        }
        return null;
    }

    // ── Mapping ──────────────────────────────────────────────────────── //

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
        f.setImage(rs.getString("image"));
        return f;
    }

    // ── DTO interne ───────────────────────────────────────────────────── //

    /**
     * Équivalent de ['filiere' => $filiere, 'recommendations' => $count] Symfony.
     * Porte l'objet Filiere complet (avec id, nom, icon) + le compteur.
     */
    public static class FiliereStatEntry {
        public final Filiere filiere;
        public final int     recommendations;

        public FiliereStatEntry(Filiere filiere, int recommendations) {
            this.filiere         = filiere;
            this.recommendations = recommendations;
        }
    }
}
