package tn.esprit.utils;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validateur d'email avancé pour SmartPath.
 *
 * Niveaux de validation :
 *  1. Syntaxe (regex stricte)
 *  2. TLD whitelist (refuser .cvc, .abc, etc.)
 *  3. DNS lookup optionnel (vérifier que le domaine existe réellement)
 */
public class EmailValidator {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. REGEX — structure locale@domaine.tld
    //    - Partie locale : lettres, chiffres, . _ % + -
    //    - Domaine      : lettres, chiffres, . -  (pas de - en début/fin)
    //    - TLD          : 2 à 10 lettres (capturé séparément pour whitelist)
    // ─────────────────────────────────────────────────────────────────────────
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9._%+\\-]{0,63}" +   // partie locale (max 64 chars)
                    "@" +
                    "(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)" + // sous-domaines
                    "+([a-zA-Z]{2,10})$"                          // TLD capturé en groupe 1
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 2. WHITELIST TLD valides (liste IANA représentative)
    //    Ajout facile : ajouter la chaîne en minuscule dans le Set.
    // ─────────────────────────────────────────────────────────────────────────
    private static final Set<String> VALID_TLDS = new HashSet<>(Arrays.asList(
            // Génériques courants
            "com", "net", "org", "edu", "gov", "mil", "int",
            "info", "biz", "name", "pro", "aero", "coop", "museum",
            // Nouveaux gTLD populaires
            "io", "co", "app", "dev", "ai", "tech", "online", "site",
            "store", "shop", "blog", "cloud", "digital", "media",
            "email", "mail", "web", "social", "news", "live", "studio",
            // Pays (ccTLD) — liste étendue
            "tn",  // Tunisie
            "fr",  // France
            "de",  // Allemagne
            "uk",  // Royaume-Uni
            "us",  // États-Unis
            "ca",  // Canada
            "au",  // Australie
            "jp",  // Japon
            "cn",  // Chine
            "br",  // Brésil
            "in",  // Inde
            "it",  // Italie
            "es",  // Espagne
            "nl",  // Pays-Bas
            "be",  // Belgique
            "ch",  // Suisse
            "at",  // Autriche
            "se",  // Suède
            "no",  // Norvège
            "dk",  // Danemark
            "fi",  // Finlande
            "pl",  // Pologne
            "ru",  // Russie
            "tr",  // Turquie
            "sa",  // Arabie Saoudite
            "ae",  // Émirats Arabes Unis
            "eg",  // Égypte
            "ma",  // Maroc
            "dz",  // Algérie
            "ly",  // Libye
            "ng",  // Nigeria
            "za",  // Afrique du Sud
            "ke",  // Kenya
            "gh",  // Ghana
            "mx",  // Mexique
            "ar",  // Argentine
            "cl",  // Chili
            "co",  // Colombie
            "ve",  // Venezuela
            "pe",  // Pérou
            "nz",  // Nouvelle-Zélande
            "sg",  // Singapour
            "my",  // Malaisie
            "hk",  // Hong Kong
            "kr",  // Corée du Sud
            "pk",  // Pakistan
            "bd",  // Bangladesh
            "id",  // Indonésie
            "ph",  // Philippines
            "th",  // Thaïlande
            "vn",  // Vietnam
            "pt",  // Portugal
            "gr",  // Grèce
            "cz",  // République Tchèque
            "hu",  // Hongrie
            "ro",  // Roumanie
            "bg",  // Bulgarie
            "hr",  // Croatie
            "sk",  // Slovaquie
            "si",  // Slovénie
            "lt",  // Lituanie
            "lv",  // Lettonie
            "ee",  // Estonie
            "by",  // Biélorussie
            "ua",  // Ukraine
            "il",  // Israël
            "ir",  // Iran
            "iq",  // Irak
            "jo",  // Jordanie
            "lb",  // Liban
            "kw",  // Koweït
            "qa",  // Qatar
            "bh",  // Bahreïn
            "om",  // Oman
            "ye",  // Yémen
            "sy",  // Syrie
            // Deuxième niveau commun
            "com.tn", "edu.tn", "gov.tn", "net.tn",
            "co.uk",  "org.uk", "me.uk",
            "com.au", "net.au", "org.au"
    ));

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSULTAT DE VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    public enum ErrorCode {
        OK,
        EMPTY,
        INVALID_FORMAT,   // syntaxe incorrecte
        INVALID_TLD,      // extension non reconnue
        DOMAIN_NOT_FOUND  // DNS : domaine inexistant
    }

    public static class ValidationResult {
        public final boolean   valid;
        public final ErrorCode code;
        public final String    message;

        private ValidationResult(boolean valid, ErrorCode code, String message) {
            this.valid   = valid;
            this.code    = code;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, ErrorCode.OK, null);
        }

        public static ValidationResult error(ErrorCode code, String message) {
            return new ValidationResult(false, code, message);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTHODES PUBLIQUES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validation complète : format + TLD whitelist + DNS (optionnel).
     *
     * @param email      l'adresse à valider
     * @param checkDns   true = vérifier que le domaine existe via DNS
     */
    public static ValidationResult validate(String email, boolean checkDns) {
        if (email == null || email.isBlank()) {
            return ValidationResult.error(ErrorCode.EMPTY, "L'email est obligatoire.");
        }

        String trimmed = email.trim().toLowerCase();

        // 1. Vérification de format (regex)
        java.util.regex.Matcher m = EMAIL_REGEX.matcher(trimmed);
        if (!m.matches()) {
            return ValidationResult.error(
                    ErrorCode.INVALID_FORMAT,
                    "Format invalide. Exemple correct : nom@domaine.com"
            );
        }

        // 2. Vérification du TLD
        String tld = m.group(1).toLowerCase();
        if (!isValidTld(trimmed, tld)) {
            return ValidationResult.error(
                    ErrorCode.INVALID_TLD,
                    "Extension « ." + tld + " » non reconnue. "
                            + "Utilisez une extension valide (.com, .tn, .fr, .org…)"
            );
        }

        // 3. Vérification DNS (optionnel — réseau requis)
        if (checkDns) {
            String domain = trimmed.substring(trimmed.indexOf('@') + 1);
            if (!domainExists(domain)) {
                return ValidationResult.error(
                        ErrorCode.DOMAIN_NOT_FOUND,
                        "Le domaine « " + domain + " » n'existe pas ou est inaccessible."
                );
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Validation sans DNS (rapide, pour usage JavaFX temps réel).
     */
    public static ValidationResult validate(String email) {
        return validate(email, false);
    }

    /**
     * Retourne true si l'email est valide (format + TLD), false sinon.
     * Raccourci pratique pour les conditions.
     */
    public static boolean isValid(String email) {
        return validate(email, false).valid;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie le TLD, en gérant aussi les TLD de 2e niveau (co.uk, com.tn…).
     */
    private static boolean isValidTld(String email, String tld) {
        // Vérifier TLD simple (ex: com, fr)
        if (VALID_TLDS.contains(tld)) return true;

        // Vérifier TLD composé ex: co.uk — prendre les 2 derniers segments
        String domain = email.substring(email.indexOf('@') + 1);
        String[] parts = domain.split("\\.");
        if (parts.length >= 3) {
            String composed = parts[parts.length - 2] + "." + parts[parts.length - 1];
            if (VALID_TLDS.contains(composed)) return true;
        }

        return false;
    }

    /**
     * Vérifie qu'un domaine existe via résolution DNS.
     * Retourne true si résolvable, false si le domaine est introuvable.
     * Note : peut être lent (~1-3s) si le serveur DNS ne répond pas vite.
     */
    private static boolean domainExists(String domain) {
        try {
            InetAddress.getByName(domain);
            return true;
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }
}
