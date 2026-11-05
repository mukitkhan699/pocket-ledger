package com.pocketledger.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.pocketledger.models.Session;

public final class SessionManager {

    private static final String KEY_USER_ID = "session_user_id";
    private static final String KEY_EMAIL = "session_email";
    private static final String KEY_NAME = "session_name";
    private static final String KEY_ACCESS = "session_access";
    private static final String KEY_REFRESH = "session_refresh";
    private static final String KEY_EXPIRES = "session_expires";
    private static final String KEY_LOCAL_ONLY = "session_local_only";

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    public static SessionManager get(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SessionManager(Context context) {
        SharedPreferences securePrefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    Constants.SECURE_PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            securePrefs = context.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
        }
        prefs = securePrefs;
    }

    public void saveSession(Session session) {
        prefs.edit()
                .putString(KEY_USER_ID, session.userId)
                .putString(KEY_EMAIL, session.email)
                .putString(KEY_NAME, session.fullName)
                .putString(KEY_ACCESS, session.accessToken)
                .putString(KEY_REFRESH, session.refreshToken)
                .putLong(KEY_EXPIRES, session.expiresAtEpochSeconds)
                .putBoolean(KEY_LOCAL_ONLY, session.localOnly)
                .apply();
    }

    public Session getSession() {
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            return null;
        }
        Session session = new Session();
        session.userId = userId;
        session.email = prefs.getString(KEY_EMAIL, "");
        session.fullName = prefs.getString(KEY_NAME, "");
        session.accessToken = prefs.getString(KEY_ACCESS, "");
        session.refreshToken = prefs.getString(KEY_REFRESH, "");
        session.expiresAtEpochSeconds = prefs.getLong(KEY_EXPIRES, 0L);
        session.localOnly = prefs.getBoolean(KEY_LOCAL_ONLY, false);
        return session;
    }

    public void updateTokens(String accessToken, String refreshToken, long expiresAtEpochSeconds) {
        prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .putLong(KEY_EXPIRES, expiresAtEpochSeconds)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getSession() != null;
    }

    public String userId() {
        Session session = getSession();
        return session == null ? null : session.userId;
    }
}
