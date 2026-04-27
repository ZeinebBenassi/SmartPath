package tn.esprit.utils.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Role;

public final class RoleUtils {
    private RoleUtils() {}

    public static Role normalize(String role) {
        if (role == null) return Role.ETUDIANT;
        String r = role.trim().toUpperCase();
        if (r.isBlank()) return Role.ETUDIANT;

        return switch (r) {
            case "ADMIN", "ROLE_ADMIN" -> Role.ADMIN;
            case "PROF", "ENSEIGNANT", "ROLE_PROF", "ROLE_ENSEIGNANT" -> Role.PROF;
            case "ETUDIANT", "STUDENT", "ROLE_ETUDIANT", "ROLE_STUDENT" -> Role.ETUDIANT;
            default -> Role.ETUDIANT;
        };
    }

    public static String display(Role role) {
        if (role == null) return "ETUDIANT";
        return switch (role) {
            case ADMIN -> "ADMIN";
            case PROF -> "PROF";
            case ETUDIANT -> "ETUDIANT";
        };
    }
}
