package com.pocketledger.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigDecimal;

public class MoneyTest {

    @Test
    public void parsesAndNormalisesAmounts() {
        assertEquals(new BigDecimal("1234.50"), Money.of("1,234.5"));
        assertEquals(new BigDecimal("100.00"), Money.of("৳100"));
        assertEquals(new BigDecimal("250.00"), Money.of("BDT 250"));
        assertEquals(new BigDecimal("137.50"), Money.of(13750L));
    }

    @Test
    public void convertsMinorUnits() {
        assertEquals(13750L, Money.parseMinor("137.50"));
        assertEquals(101L, Money.toMinor(new BigDecimal("1.005")));
        assertEquals("137.50", Money.decimal(13750L));
        assertEquals(0L, Money.toMinor(null));
    }

    @Test
    public void rejectsInvalidAmounts() {
        for (String value : new String[]{"", "  ", "-", ".", "abc"}) {
            try {
                Money.of(value);
                fail("Expected invalid amount: " + value);
            } catch (NumberFormatException expected) {
                // expected
            }
        }
    }

    @Test
    public void nullAmountIsZero() {
        assertEquals(Money.ZERO, Money.of((String) null));
    }

    @Test
    public void validatesPositiveAmounts() {
        assertTrue(Money.isValidPositive("0.01"));
        assertFalse(Money.isValidPositive("0"));
        assertFalse(Money.isValidPositive("-5"));
        assertFalse(Money.isValidPositive("abc"));
    }

    @Test
    public void formatsSignedAmounts() {
        assertEquals("+৳137.50", Money.signed(13750L, true));
        assertEquals("-৳137.50", Money.signed(-13750L, false));
        assertEquals("-৳137.50", Money.signed(13750L, false));
    }

    @Test
    public void compactsLargeAmounts() {
        assertEquals("৳1.5M", Money.compact(150000000L));
        assertEquals("৳150.0k", Money.compact(15000000L));
        assertEquals("৳500", Money.compact(50000L));
    }
}
