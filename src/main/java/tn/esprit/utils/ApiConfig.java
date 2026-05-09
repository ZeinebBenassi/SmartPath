package tn.esprit.utils;

/**
 * Configuration centralisée pour les endpoints de l'API Symfony.
 * Modifiez BASE_URL selon votre environnement (développement, production).
 */
public class ApiConfig {
    // ── Base URL de l'API Symfony ─────────────────────────────────────────────
    // ✅ UPDATE THIS WITH YOUR ACTUAL API URL:
    // For local dev:  "http://localhost:8000"
    // For production: "https://api.yourdomain.com"
    public static final String BASE_URL = "http://localhost:8000";  // ← CHANGE THIS
    
    // ── Endpoints d'authentification ──────────────────────────────────────────
    public static final String LOGIN_ENDPOINT = BASE_URL + "/api/login";
    public static final String REGISTER_ENDPOINT = BASE_URL + "/api/register";
    public static final String USER_PROFILE_ENDPOINT = BASE_URL + "/api/user/profile";
    
    // ── Timeout (en secondes) ─────────────────────────────────────────────────
    public static final int REQUEST_TIMEOUT = 30;
}
