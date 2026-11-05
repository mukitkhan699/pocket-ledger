package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.CategoryDao;
import com.pocketledger.database.ProfileDao;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Category;
import com.pocketledger.models.MealSettings;
import com.pocketledger.models.Profile;
import com.pocketledger.models.Session;
import com.pocketledger.models.UserSettings;
import com.pocketledger.network.SupabaseClient;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.Prefs;
import com.pocketledger.utils.SessionManager;

import java.io.IOException;

public class AuthRepository {

    private final Context context;
    private final SessionManager sessionManager;
    private final SupabaseClient client;
    private final ProfileDao profileDao;
    private final CategoryDao categoryDao;
    private final SettingsDao settingsDao;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = SessionManager.get(context);
        this.client = SupabaseClient.get();
        this.profileDao = new ProfileDao(context);
        this.categoryDao = new CategoryDao(context);
        this.settingsDao = new SettingsDao(context);
    }

    public SupabaseClient.AuthResult signUp(String email, String password, String fullName)
            throws IOException, SupabaseClient.ApiException {
        SupabaseClient.AuthResult result = client.signUp(email, password, fullName);
        if (result.session != null) {
            completeLogin(result.session);
        }
        return result;
    }

    public Session signIn(String email, String password) throws IOException, SupabaseClient.ApiException {
        Session session = client.signIn(email, password);
        completeLogin(session);
        return session;
    }

    public void requestPasswordReset(String email) throws IOException, SupabaseClient.ApiException {
        client.recover(email);
    }

    public Session continueOffline(String name) {
        Session session = new Session();
        session.userId = Ids.newId();
        session.email = "";
        session.fullName = name == null || name.trim().isEmpty() ? "Local user" : name.trim();
        session.accessToken = "";
        session.refreshToken = "";
        session.expiresAtEpochSeconds = 0L;
        session.localOnly = true;
        sessionManager.saveSession(session);
        bootstrap(session, false);
        return session;
    }

    public void logout() {
        Session session = sessionManager.getSession();
        if (session != null && session.hasCloudSession()) {
            try {
                client.logout(session.accessToken);
            } catch (Exception ignored) {
            }
        }
        sessionManager.clear();
    }

    public Session refreshIfNeeded(Session session) throws IOException, SupabaseClient.ApiException {
        if (session == null) {
            return null;
        }
        if (session.localOnly || !session.isExpired()) {
            return session;
        }
        if (session.refreshToken == null || session.refreshToken.isEmpty()) {
            sessionManager.clear();
            throw new SupabaseClient.UnauthorizedException("Session expired");
        }
        Session refreshed;
        try {
            refreshed = client.refresh(session.refreshToken);
        } catch (SupabaseClient.UnauthorizedException e) {
            sessionManager.clear();
            throw e;
        }
        if (refreshed.fullName == null || refreshed.fullName.isEmpty()) {
            refreshed.fullName = session.fullName;
        }
        if (refreshed.email == null || refreshed.email.isEmpty()) {
            refreshed.email = session.email;
        }
        sessionManager.saveSession(refreshed);
        return refreshed;
    }

    public Session forceRefresh(Session session) throws IOException, SupabaseClient.ApiException {
        if (session == null || session.localOnly) {
            return session;
        }
        if (session.refreshToken == null || session.refreshToken.isEmpty()) {
            sessionManager.clear();
            throw new SupabaseClient.UnauthorizedException("Session expired");
        }
        Session refreshed;
        try {
            refreshed = client.refresh(session.refreshToken);
        } catch (SupabaseClient.UnauthorizedException e) {
            sessionManager.clear();
            throw e;
        }
        if (refreshed.fullName == null || refreshed.fullName.isEmpty()) {
            refreshed.fullName = session.fullName;
        }
        if (refreshed.email == null || refreshed.email.isEmpty()) {
            refreshed.email = session.email;
        }
        sessionManager.saveSession(refreshed);
        return refreshed;
    }

    public Session requireValidSession() throws IOException, SupabaseClient.ApiException {
        Session session = sessionManager.getSession();
        if (session == null) {
            throw new SupabaseClient.UnauthorizedException("Not signed in");
        }
        return refreshIfNeeded(session);
    }

    public void bootstrap(Session session, boolean enqueue) {
        String userId = session.userId;
        Profile existing = profileDao.find(userId);
        Profile profile = new Profile();
        profile.id = userId;
        profile.userId = userId;
        profile.email = session.email;
        profile.fullName = session.fullName;
        profile.createdAt = existing == null ? DateUtils.nowIso() : existing.createdAt;
        profile.updatedAt = DateUtils.nowIso();
        profile.deleted = false;
        profileDao.upsert(profile);

        int categoryCount = categoryDao.countActive(userId);
        categoryDao.seedDefaults(userId);

        MealSettings mealSettings = settingsDao.getMealSettings(userId);
        UserSettings userSettings = settingsDao.getUserSettings(userId);
        Prefs.setTheme(context, userSettings.theme);

        if (enqueue && !session.localOnly) {
            settingsDao.enqueue(userId, Constants.TABLE_PROFILES, userId);
            settingsDao.enqueue(userId, Constants.TABLE_MEAL_SETTINGS, mealSettings.id);
            settingsDao.enqueue(userId, Constants.TABLE_USER_SETTINGS, userSettings.id);
            if (categoryCount == 0) {
                for (Category category : categoryDao.list(userId, null)) {
                    settingsDao.enqueue(userId, Constants.TABLE_CATEGORIES, category.id);
                }
            }
        }
    }

    private void completeLogin(Session session) {
        if (session.fullName == null || session.fullName.trim().isEmpty()) {
            session.fullName = "Friend";
        }
        session.localOnly = false;
        sessionManager.saveSession(session);
        bootstrap(session, true);
    }
}
