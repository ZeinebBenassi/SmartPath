package tn.esprit.utils.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Role;

public final class AccessControl {
    private AccessControl() {}

    public static boolean canManageContent(Role role) {
        return role == Role.ADMIN || role == Role.PROF;
    }

    public static boolean canDelete(Role role) {
        return role == Role.ADMIN || role == Role.PROF;
    }
}
