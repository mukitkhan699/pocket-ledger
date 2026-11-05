package com.pocketledger.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.pocketledger.database.DatabaseHelper;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
import com.pocketledger.models.SyncQueueItem;
import com.pocketledger.network.NetworkMonitor;
import com.pocketledger.network.SupabaseClient;
import com.pocketledger.network.SupabaseConfig;
import com.pocketledger.repositories.AuthRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncManager {

    private static final int PAGE_SIZE = 500;
    private static final String[] PULL_ORDER = new String[]{
            Constants.TABLE_PROFILES,
            Constants.TABLE_CATEGORIES,
            Constants.TABLE_MEAL_SETTINGS,
            Constants.TABLE_USER_SETTINGS,
            Constants.TABLE_MEAL_ENTRIES,
            Constants.TABLE_TRANSACTIONS,
            Constants.TABLE_LOANS,
            Constants.TABLE_LOAN_REPAYMENTS
    };

    public interface Callback {
        void onComplete(SyncResult result);
    }

    private static volatile SyncManager instance;

    private final Context app;
    private final DatabaseHelper helper;
    private final SettingsDao settingsDao;
    private final AuthRepository authRepository;
    private final SessionManager sessionManager;
    private final SupabaseClient client;

    public static SyncManager get(Context context) {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SyncManager(Context context) {
        this.app = context;
        this.helper = DatabaseHelper.get(context);
        this.settingsDao = new SettingsDao(context);
        this.authRepository = new AuthRepository(context);
        this.sessionManager = SessionManager.get(context);
        this.client = SupabaseClient.get();
    }

    public void syncAsync(Callback callback) {
        AppExecutors.io().execute(() -> {
            SyncResult result = syncBlocking();
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(result));
            }
        });
    }

    public SyncResult syncBlocking() {
        Session session = sessionManager.getSession();
        if (session == null) {
            SyncResult result = SyncResult.unauthorized();
            broadcast(result);
            return result;
        }
        if (session.localOnly) {
            SyncResult result = SyncResult.localOnly();
            result.pending = settingsDao.queueCount(session.userId);
            broadcast(result);
            return result;
        }
        if (!SupabaseConfig.isConfigured()) {
            SyncResult result = SyncResult.notConfigured();
            result.pending = settingsDao.queueCount(session.userId);
            broadcast(result);
            return result;
        }
        if (!NetworkMonitor.isOnline(app)) {
            SyncResult result = SyncResult.noNetwork();
            result.pending = settingsDao.queueCount(session.userId);
            broadcast(result);
            return result;
        }

        SyncResult result = new SyncResult();
        try {
            session = authRepository.requireValidSession();
        } catch (SupabaseClient.UnauthorizedException e) {
            SyncResult unauthorized = SyncResult.unauthorized();
            unauthorized.pending = settingsDao.queueCount(session.userId);
            broadcast(unauthorized);
            broadcastAuthExpired();
            return unauthorized;
        } catch (IOException e) {
            SyncResult offline = SyncResult.noNetwork();
            offline.pending = settingsDao.queueCount(session.userId);
            broadcast(offline);
            return offline;
        } catch (SupabaseClient.ApiException e) {
            SyncResult failed = new SyncResult();
            failed.message = "Could not verify your session: " + e.getMessage();
            failed.pending = settingsDao.queueCount(session.userId);
            broadcast(failed);
            return failed;
        }

        String userId = session.userId;
        String token = session.accessToken;
        Set<String> queued = new HashSet<>();
        for (SyncQueueItem item : settingsDao.queue(userId, 0)) {
            queued.add(key(item.tableName, item.recordId));
        }
        List<Deferred> deferred = new ArrayList<>();
        int newConflicts = 0;

        try {
            for (String table : PULL_ORDER) {
                PullOutcome outcome = pullTable(table, userId, token, queued, deferred);
                result.pulled += outcome.applied;
                newConflicts += outcome.conflicts;
            }
            if (!deferred.isEmpty()) {
                List<Deferred> retry = new ArrayList<>(deferred);
                deferred.clear();
                for (Deferred pending : retry) {
                    if (applyRemote(pending.table, pending.row)) {
                        if (pending.conflict) {
                            logConflict(userId, pending.table, pending.row, pending.localPayload);
                            settingsDao.dequeue(queueId(pending.table, pending.row.optString("id")));
                            queued.remove(key(pending.table, pending.row.optString("id")));
                            newConflicts++;
                        }
                        result.pulled++;
                    } else {
                        deferred.add(pending);
                    }
                }
            }

            PushOutcome pushOutcome = pushAll(userId, token, result);
            result.pushed = pushOutcome.pushed;
            result.failedPushes = pushOutcome.failed;
            newConflicts += pushOutcome.conflicts;
            if (pushOutcome.interrupted) {
                result.interrupted = true;
                result.noNetwork = true;
                result.message = "You are offline. Changes are saved locally.";
            }

            settingsDao.setLastSync(userId, DateUtils.nowIso());
            if (!result.noNetwork) {
                if (result.failedPushes > 0) {
                    result.success = false;
                    result.message = "Sync completed with " + result.failedPushes + " failed upload"
                            + (result.failedPushes == 1 ? "" : "s")
                            + ". Changes saved locally.";
                    result.partialSuccess = true;
                } else if (result.pushed == 0 && result.pulled == 0) {
                    result.success = true;
                    result.message = "Everything is up to date";
                } else {
                    result.success = true;
                    result.message = "Synced " + (result.pushed + result.pulled) + " change"
                            + ((result.pushed + result.pulled) == 1 ? "" : "s");
                }
            }
        } catch (SupabaseClient.UnauthorizedException e) {
            SyncResult unauthorized = SyncResult.unauthorized();
            unauthorized.pending = settingsDao.queueCount(userId);
            broadcast(unauthorized);
            broadcastAuthExpired();
            return unauthorized;
        } catch (IOException e) {
            result.noNetwork = true;
            result.interrupted = true;
            result.message = "You are offline. Changes are saved locally.";
        } catch (SupabaseClient.ApiException e) {
            result.success = false;
            result.message = "Sync failed: " + e.getMessage();
        } catch (Exception e) {
            result.success = false;
            result.message = "Sync failed: " + (e.getMessage() == null ? "unexpected error" : e.getMessage());
        }

        result.pending = settingsDao.queueCount(userId);
        result.conflicts = newConflicts;
        broadcast(result);
        return result;
    }

    private PullOutcome pullTable(String table, String userId, String token,
                                  Set<String> queued, List<Deferred> deferred) throws IOException,
            SupabaseClient.ApiException {
        PullOutcome outcome = new PullOutcome();
        String cursor = settingsDao.getLastPulledAt(userId, table);
        String maxUpdated = null;
        boolean hadDeferred = false;
        int offset = 0;
        while (true) {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*");
            params.put("user_id", "eq." + userId);
            params.put("order", "updated_at.asc");
            params.put("limit", String.valueOf(PAGE_SIZE));
            params.put("offset", String.valueOf(offset));
            if (cursor != null) {
                params.put("updated_at", "gt." + cursor);
            }
            JSONArray rows = client.select(table, params, token);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String recordId = row.optString("id");
                String serverUpdated = row.optString("updated_at");
                if (serverUpdated != null && !serverUpdated.isEmpty()) {
                    if (maxUpdated == null
                            || DateUtils.parseInstant(serverUpdated).isAfter(DateUtils.parseInstant(maxUpdated))) {
                        maxUpdated = serverUpdated;
                    }
                }
                String recordKey = key(table, recordId);
                boolean isQueued = queued.contains(recordKey);
                if (isQueued) {
                    String localUpdated = localUpdatedAt(table, recordId);
                    boolean serverNewer = DateUtils.parseInstant(serverUpdated)
                            .isAfter(DateUtils.parseInstant(localUpdated));
                    if (!serverNewer) {
                        continue;
                    }
                    JSONObject local = readLocal(table, recordId);
                    if (applyRemote(table, row)) {
                        settingsDao.logConflict(userId, table, recordId,
                                local == null ? "{}" : local.toString(), row.toString());
                        settingsDao.dequeue(queueId(table, recordId));
                        queued.remove(recordKey);
                        outcome.conflicts++;
                        outcome.applied++;
                    } else {
                        hadDeferred = true;
                        deferred.add(new Deferred(table, row, true,
                                local == null ? "{}" : local.toString()));
                    }
                } else {
                    if (applyRemote(table, row)) {
                        outcome.applied++;
                    } else {
                        hadDeferred = true;
                        deferred.add(new Deferred(table, row, false, "{}"));
                    }
                }
            }
            if (rows.length() < PAGE_SIZE) {
                break;
            }
            offset += rows.length();
        }
        if (!hadDeferred && maxUpdated != null) {
            settingsDao.setLastPulledAt(userId, table, maxUpdated);
        }
        return outcome;
    }

    private PushOutcome pushAll(String userId, String token, SyncResult result) throws IOException,
            SupabaseClient.ApiException {
        PushOutcome outcome = new PushOutcome();
        List<SyncQueueItem> items = settingsDao.queue(userId, 0);
        for (SyncQueueItem item : items) {
            JSONObject local = readLocal(item.tableName, item.recordId);
            if (local == null) {
                settingsDao.dequeue(item.id);
                continue;
            }
            JSONObject server = readServer(item.tableName, item.recordId, token);
            if (server != null) {
                String serverUpdated = server.optString("updated_at");
                String localUpdated = local.optString("updated_at");
                if (DateUtils.parseInstant(serverUpdated)
                        .isAfter(DateUtils.parseInstant(localUpdated))) {
                    settingsDao.logConflict(userId, item.tableName, item.recordId,
                            local.toString(), server.toString());
                    if (applyRemote(item.tableName, server)) {
                        settingsDao.dequeue(item.id);
                        outcome.conflicts++;
                        continue;
                    }
                    settingsDao.recordQueueFailure(item.id, "Could not apply newer server data");
                    outcome.failed++;
                    result.failedPushes++;
                    continue;
                }
            }

            JSONArray response;
            try {
                response = client.upsert(item.tableName, new JSONArray().put(local), token);
            } catch (SupabaseClient.UnauthorizedException e) {
                Session refreshed = authRepository.forceRefresh(sessionManager.getSession());
                token = refreshed.accessToken;
                response = client.upsert(item.tableName, new JSONArray().put(local), token);
            } catch (IOException e) {
                outcome.interrupted = true;
                outcome.failed++;
                result.failedPushes++;
                return outcome;
            } catch (SupabaseClient.ApiException e) {
                settingsDao.recordQueueFailure(item.id, e.getMessage());
                outcome.failed++;
                result.failedPushes++;
                if (e.statusCode >= 500 || e.statusCode == 429) {
                    return outcome;
                }
                continue;
            }

            String serverUpdated = local.optString("updated_at");
            if (response.length() > 0) {
                JSONObject first = response.optJSONObject(0);
                if (first != null) {
                    String value = first.optString("updated_at");
                    if (value != null && !value.isEmpty()) {
                        serverUpdated = value;
                    }
                }
            }
            updateLocalUpdatedAt(item.tableName, item.recordId, serverUpdated);
            settingsDao.dequeue(item.id);
            outcome.pushed++;
        }
        return outcome;
    }

    private boolean applyRemote(String table, JSONObject row) {
        try {
            ContentValues values = SyncMapper.fromCloud(table, row);
            SQLiteDatabase db = helper.getWritableDatabase();
            int updated = db.update(table, values, "id=?", new String[]{row.optString("id")});
            if (updated == 0) {
                db.insertOrThrow(table, null, values);
            }
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private JSONObject readLocal(String table, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM " + table + " WHERE id=?", new String[]{id})) {
            if (!c.moveToFirst()) {
                return null;
            }
            return SyncMapper.toCloud(table, c);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject readServer(String table, String id, String token) throws IOException,
            SupabaseClient.ApiException {
        Map<String, String> params = new HashMap<>();
        params.put("select", "*");
        params.put("id", "eq." + id);
        params.put("limit", "1");
        JSONArray rows = client.select(table, params, token);
        return rows.length() == 0 ? null : rows.optJSONObject(0);
    }

    private String localUpdatedAt(String table, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT updated_at FROM " + table + " WHERE id=?",
                new String[]{id})) {
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void updateLocalUpdatedAt(String table, String id, String updatedAt) {
        if (updatedAt == null || updatedAt.isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("updated_at", updatedAt);
        helper.getWritableDatabase().update(table, values, "id=?", new String[]{id});
    }

    private void logConflict(String userId, String table, JSONObject row, String localPayload) {
        settingsDao.logConflict(userId, table, row.optString("id"), localPayload, row.toString());
    }

    private String queueId(String table, String recordId) {
        return Ids.deterministic("queue:" + table + ":" + recordId);
    }

    private String key(String table, String recordId) {
        return table + "|" + recordId;
    }

    private void broadcast(SyncResult result) {
        Intent intent = new Intent(Constants.ACTION_SYNC_STATUS);
        intent.setPackage(app.getPackageName());
        intent.putExtra(Constants.EXTRA_SYNC_SUCCESS, result.success);
        intent.putExtra(Constants.EXTRA_SYNC_MESSAGE, result.message);
        app.sendBroadcast(intent);
    }

    private void broadcastAuthExpired() {
        Intent intent = new Intent(Constants.ACTION_AUTH_EXPIRED);
        intent.setPackage(app.getPackageName());
        app.sendBroadcast(intent);
    }

    private static class PullOutcome {
        int applied;
        int conflicts;
    }

    private static class PushOutcome {
        int pushed;
        int conflicts;
        int failed;
        boolean interrupted;
    }

    private static class Deferred {
        final String table;
        final JSONObject row;
        final boolean conflict;
        final String localPayload;

        Deferred(String table, JSONObject row, boolean conflict, String localPayload) {
            this.table = table;
            this.row = row;
            this.conflict = conflict;
            this.localPayload = localPayload;
        }
    }
}
