package tn.esprit.utils.feature_cours_et_quiz;

import tn.esprit.entity.User;

public final class AppSession {
    private static volatile User currentUser;

    private AppSession() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static String getFullName() {
        if (currentUser == null) return "";
        return (currentUser.getPrenom() != null ? currentUser.getPrenom() : "") + " " + 
               (currentUser.getNom() != null ? currentUser.getNom() : "");
    }

    public static void clear() {
        currentUser = null;
    }
}
