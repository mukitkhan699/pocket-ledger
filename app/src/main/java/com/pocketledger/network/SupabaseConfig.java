package com.pocketledger.network;

import com.pocketledger.BuildConfig;

public final class SupabaseConfig {

    private SupabaseConfig() {
    }

    public static boolean isConfigured() {
        return !url().isEmpty() && !anonKey().isEmpty()
                && url().startsWith("https://");
    }

    public static String url() {
        return BuildConfig.SUPABASE_URL == null ? "" : BuildConfig.SUPABASE_URL.trim();
    }

    public static String anonKey() {
        return BuildConfig.SUPABASE_ANON_KEY == null ? "" : BuildConfig.SUPABASE_ANON_KEY.trim();
    }

    public static String normalizedUrl() {
        String value = url();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String restUrl() {
        return normalizedUrl() + "/rest/v1";
    }

    public static String authUrl() {
        return normalizedUrl() + "/auth/v1";
    }

    public static String realtimeUrl() {
        String value = normalizedUrl();
        if (value.startsWith("https://")) {
            value = "wss://" + value.substring("https://".length());
        } else if (value.startsWith("http://")) {
            value = "ws://" + value.substring("http://".length());
        }
        return value + "/realtime/v1/websocket";
    }
}
