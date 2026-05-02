package tn.esprit.services;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lit .env et config.properties.
 */
public class ConfigLoader {

    private static final Properties props = new Properties();
    private static Dotenv dotenv;

    static {
        // 1) Charger .env
        try {
            dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception ignored) {}

        // 2) Essai depuis le classpath (config.properties)
        try (InputStream is = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {}

        // 2) Fallback : fichier à la racine du projet (workdir)
        if (props.isEmpty()) {
            try (InputStream is = new java.io.FileInputStream("config.properties")) {
                props.load(is);
            } catch (IOException ignored) {}
        }
    }

    public static String get(String key) {
        // Priorité 1 : .env
        if (dotenv != null) {
            String val = dotenv.get(key);
            if (val != null && !val.isEmpty()) return val;
        }

        // Priorité 2 : variable d'environnement système
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) return env;

        // Priorité 3 : properties
        return props.getProperty(key, "");
    }
}
