package tn.esprit.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class EmailService {

    private static final String ENV_EMAIL_PROVIDER = "SMARTPATH_EMAIL_PROVIDER";
    private static final String ENV_EMAIL_FROM = "SMARTPATH_EMAIL_FROM";
    private static final String ENV_EMAIL_FROM_NAME = "SMARTPATH_EMAIL_FROM_NAME";

    private static final String ENV_GMAIL_USER = "SMARTPATH_GMAIL_USER";
    private static final String ENV_GMAIL_APP_PASSWORD = "SMARTPATH_GMAIL_APP_PASSWORD";

    private static final String ENV_SENDGRID_API_KEY = "SMARTPATH_SENDGRID_API_KEY";

    private static final String DEFAULT_HOST = "smtp.gmail.com";
    private static final String DEFAULT_PORT = "587";

    private static final String SENDGRID_ENDPOINT = "https://api.sendgrid.com/v3/mail/send";

    public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes) throws MessagingException {
        Properties p = loadMailProperties();
        String provider = firstNonBlank(System.getenv(ENV_EMAIL_PROVIDER), p.getProperty("mail.provider"), "smtp");

        String subject = "SmartPath - Code de réinitialisation";
        String body = buildBody(code, ttlMinutes);

        if ("sendgrid".equalsIgnoreCase(provider)) {
            SendGridSettings settings = SendGridSettings.load(p);
            sendViaSendGrid(settings, toEmail, subject, body);
            return;
        }

        MailSettings settings = MailSettings.load(p);
        sendViaSmtp(settings, toEmail, subject, body);
    }

    private void sendViaSmtp(MailSettings settings, String toEmail, String subject, String body) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", settings.smtpAuth);
        props.put("mail.smtp.starttls.enable", settings.starttlsEnable);
        props.put("mail.smtp.host", settings.smtpHost);
        props.put("mail.smtp.port", settings.smtpPort);
        props.put("mail.smtp.ssl.trust", settings.smtpHost);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(settings.username, settings.appPassword);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(settings.username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");

        Transport.send(message);
    }

    private void sendViaSendGrid(SendGridSettings settings, String toEmail, String subject, String body) throws MessagingException {
        String json = buildSendGridJson(settings, toEmail, subject, body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SENDGRID_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + settings.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("SendGrid: requête interrompue", e);
        } catch (Exception e) {
            throw new MessagingException("SendGrid: échec requête - " + e.getMessage(), e);
        }

        int status = response.statusCode();
        if (status / 100 != 2) {
            String bodyResp = response.body();
            if (bodyResp == null) bodyResp = "";
            throw new MessagingException("SendGrid: erreur HTTP " + status + " - " + bodyResp);
        }
    }

    private String buildSendGridJson(SendGridSettings settings, String toEmail, String subject, String body) {
        String fromEmail = settings.fromEmail;
        String fromName = settings.fromName;

        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        sb.append("\"personalizations\":[{\"to\":[{\"email\":\"")
                .append(escapeJson(toEmail))
                .append("\"}],\"subject\":\"")
                .append(escapeJson(subject))
                .append("\"}]");

        sb.append(",\"from\":{\"email\":\"")
                .append(escapeJson(fromEmail))
                .append("\"");
        if (!isBlank(fromName)) {
            sb.append(",\"name\":\"").append(escapeJson(fromName)).append("\"");
        }
        sb.append('}');

        sb.append(",\"content\":[{\"type\":\"text/plain\",\"value\":\"")
                .append(escapeJson(body))
                .append("\"}]}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private String buildBody(String code, int ttlMinutes) {
        return "Bonjour,\n\n"
                + "Voici votre code de réinitialisation SmartPath : " + code + "\n"
                + "Ce code est valable " + ttlMinutes + " minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n"
                + "— SmartPath\n";
    }

    private static Properties loadMailProperties() {
        Properties p = new Properties();
        try (InputStream is = EmailService.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {}
        return p;
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        if (!isBlank(c)) return c;
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static final class MailSettings {
        private final String username;
        private final String appPassword;
        private final String smtpHost;
        private final String smtpPort;
        private final String starttlsEnable;
        private final String smtpAuth;

        private MailSettings(String username, String appPassword, String smtpHost, String smtpPort, String starttlsEnable, String smtpAuth) {
            this.username = username;
            this.appPassword = appPassword;
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.starttlsEnable = starttlsEnable;
            this.smtpAuth = smtpAuth;
        }

        private static MailSettings load(Properties p) {
            String username = firstNonBlank(System.getenv(ENV_GMAIL_USER), p.getProperty("mail.username"));
            String appPwd = firstNonBlank(System.getenv(ENV_GMAIL_APP_PASSWORD), p.getProperty("mail.appPassword"));

            if (isBlank(username) || isBlank(appPwd)) {
                throw new IllegalStateException(
                        "Identifiants Gmail manquants. Configurez les variables d'environnement "
                                + ENV_GMAIL_USER + " et " + ENV_GMAIL_APP_PASSWORD
                                + " (App Password), ou remplissez src/main/resources/mail.properties.");
            }

            String normalizedPwd = appPwd.trim().replaceAll("\\s+", "");

            String host = firstNonBlank(p.getProperty("mail.smtp.host"), DEFAULT_HOST);
            String port = firstNonBlank(p.getProperty("mail.smtp.port"), DEFAULT_PORT);
            String starttls = firstNonBlank(p.getProperty("mail.smtp.starttls.enable"), "true");
            String auth = firstNonBlank(p.getProperty("mail.smtp.auth"), "true");

            return new MailSettings(username.trim(), normalizedPwd, host.trim(), port.trim(), starttls.trim(), auth.trim());
        }
    }

    private static final class SendGridSettings {
        private final String apiKey;
        private final String fromEmail;
        private final String fromName;

        private SendGridSettings(String apiKey, String fromEmail, String fromName) {
            this.apiKey = apiKey;
            this.fromEmail = fromEmail;
            this.fromName = fromName;
        }

        private static SendGridSettings load(Properties p) {
            String apiKey = firstNonBlank(System.getenv(ENV_SENDGRID_API_KEY), p.getProperty("mail.sendgrid.apiKey"));
            String from = firstNonBlank(System.getenv(ENV_EMAIL_FROM), p.getProperty("mail.from"));
            String fromName = firstNonBlank(System.getenv(ENV_EMAIL_FROM_NAME), p.getProperty("mail.fromName"), "SmartPath");

            if (isBlank(apiKey)) {
                throw new IllegalStateException(
                        "Clé API SendGrid manquante. Configurez la variable d'environnement "
                                + ENV_SENDGRID_API_KEY + " ou remplissez mail.sendgrid.apiKey dans src/main/resources/mail.properties.");
            }
            if (isBlank(from)) {
                throw new IllegalStateException(
                        "Adresse 'from' manquante. Configurez la variable d'environnement "
                                + ENV_EMAIL_FROM + " (adresse expéditeur validée SendGrid), ou remplissez mail.from dans src/main/resources/mail.properties.");
            }

            return new SendGridSettings(apiKey.trim(), from.trim(), isBlank(fromName) ? null : fromName.trim());
        }
    }
}
