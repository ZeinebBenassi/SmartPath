package tn.esprit.services;

import tn.esprit.entity.Answer;
import tn.esprit.entity.Filiere;

import java.sql.SQLException;
import java.util.*;

/**
 * QuizAnalyzer — analyse les réponses et recommande des filières.
 * Les recommandations sont utilisées par QuizResultController pour appeler
 * l'API Groq directement (sans BDD) et afficher les universités.
 */
public class QuizAnalyzer {

    private static final List<String> TRAITS = List.of(
            "analytique", "pratique", "creatif", "technique",
            "mathematique", "algorithmique", "systemes",
            "reseaux", "securite", "donnees"
    );

    private static final Map<String, String> PROFILES = Map.of(
            "analytique",    "Data Analyst",
            "pratique",      "Développeur Full Stack",
            "creatif",       "UX/UI Designer",
            "technique",     "Ingénieur Système",
            "mathematique",  "Data Scientist",
            "algorithmique", "Expert IA/ML",
            "systemes",      "Administrateur Système",
            "reseaux",       "Ingénieur Réseau",
            "securite",      "Expert Cybersécurité",
            "donnees",       "Ingénieur Big Data"
    );

    // Traits pondérés de chaque filière (poids de 1 à 3)
    private static final Map<String, Map<String, Integer>> FILIERE_TRAITS = new LinkedHashMap<>() {{
        put("Génie Logiciel",              Map.of("pratique", 3, "algorithmique", 2, "technique", 2));
        put("Data Science",                Map.of("mathematique", 3, "analytique", 3, "donnees", 2));
        put("Intelligence Artificielle",   Map.of("algorithmique", 3, "mathematique", 2, "analytique", 2));
        put("Cybersécurité",               Map.of("securite", 3, "technique", 2, "reseaux", 2));
        put("Réseaux & Télécommunications",Map.of("reseaux", 3, "technique", 2, "systemes", 2));
        put("Cloud Computing",             Map.of("systemes", 3, "technique", 2, "reseaux", 2));
        put("UX/UI Design",                Map.of("creatif", 3, "analytique", 2, "pratique", 1));
        put("Big Data & Analytics",        Map.of("donnees", 3, "mathematique", 2, "analytique", 2));
        put("Développement Mobile",        Map.of("pratique", 3, "creatif", 2, "technique", 1));
        put("Systèmes Embarqués",          Map.of("technique", 3, "algorithmique", 2, "systemes", 2));
    }};

    private final FiliereService filiereService = new FiliereService();

    // ------------------------------------------------------------------ //
    //  Analyse principale                                                 //
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeResponses(List<Answer> selectedAnswers) {
        // Calculer les scores par trait
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String trait : TRAITS) scores.put(trait, 0);
        for (Answer answer : selectedAnswers) {
            String trait = answer.getTrait();
            if (scores.containsKey(trait))
                scores.put(trait, scores.get(trait) + answer.getPoints());
        }

        String profileType                   = determineProfileType(scores);
        List<Map<String, Object>> recs       = recommendFilieres(scores);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scores",          scores);
        result.put("profileType",     profileType);
        result.put("recommendations", recs);
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Profil dominant                                                    //
    // ------------------------------------------------------------------ //

    public String determineProfileType(Map<String, Integer> scores) {
        if (scores.isEmpty()) return "Informaticien Polyvalent";
        String maxTrait = null;
        int maxValue = -1;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() > maxValue) { maxValue = e.getValue(); maxTrait = e.getKey(); }
        }
        return PROFILES.getOrDefault(maxTrait, "Informaticien Polyvalent");
    }

    // ------------------------------------------------------------------ //
    //  Recommandations de filières (top 5)                               //
    // ------------------------------------------------------------------ //

    public List<Map<String, Object>> recommendFilieres(Map<String, Integer> studentScores) {
        List<Map<String, Object>> recs = new ArrayList<>();

        // Essayer de lire les filières depuis la BDD
        List<Filiere> filieresBdd = new ArrayList<>();
        try { filieresBdd = filiereService.afficher(); } catch (SQLException ignored) {}

        if (filieresBdd.isEmpty()) {
            // Pas de BDD → utiliser les filières statiques FILIERE_TRAITS
            for (Map.Entry<String, Map<String, Integer>> entry : FILIERE_TRAITS.entrySet()) {
                int score = calculateMatchScore(studentScores, entry.getValue());
                if (score > 0) {
                    Map<String, Object> rec = new LinkedHashMap<>();
                    rec.put("filiereNom", entry.getKey());
                    rec.put("score",      score);
                    rec.put("percentage", Math.min(100, score));
                    recs.add(rec);
                }
            }
        } else {
            // BDD disponible : faire le match avec les filières réelles
            for (Filiere f : filieresBdd) {
                Map<String, Integer> traits = FILIERE_TRAITS.getOrDefault(f.getNom(), Map.of());
                if (traits.isEmpty()) continue;
                int score = calculateMatchScore(studentScores, traits);
                if (score > 0) {
                    Map<String, Object> rec = new LinkedHashMap<>();
                    rec.put("filiereNom", f.getNom());
                    rec.put("score",      score);
                    rec.put("percentage", Math.min(100, score));
                    recs.add(rec);
                }
            }
        }

        // Trier par score décroissant, top 5
        recs.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        return recs.size() > 5 ? recs.subList(0, 5) : recs;
    }

    private int calculateMatchScore(Map<String, Integer> student, Map<String, Integer> traits) {
        if (traits.isEmpty()) return 0;
        double total = 0, maxPossible = 0;
        for (Map.Entry<String, Integer> e : traits.entrySet()) {
            int studentScore = student.getOrDefault(e.getKey(), 0);
            int weight       = e.getValue();
            total       += (double) studentScore * weight;
            maxPossible += 100.0 * weight;
        }
        return maxPossible > 0 ? (int) ((total / maxPossible) * 100) : 0;
    }

    // ------------------------------------------------------------------ //
    //  Sérialisation JSON (pour sauvegarder en BDD)                      //
    // ------------------------------------------------------------------ //

    public String scoresToJson(Map<String, Integer> scores) {
        StringBuilder sb = new StringBuilder("{");
        scores.forEach((k, v) -> sb.append("\"").append(k).append("\":").append(v).append(","));
        if (sb.length() > 1 && sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        return sb.append("}").toString();
    }

    public String responsesToJson(List<Integer> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    public String recommendationsToJson(List<Map<String, Object>> recs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < recs.size(); i++) {
            Map<String, Object> r = recs.get(i);
            sb.append("{")
              .append("\"filiereNom\":\"").append(r.get("filiereNom")).append("\",")
              .append("\"score\":").append(r.get("score")).append(",")
              .append("\"percentage\":").append(r.get("percentage"))
              .append("}");
            if (i < recs.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
