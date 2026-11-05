package com.pocketledger.sync;

public class SyncResult {
    public boolean success;
    public String message = "";
    public int pushed;
    public int pulled;
    public int pending;
    public int conflicts;
    public int failedPushes;
    public int failedUploads;
    public boolean localOnly;
    public boolean notConfigured;
    public boolean noNetwork;
    public boolean unauthorized;
    public boolean interrupted;
    public boolean partialSuccess;
    public String lastError;

    public static SyncResult localOnly() {
        SyncResult result = new SyncResult();
        result.localOnly = true;
        result.message = "Local-only mode";
        return result;
    }

    public static SyncResult notConfigured() {
        SyncResult result = new SyncResult();
        result.notConfigured = true;
        result.message = "Cloud sync is not configured";
        return result;
    }

    public static SyncResult noNetwork() {
        SyncResult result = new SyncResult();
        result.noNetwork = true;
        result.message = "You are offline. Changes are saved locally.";
        return result;
    }

    public static SyncResult unauthorized() {
        SyncResult result = new SyncResult();
        result.unauthorized = true;
        result.message = "Your session has expired. Please sign in again.";
        return result;
    }
}
