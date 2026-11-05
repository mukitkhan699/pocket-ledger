package com.pocketledger.models;

public class Session {
    public String userId;
    public String email;
    public String fullName;
    public String accessToken;
    public String refreshToken;
    public long expiresAtEpochSeconds;
    public boolean localOnly;

    public boolean isExpired() {
        return !localOnly && System.currentTimeMillis() / 1000L >= expiresAtEpochSeconds - 30;
    }

    public boolean hasCloudSession() {
        return !localOnly && accessToken != null && !accessToken.isEmpty();
    }
}
