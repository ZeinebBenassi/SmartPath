package tn.esprit.services.feature_cours_et_quiz;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BadWordsService {
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
        // French
        "merde", "con", "connard", "salope", "pute", "encule", "batard", "bite", "couille", "nique",
        // English
        "fuck", "shit", "asshole", "bitch", "bastard", "dick", "cunt", "pussy", "faggot", "nigger",
        // Arabic (transliterated/common)
        "zibi", "rabek", "kahba", "manyak", "zabour", "asba", "nayek", "mousiba",
        // Spanish
        "mierda", "puta", "cabron", "joder", "coño", "maricon", "pendejo"
    ));

    public static boolean containsBadWords(String text) {
        if (text == null || text.isBlank()) return false;
        
        String cleanText = text.toLowerCase()
            .replaceAll("[^a-z0-9\\sàâäéèêëïîôöùûüç]", " ")
            .replaceAll("\\s+", " ");
            
        String[] words = cleanText.split("\\s+");
        for (String word : words) {
            if (BAD_WORDS.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
