package com.pocketledger.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String deterministic(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String categoryId(String userId, String name) {
        return deterministic("category:" + userId + ":" + name.toLowerCase());
    }

    public static String mealId(String userId, String date, String mealType) {
        return deterministic("meal:" + userId + ":" + date + ":" + mealType);
    }

    public static String mealTransactionId(String mealEntryId) {
        return deterministic("meal-transaction:" + mealEntryId);
    }
}
