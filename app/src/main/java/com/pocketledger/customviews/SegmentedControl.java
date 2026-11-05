package com.pocketledger.customviews;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pocketledger.R;
import com.pocketledger.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class SegmentedControl extends LinearLayout {

    public interface OnSelectionChanged {
        void onChanged(int index, String value);
    }

    private final List<TextView> buttons = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private int selectedIndex = 0;
    private OnSelectionChanged listener;

    public SegmentedControl(Context context) {
        this(context, null);
    }

    public SegmentedControl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setBackground(ContextCompat.getDrawable(context, R.drawable.bg_segmented_group));
        int padding = UiUtils.dp(context, 3);
        setPadding(padding, padding, padding, padding);
    }

    public void setItems(String[] labels, String[] newValues) {
        removeAllViews();
        buttons.clear();
        values.clear();
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView button = new TextView(getContext());
            button.setText(labels[i]);
            button.setGravity(Gravity.CENTER);
            button.setTextSize(13);
            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setClickable(true);
            button.setFocusable(true);
            LayoutParams params = new LayoutParams(0, UiUtils.dp(getContext(), 38), 1f);
            params.setMargins(UiUtils.dp(getContext(), 1), 0, UiUtils.dp(getContext(), 1), 0);
            button.setLayoutParams(params);
            button.setOnClickListener(v -> select(index, true));
            addView(button);
            buttons.add(button);
            values.add(newValues[i]);
        }
        select(selectedIndex, false);
    }

    public void setSelectedValue(String value) {
        int index = values.indexOf(value);
        if (index >= 0) {
            select(index, false);
        }
    }

    public String getSelectedValue() {
        if (values.isEmpty()) {
            return null;
        }
        return values.get(Math.max(0, Math.min(selectedIndex, values.size() - 1)));
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setOnSelectionChanged(OnSelectionChanged listener) {
        this.listener = listener;
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= buttons.size()) {
            return;
        }
        selectedIndex = index;
        for (int i = 0; i < buttons.size(); i++) {
            TextView button = buttons.get(i);
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(UiUtils.dp(getContext(), 11));
            if (i == selectedIndex) {
                background.setColor(ContextCompat.getColor(getContext(), R.color.surface_elevated));
                button.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
            } else {
                background.setColor(ContextCompat.getColor(getContext(), R.color.transparent));
                button.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
            button.setBackground(background);
        }
        if (notify && listener != null) {
            listener.onChanged(selectedIndex, getSelectedValue());
        }
    }
}
