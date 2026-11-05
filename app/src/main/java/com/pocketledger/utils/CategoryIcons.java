package com.pocketledger.utils;

import com.pocketledger.R;

import java.util.Arrays;
import java.util.List;

public final class CategoryIcons {

    public static final List<String> ICON_KEYS = Arrays.asList(
            "food", "sunrise", "moon", "transport", "shopping", "phone",
            "education", "person", "medical", "other", "income", "repayment");

    public static final List<String> COLORS = Arrays.asList(
            "#16C784", "#FF6B6B", "#F5A623", "#4A9DFF", "#A78BFA",
            "#F472B6", "#22D3EE", "#FBBF24", "#FB7185", "#94A3B8");

    private CategoryIcons() {
    }

    public static int drawable(String key) {
        if (key == null) {
            return R.drawable.ic_other;
        }
        switch (key) {
            case "food":
                return R.drawable.ic_food;
            case "sunrise":
                return R.drawable.ic_sunrise;
            case "moon":
                return R.drawable.ic_moon;
            case "transport":
                return R.drawable.ic_transport;
            case "shopping":
                return R.drawable.ic_shopping;
            case "phone":
                return R.drawable.ic_phone;
            case "education":
                return R.drawable.ic_education;
            case "person":
                return R.drawable.ic_person;
            case "medical":
                return R.drawable.ic_medical;
            case "income":
                return R.drawable.ic_income;
            case "repayment":
                return R.drawable.ic_repayment;
            case "loan":
                return R.drawable.ic_loan;
            case "meal":
                return R.drawable.ic_restaurant_menu;
            default:
                return R.drawable.ic_other;
        }
    }

    public static int color(String value) {
        try {
            return android.graphics.Color.parseColor(value);
        } catch (Exception e) {
            return android.graphics.Color.parseColor("#94A3B8");
        }
    }
}
