package tn.esprit.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ══════════════════════════════════════════════════════════════
 *  API 2 : Nominatim — API de géocodage gratuite OpenStreetMap
 * ══════════════════════════════════════════════════════════════
 *
 *  Endpoint : https://nominatim.openstreetmap.org/search
 *  Pas de clé API requise.
 *  Retourne latitude + longitude + adresse complète d'un lieu.
 *
 *  Utilisée dans SmartPath pour localiser les universités
 *  et afficher leur position sur une carte interactive Leaflet.js.
 */
public class NominatimService {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "SmartPathApp/1.0 (contact@smartpath.tn)";

    private final HttpClient http = HttpClient.newHttpClient();

    // ─────────────────────────────────────────────────────────────────
    //  Résultat de géocodage
    // ─────────────────────────────────────────────────────────────────

    public static class GeoResult {
        public final double lat;
        public final double lon;
        public final String displayName;

        public GeoResult(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }

        public boolean isValid() {
            return lat != 0 && lon != 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Méthode principale : géocoder un nom d'université
    // ─────────────────────────────────────────────────────────────────

    /**
     * Appel API Nominatim pour obtenir lat/lon d'une université.
     * Essaie d'abord avec countrycodes=tn (Tunisie), puis sans filtre.
     *
     * @param query  ex : "Université de Tunis El Manar Tunis Tunisie"
     * @return GeoResult avec lat, lon, displayName — ou null si introuvable
     */
    public GeoResult geocoder(String query) {
        // 1ère tentative : restreindre à la Tunisie
        GeoResult result = appellerNominatim(query, "tn");
        if (result != null && result.isValid()) return result;

        // 2ème tentative : sans restriction pays
        return appellerNominatim(query, null);
    }

    private GeoResult appellerNominatim(String query, String countryCode) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BASE_URL + "?q=" + encoded
                    + "&format=json&limit=1&accept-language=fr"
                    + (countryCode != null ? "&countrycodes=" + countryCode : "");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "fr")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return null;

            String body = resp.body().trim();
            if (body.equals("[]") || body.isEmpty()) return null;

            // Parser le JSON manuellement (sans dépendance externe)
            double lat = extraireDouble(body, "\"lat\"");
            double lon = extraireDouble(body, "\"lon\"");
            String displayName = extraireString(body, "\"display_name\"");

            if (lat == 0 && lon == 0) return null;
            return new GeoResult(lat, lon, displayName);

        } catch (Exception e) {
            System.err.println("Nominatim erreur : " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Parsing JSON minimal
    // ─────────────────────────────────────────────────────────────────

    private double extraireDouble(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            int start = json.indexOf('"', idx + key.length() + 1) + 1;
            int end   = json.indexOf('"', start);
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) { return 0; }
    }

    private String extraireString(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            int start = json.indexOf('"', idx + key.length() + 1) + 1;
            int end   = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) { return ""; }
    }
}
