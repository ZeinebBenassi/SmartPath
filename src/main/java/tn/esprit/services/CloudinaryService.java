package tn.esprit.services;

import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Service d'upload / suppression d'images vers Cloudinary.
 * Utilise uniquement le HTTP client natif Java 11+ (aucun SDK tiers).
 * Les credentials sont lus depuis config.properties via ConfigLoader.
 *
 * Règle de signature Cloudinary :
 *   SHA1( param1=val1&param2=val2&...&paramN=valN + api_secret )
 *   où les paramètres sont triés ALPHABÉTIQUEMENT et on exclut
 *   api_key, file, resource_type et signature lui-même.
 */
public class CloudinaryService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    private static final String BASE = "https://api.cloudinary.com/v1_1/";
    private final HttpClient http = HttpClient.newHttpClient();

    public CloudinaryService() {
        this.cloudName = ConfigLoader.get("cloudinary.cloud_name");
        this.apiKey    = ConfigLoader.get("cloudinary.api_key");
        this.apiSecret = ConfigLoader.get("cloudinary.api_secret");
        this.folder    = ConfigLoader.get("cloudinary.folder");
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    /**
     * Upload une image vers Cloudinary.
     *
     * @param imageFile fichier local à uploader
     * @param publicId  identifiant Cloudinary court, SANS le dossier (ex: "user_42").
     *                  Le dossier est ajouté automatiquement depuis config.properties.
     * @return URL HTTPS publique de l'image, ou null en cas d'erreur
     */
    public String uploadImage(File imageFile, String publicId) {
        try {
            long timestamp = System.currentTimeMillis() / 1000L;

            // ── 1. Paramètres à signer (ORDRE ALPHABÉTIQUE, sans api_key/file/signature) ──
            // Cloudinary signe exactement ce que tu lui envoies → on construit
            // la map triée, puis la chaîne à signer, puis la signature.
            TreeMap<String, String> params = new TreeMap<>();
            params.put("folder",    folder);
            params.put("overwrite", "true");
            if (publicId != null && !publicId.isBlank())
                params.put("public_id", publicId);
            params.put("timestamp", String.valueOf(timestamp));

            String stringToSign = buildQueryString(params);          // trié alphabétiquement
            String signature    = sha1(stringToSign + apiSecret);    // sans "&" avant le secret

            System.out.println("🔑 String to sign : " + stringToSign);
            System.out.println("🔑 Signature      : " + signature);

            // ── 2. Corps multipart ────────────────────────────────────────────────────
            String boundary = "----Boundary" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream body = new ByteArrayOutputStream();

            addFilePart(body, boundary, imageFile);
            addField(body, boundary, "api_key",   apiKey);
            addField(body, boundary, "timestamp",  String.valueOf(timestamp));
            addField(body, boundary, "signature",  signature);
            addField(body, boundary, "folder",     folder);
            addField(body, boundary, "overwrite",  "true");
            if (publicId != null && !publicId.isBlank())
                addField(body, boundary, "public_id", publicId);
            write(body, "--" + boundary + "--\r\n");

            // ── 3. Requête HTTP ───────────────────────────────────────────────────────
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + cloudName + "/image/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(res.body());

            if (json.has("secure_url")) {
                System.out.println("✅ Cloudinary upload OK : " + json.getString("secure_url"));
                return json.getString("secure_url");
            } else {
                System.err.println("❌ Cloudinary upload error : " + res.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ CloudinaryService.uploadImage : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Supprime une image de Cloudinary via son public_id complet
     * (avec dossier, ex: "smartpath/avatars/user_42").
     *
     * @return true si supprimé avec succès
     */
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        try {
            long timestamp = System.currentTimeMillis() / 1000L;

            // Paramètres signés (ordre alphabétique)
            TreeMap<String, String> params = new TreeMap<>();
            params.put("public_id", publicId);
            params.put("timestamp", String.valueOf(timestamp));

            String signature = sha1(buildQueryString(params) + apiSecret);

            String formBody = "public_id=" + URLEncoder.encode(publicId, StandardCharsets.UTF_8)
                    + "&timestamp=" + timestamp
                    + "&api_key="   + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&signature=" + signature;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + cloudName + "/image/destroy"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(res.body());
            boolean ok = "ok".equals(json.optString("result"));
            System.out.println(ok
                    ? "✅ Cloudinary delete OK : " + publicId
                    : "⚠️  Cloudinary delete result : " + res.body());
            return ok;

        } catch (Exception e) {
            System.err.println("❌ CloudinaryService.deleteImage : " + e.getMessage());
            return false;
        }
    }

    // ── Utilitaires statiques ────────────────────────────────────────────────

    /**
     * Extrait le public_id complet depuis une URL Cloudinary.
     * Ex: "https://res.cloudinary.com/demo/image/upload/v123/smartpath/avatars/user_42.jpg"
     *   → "smartpath/avatars/user_42"
     */
    public static String extractPublicId(String url) {
        if (url == null || url.isBlank() || !url.contains("cloudinary.com")) return null;
        int idx = url.indexOf("/upload/");
        if (idx < 0) return null;
        String after = url.substring(idx + 8);
        // Sauter le token de version (v1234567/)
        if (after.matches("v\\d+/.*")) after = after.substring(after.indexOf('/') + 1);
        // Enlever l'extension
        int dot = after.lastIndexOf('.');
        return dot > 0 ? after.substring(0, dot) : after;
    }

    /** Indique si une URL est hébergée sur Cloudinary. */
    public static boolean isCloudinaryUrl(String url) {
        return url != null && url.contains("cloudinary.com");
    }

    // ── Helpers internes ─────────────────────────────────────────────────────

    /**
     * Construit la query-string triée alphabétiquement depuis une TreeMap.
     * Ex: {folder=x, overwrite=true, public_id=y, timestamp=z}
     *   → "folder=x&overwrite=true&public_id=y&timestamp=z"
     */
    private static String buildQueryString(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static void addFilePart(ByteArrayOutputStream out, String boundary, File file) throws IOException {
        String ext = getExt(file);
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
        write(out, "Content-Type: image/" + ext + "\r\n\r\n");
        out.write(Files.readAllBytes(file.toPath()));
        write(out, "\r\n");
    }

    private static void addField(ByteArrayOutputStream out, String boundary,
                                  String name, String value) throws IOException {
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(out, value + "\r\n");
    }

    private static void write(OutputStream os, String s) throws IOException {
        os.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String getExt(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        String ext = i > 0 ? n.substring(i + 1).toLowerCase() : "jpeg";
        return ext.equals("jpg") ? "jpeg" : ext;
    }

    private static String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
