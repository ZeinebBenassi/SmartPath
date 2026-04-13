package com.smartpath.util.feature_cours_et_quiz;

import com.smartpath.model.feature_cours_et_quiz.User;

public final class AppSession {
    private static volatile User currentUser;

    private AppSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
