package com.pocketledger.utils;

import android.util.Patterns;

public final class Validators {

    private Validators() {
    }

    public static boolean isEmail(String value) {
        return value != null && Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches();
    }

    public static boolean isPassword(String value) {
        return value != null && value.length() >= 6;
    }

    public static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
