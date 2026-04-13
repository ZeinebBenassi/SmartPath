package tn.esprit.utils;

/**
 * Gestionnaire de navigation centralisé.
 * Permet à n'importe quel contrôleur enfant de naviguer
 * sans connaître le dashboard parent.
 */
public class NavigationManager {

    // Référence vers le dashboard actif (Admin, Prof, ou Etudiant)
    private static NavigationHandler activeHandler;

    public interface NavigationHandler {
        void navigateTo(String fxmlPath, String title);
    }

    public static void setHandler(NavigationHandler handler) {
        activeHandler = handler;
    }

    public static void navigateTo(String fxmlPath, String title) {
        if (activeHandler != null) {
            activeHandler.navigateTo(fxmlPath, title);
        }
    }
}
