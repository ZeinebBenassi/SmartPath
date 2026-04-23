package tn.esprit.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lit config.properties depuis la racine du classpath ou du projet.
 */
public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        // 1) Essai depuis le classpath (src/main/resources ou racine)
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
        // Priorité : variable d'environnement, puis properties
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) return env;
        return props.getProperty(key, "");
    }
}
