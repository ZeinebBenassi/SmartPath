package tn.esprit.utils;

/**
 * PasswordStrengthUtil – Analyse et classe la force d'un mot de passe.
 *
 * Niveaux :
 *   LOW    🔴 – mot de passe faible
 *   MEDIUM 🟠 – mot de passe moyen
 *   HIGH   🟢 – mot de passe fort
 *
 * Règles de classification :
 *   LOW    : < 6 caractères  OU  uniquement lettres  OU  uniquement chiffres
 *            OU mot très simple (123456, password, azerty…)
 *   MEDIUM : 6-10 caractères, mélange partiel (lettres + chiffres), sans symboles
 *   HIGH   : > 10 caractères + majuscules + minuscules + chiffres + symboles
 */
public class PasswordStrengthUtil {

    // ── Mots de passe bannis (exemples très simples) ──────────────────────────
    private static final java.util.Set<String> BANNED = java.util.Set.of(
        "123456", "password", "azerty", "qwerty", "111111",
        "123456789", "000000", "abc123", "iloveyou", "admin",
        "letmein", "welcome", "monkey", "dragon", "master",
        "pass", "test", "1234", "12345", "1234567890"
    );

    // ── Résultat d'analyse ────────────────────────────────────────────────────
    public enum Level { LOW, MEDIUM, HIGH }

    public static class Result {
        public final Level  level;
        public final String explication;
        public final String conseil;
        public final int    score; // 0-100 pour la barre de progression

        Result(Level level, String explication, String conseil, int score) {
            this.level       = level;
            this.explication = explication;
            this.conseil     = conseil;
            this.score       = score;
        }

        public String getLevelLabel() {
            return switch (level) {
                case LOW    -> "🔴 FAIBLE";
                case MEDIUM -> "🟠 MOYEN";
                case HIGH   -> "🟢 FORT";
            };
        }

        public String getColor() {
            return switch (level) {
                case LOW    -> "#e53e3e";
                case MEDIUM -> "#dd6b20";
                case HIGH   -> "#38a169";
            };
        }
    }

    // ── Analyse principale ────────────────────────────────────────────────────
    public static Result analyze(String password) {
        if (password == null) password = "";

        int    len        = password.length();
        boolean haUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower  = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(c ->
                !Character.isLetterOrDigit(c));
        boolean onlyLetters = password.chars().allMatch(Character::isLetter);
        boolean onlyDigits  = password.chars().allMatch(Character::isDigit);
        boolean isBanned    = BANNED.contains(password.toLowerCase());

        // ── LOW ──────────────────────────────────────────────────────────────
        if (len == 0) {
            return new Result(Level.LOW,
                "Aucun mot de passe saisi.",
                "Saisissez un mot de passe d'au moins 6 caractères.",
                0);
        }
        if (isBanned) {
            return new Result(Level.LOW,
                "Ce mot de passe est beaucoup trop courant et connu des pirates.",
                "Choisissez quelque chose d'unique : évitez les suites simples et les mots du dictionnaire.",
                5);
        }
        if (len < 6) {
            return new Result(Level.LOW,
                "Trop court (" + len + " caractère" + (len > 1 ? "s" : "") + ") — facile à deviner.",
                "Ajoutez plus de caractères. Visez au moins 10 avec des majuscules, chiffres et symboles.",
                10);
        }
        if (onlyLetters) {
            return new Result(Level.LOW,
                "Uniquement des lettres — manque de variété.",
                "Ajoutez des chiffres (@, #, !, 3, 7…) pour augmenter la complexité.",
                15);
        }
        if (onlyDigits) {
            return new Result(Level.LOW,
                "Uniquement des chiffres — très vulnérable aux attaques par force brute.",
                "Mélangez lettres, chiffres et symboles.",
                15);
        }

        // ── HIGH ─────────────────────────────────────────────────────────────
        if (len > 10 && haUpper && hasLower && hasDigit && hasSymbol) {
            int score = Math.min(100, 75 + (len - 11) * 2);
            return new Result(Level.HIGH,
                "Excellent ! Mélange complet : majuscules, minuscules, chiffres et symboles sur " + len + " caractères.",
                "Parfait. Conservez-le dans un gestionnaire de mots de passe sécurisé.",
                score);
        }

        // ── MEDIUM ───────────────────────────────────────────────────────────
        StringBuilder explBldr   = new StringBuilder();
        StringBuilder adviceBldr = new StringBuilder();

        if (len >= 6 && len <= 10) {
            explBldr.append("Longueur correcte (").append(len).append(" caractères)");
        } else {
            explBldr.append("Bonne longueur (").append(len).append(" caractères)");
        }

        java.util.List<String> manques = new java.util.ArrayList<>();
        if (!haUpper)   manques.add("majuscules");
        if (!hasLower)  manques.add("minuscules");
        if (!hasDigit)  manques.add("chiffres");
        if (!hasSymbol) manques.add("symboles (@, #, !, …)");

        if (!manques.isEmpty()) {
            explBldr.append(" mais il manque : ").append(String.join(", ", manques)).append(".");
            adviceBldr.append("Ajoutez ").append(String.join(", ", manques));
            if (len <= 10) adviceBldr.append(" et allongez à plus de 10 caractères");
            adviceBldr.append(" pour atteindre le niveau FORT.");
        } else {
            explBldr.append(". Bonne combinaison, mais un peu court.");
            adviceBldr.append("Allongez à plus de 10 caractères pour atteindre le niveau FORT.");
        }

        // Score intermédiaire
        int score = 30;
        if (haUpper)   score += 10;
        if (hasLower)  score += 10;
        if (hasDigit)  score += 10;
        if (hasSymbol) score += 15;
        if (len > 8)   score += 10;
        score = Math.min(score, 70);

        return new Result(Level.MEDIUM, explBldr.toString(), adviceBldr.toString(), score);
    }
}
