package tn.esprit.utils;

import tn.esprit.entity.User;

public class EduSession {
    private static User currentUser;

    public static User getUser() {
        return currentUser;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    public static boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getType());
    }

    public static boolean isProf() {
        return currentUser != null && "prof".equalsIgnoreCase(currentUser.getType());
    }

    public static boolean isEtudiant() {
        return currentUser != null && "etudiant".equalsIgnoreCase(currentUser.getType());
    }

    public static boolean canManage() {
        return isAdmin() || isProf();
    }
}
