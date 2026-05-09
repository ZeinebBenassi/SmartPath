package tn.esprit.utils;

import org.json.JSONObject;
import tn.esprit.services.SymfonyAuthService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Utilitaire pour faire des requêtes HTTPS à l'API Symfony.
 * Gère automatiquement le token JWT et les en-têtes.
 */
public class ApiHttpClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
            .build();

    // ── GET Request ───────────────────────────────────────────────────────────
    /**
     * Effectue une requête GET à l'API avec authentification JWT.
     *
     * @param endpoint L'endpoint complet (ex: https://api.com/user/profile)
     * @return JSONObject si réussie, null sinon
     */
    public static JSONObject get(String endpoint) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Accept", "application/json");

            // Ajouter le token JWT si disponible
            SymfonyAuthService.addAuthorizationHeader(builder);

            HttpRequest request = builder.GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return new JSONObject(response.body());
            } else if (response.statusCode() == 401) {
                System.err.println("[ApiHttpClient] 401 Unauthorized - Token may have expired");
                SymfonyAuthService.clearAuthToken();
                return null;
            } else {
                System.err.println("[ApiHttpClient] GET error " + response.statusCode() + ": " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[ApiHttpClient] GET error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── POST Request ──────────────────────────────────────────────────────────
    /**
     * Effectue une requête POST à l'API avec authentification JWT.
     *
     * @param endpoint L'endpoint complet (ex: https://api.com/user/profile)
     * @param payload  JSONObject à envoyer
     * @return JSONObject si réussie, null sinon
     */
    public static JSONObject post(String endpoint, JSONObject payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            // Ajouter le token JWT si disponible
            SymfonyAuthService.addAuthorizationHeader(builder);

            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return new JSONObject(response.body());
            } else if (response.statusCode() == 401) {
                System.err.println("[ApiHttpClient] 401 Unauthorized - Token may have expired");
                SymfonyAuthService.clearAuthToken();
                return null;
            } else {
                System.err.println("[ApiHttpClient] POST error " + response.statusCode() + ": " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[ApiHttpClient] POST error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── PUT Request ───────────────────────────────────────────────────────────
    /**
     * Effectue une requête PUT à l'API avec authentification JWT.
     *
     * @param endpoint L'endpoint complet (ex: https://api.com/user/1)
     * @param payload  JSONObject à envoyer
     * @return JSONObject si réussie, null sinon
     */
    public static JSONObject put(String endpoint, JSONObject payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            // Ajouter le token JWT si disponible
            SymfonyAuthService.addAuthorizationHeader(builder);

            HttpRequest request = builder.method("PUT", HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return new JSONObject(response.body());
            } else if (response.statusCode() == 401) {
                System.err.println("[ApiHttpClient] 401 Unauthorized - Token may have expired");
                SymfonyAuthService.clearAuthToken();
                return null;
            } else {
                System.err.println("[ApiHttpClient] PUT error " + response.statusCode() + ": " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[ApiHttpClient] PUT error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── DELETE Request ────────────────────────────────────────────────────────
    /**
     * Effectue une requête DELETE à l'API avec authentification JWT.
     *
     * @param endpoint L'endpoint complet (ex: https://api.com/user/1)
     * @return true si réussie, false sinon
     */
    public static boolean delete(String endpoint) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT))
                    .header("Accept", "application/json");

            // Ajouter le token JWT si disponible
            SymfonyAuthService.addAuthorizationHeader(builder);

            HttpRequest request = builder.DELETE().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                return true;
            } else if (response.statusCode() == 401) {
                System.err.println("[ApiHttpClient] 401 Unauthorized - Token may have expired");
                SymfonyAuthService.clearAuthToken();
                return false;
            } else {
                System.err.println("[ApiHttpClient] DELETE error " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ApiHttpClient] DELETE error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── Utility Methods ───────────────────────────────────────────────────────

    /**
     * Vérifie si le token JWT est disponible.
     */
    public static boolean isAuthenticated() {
        return SymfonyAuthService.isAuthenticated();
    }

    /**
     * Efface le token (déconnexion).
     */
    public static void logout() {
        SymfonyAuthService.clearAuthToken();
        System.out.println("[ApiHttpClient] Déconnexion effectuée");
    }
}
