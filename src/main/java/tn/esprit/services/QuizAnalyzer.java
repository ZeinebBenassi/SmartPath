package tn.esprit.services;

import tn.esprit.entity.Answer;

import java.util.*;

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

    public Map<String, Object> analyzeResponses(List<Answer> selectedAnswers) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String trait : TRAITS) scores.put(trait, 0);

        for (Answer answer : selectedAnswers) {
            String trait = answer.getTrait();
            if (scores.containsKey(trait))
                scores.put(trait, scores.get(trait) + answer.getPoints());
        }

        String profileType = determineProfileType(scores);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scores", scores);
        result.put("profileType", profileType);
        return result;
    }

    public String determineProfileType(Map<String, Integer> scores) {
        if (scores.isEmpty()) return "Informaticien Polyvalent";
        String maxTrait = null;
        int maxValue = -1;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                maxTrait = entry.getKey();
            }
        }
        return PROFILES.getOrDefault(maxTrait, "Informaticien Polyvalent");
    }

    public String scoresToJson(Map<String, Integer> scores) {
        StringBuilder sb = new StringBuilder("{");
        scores.forEach((k, v) -> sb.append("\"").append(k).append("\":").append(v).append(","));
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    public String responsesToJson(List<Integer> answerIds) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < answerIds.size(); i++) {
            sb.append(answerIds.get(i));
            if (i < answerIds.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
