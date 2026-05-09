package tn.esprit.services;

import org.json.JSONObject;
import org.json.JSONException;
import tn.esprit.entity.User;
import tn.esprit.utils.ApiConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service d'authentification avec l'API Symfony.
 * ✅ Envoie email + mot de passe en CLAIR via HTTPS JSON
 * ✅ Symfony gère le hachage et la vérification
 * ✅ Gère les erreurs 401 (identifiants invalides)
 */
public class SymfonyAuthService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
            .build();

    // ── Login ─────────────────────────────────────────────────────────────────
    /**
     * Authentifie un utilisateur auprès de l'API Symfony.
     *
     * @param email    Email de l'utilisateur
     * @param password Mot de passe EN CLAIR (Symfony le hache)
     * @return User si authentification réussie, null sinon
     */
    public User login(String email, String password) {
        try {
            // 1. Créer le JSON payload
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("password", password);  // ✅ Envoyer en CLAIR

            // 2. Créer la requête HTTPS
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.LOGIN_ENDPOINT))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // 3. Envoyer la requête
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 4. Traiter la réponse
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode == 200 || statusCode == 201) {
                // ✅ Login réussi
                return parseUserFromResponse(body);
            } else if (statusCode == 401) {
                // ❌ Identifiants invalides
                System.err.println("[SymfonyAuthService] Login 401: Identifiants incorrects pour " + email);
                return null;
            } else {
                // ❌ Autre erreur serveur
                System.err.println("[SymfonyAuthService] Login erreur " + statusCode + ": " + body);
                return null;
            }

        } catch (Exception e) {
            System.err.println("[SymfonyAuthService] Erreur login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    /**
     * Enregistre un nouvel utilisateur via l'API Symfony.
     *
     * @param email       Email
     * @param password    Mot de passe EN CLAIR
     * @param nom         Nom
     * @param prenom      Prénom
     * @param telephone   Numéro de téléphone (optionnel)
     * @param userType    Type d'utilisateur (etudiant, prof, admin)
     * @return User si inscription réussie, null sinon
     */
    public User register(String email, String password, String nom, String prenom,
                        String telephone, String userType) {
        try {
            // 1. Créer le JSON payload
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("password", password);  // ✅ Envoyer en CLAIR
            payload.put("nom", nom);
            payload.put("prenom", prenom);
            if (telephone != null && !telephone.isEmpty()) {
                payload.put("telephone", telephone);
            }
            if (userType != null && !userType.isEmpty()) {
                payload.put("type", userType);
            }

            // 2. Créer la requête HTTPS
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.REGISTER_ENDPOINT))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // 3. Envoyer la requête
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 4. Traiter la réponse
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode == 200 || statusCode == 201) {
                // ✅ Inscription réussie
                return parseUserFromResponse(body);
            } else if (statusCode == 400 || statusCode == 409) {
                // ❌ Email déjà utilisé ou données invalides
                System.err.println("[SymfonyAuthService] Register erreur " + statusCode + ": " + body);
                return null;
            } else {
                // ❌ Autre erreur serveur
                System.err.println("[SymfonyAuthService] Register erreur " + statusCode + ": " + body);
                return null;
            }

        } catch (Exception e) {
            System.err.println("[SymfonyAuthService] Erreur register: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── Parse User from Response ──────────────────────────────────────────────
    /**
     * Convertit la réponse JSON Symfony en objet User.
     * La réponse Symfony doit contenir les champs: id, email, nom, prenom, type, roles, etc.
     */
    private User parseUserFromResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            User user = new User();
            user.setId(json.optInt("id", 0));
            user.setEmail(json.optString("email", ""));
            user.setNom(json.optString("nom", ""));
            user.setPrenom(json.optString("prenom", ""));
            user.setTelephone(json.optString("telephone", ""));
            user.setType(json.optString("type", "etudiant"));
            user.setRoles(json.optString("roles", "[]"));
            user.setPhoto(json.optString("photo", null));
            user.setStatus(json.optString("status", "actif"));

            // Si vous avez un token JWT, vous pouvez le stocker
            if (json.has("token")) {
                String token = json.getString("token");
                // ✅ Stocker le token pour les requêtes futures
                storeAuthToken(token);
            }

            return user;
        } catch (JSONException e) {
            System.err.println("[SymfonyAuthService] Erreur parsing JSON: " + e.getMessage());
            return null;
        }
    }

    // ── Token Management ──────────────────────────────────────────────────────
    private static String authToken = null;

    /**
     * Stocke le token JWT pour les requêtes ultérieures.
     */
    public static void storeAuthToken(String token) {
        authToken = token;
    }

    /**
     * Récupère le token stocké.
     */
    public static String getAuthToken() {
        return authToken;
    }

    /**
     * Efface le token (déconnexion).
     */
    public static void clearAuthToken() {
        authToken = null;
    }

    /**
     * Vérifie si un token est disponible.
     */
    public static boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    /**
     * Ajoute le token JWT à l'en-tête Authorization d'une requête.
     */
    public static void addAuthorizationHeader(HttpRequest.Builder builder) {
        if (authToken != null && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
    }
}
