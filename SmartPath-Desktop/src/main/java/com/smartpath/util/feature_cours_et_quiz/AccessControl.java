package com.smartpath.util.feature_cours_et_quiz;

import com.smartpath.model.feature_cours_et_quiz.Role;

public final class AccessControl {
    private AccessControl() {
    }

    public static boolean canManageContent(Role role) {
        return role == Role.ADMIN || role == Role.PROF;
    }

    public static boolean canDelete(Role role) {
        return role == Role.ADMIN || role == Role.PROF;
    }
}
