package com.pocketledger.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.UUID;

public class IdsTest {

    @Test
    public void deterministicIdsAreStable() {
        String first = Ids.deterministic("queue:transactions:abc");
        String second = Ids.deterministic("queue:transactions:abc");
        assertEquals(first, second);
        assertNotEquals(first, Ids.deterministic("queue:transactions:def"));
    }

    @Test
    public void generatedIdsAreValidUuids() {
        UUID.fromString(Ids.newId());
        UUID.fromString(Ids.deterministic("seed"));
        UUID.fromString(Ids.categoryId("user-1", "Food"));
        UUID.fromString(Ids.mealId("user-1", "2026-11-04", "LUNCH"));
        UUID.fromString(Ids.mealTransactionId("meal-1"));
    }

    @Test
    public void domainIdsNormaliseTheirSeed() {
        assertEquals(Ids.categoryId("user-1", "Food"), Ids.categoryId("user-1", "food"));
        assertNotEquals(Ids.categoryId("user-1", "Food"), Ids.categoryId("user-2", "Food"));
        assertTrue(Ids.mealId("u", "2026-11-04", "LUNCH")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
}
