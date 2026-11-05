package com.pocketledger.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pocketledger.R;
import com.pocketledger.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint expenseBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint incomeBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Long> expenseValues = new ArrayList<>();
    private final List<Long> incomeValues = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    private boolean showLegend = true;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Context ctx = getContext();
        gridPaint.setColor(ContextCompat.getColor(ctx, R.color.chart_grid));
        gridPaint.setStrokeWidth(UiUtils.dp(ctx, 1));

        expenseBarPaint.setColor(ContextCompat.getColor(ctx, R.color.expense));
        incomeBarPaint.setColor(ContextCompat.getColor(ctx, R.color.income));

        labelPaint.setColor(ContextCompat.getColor(ctx, R.color.text_tertiary));
        labelPaint.setTextSize(UiUtils.dp(ctx, 10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        legendPaint.setTextSize(UiUtils.dp(ctx, 10));
        legendPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setData(List<Long> expenseValues, List<Long> incomeValues, List<String> labels) {
        this.expenseValues.clear();
        this.incomeValues.clear();
        this.labels.clear();
        if (expenseValues != null) {
            this.expenseValues.addAll(expenseValues);
        }
        if (incomeValues != null) {
            this.incomeValues.addAll(incomeValues);
        }
        if (labels != null) {
            this.labels.addAll(labels);
        }
        invalidate();
    }

    @Deprecated
    public void setData(List<Long> values, List<String> labels) {
        this.expenseValues.clear();
        this.incomeValues.clear();
        this.labels.clear();
        if (values != null) {
            this.expenseValues.addAll(values);
        }
        if (labels != null) {
            this.labels.addAll(labels);
        }
        invalidate();
    }

    public void setBarColor(int color) {
        expenseBarPaint.setColor(color);
        invalidate();
    }

    public void setIncomeBarColor(int color) {
        incomeBarPaint.setColor(color);
        invalidate();
    }

    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float legendHeight = showLegend ? UiUtils.dp(getContext(), 20) : 0;
        float labelHeight = UiUtils.dp(getContext(), 18);
        float chartBottom = height - labelHeight - legendHeight;
        float chartTop = UiUtils.dp(getContext(), 10);
        float chartHeight = Math.max(1f, chartBottom - chartTop);

        for (int i = 0; i <= 3; i++) {
            float y = chartTop + chartHeight * i / 3f;
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        if (expenseValues.isEmpty() && incomeValues.isEmpty()) {
            drawLegend(canvas, width, chartBottom + labelHeight + (legendHeight / 2f));
            return;
        }

        long max = 1L;
        for (Long value : expenseValues) {
            if (value != null && value > max) {
                max = value;
            }
        }
        for (Long value : incomeValues) {
            if (value != null && value > max) {
                max = value;
            }
        }

        int count = Math.max(expenseValues.size(), incomeValues.size());
        float slot = (float) width / count;
        float barWidth = Math.min(UiUtils.dp(getContext(), 16), slot * 0.4f);
        float radius = Math.min(barWidth / 2f, UiUtils.dp(getContext(), 4));
        int labelStep = Math.max(1, count / 7);

        for (int i = 0; i < count; i++) {
            float slotCenter = i * slot + slot / 2f;

            // Draw expense bar (left)
            if (i < expenseValues.size()) {
                long value = expenseValues.get(i) == null ? 0L : expenseValues.get(i);
                float ratio = max == 0 ? 0f : (float) value / max;
                float barHeight = Math.max(value > 0 ? UiUtils.dp(getContext(), 3) : 0, chartHeight * ratio);
                float left = slotCenter - barWidth - UiUtils.dp(getContext(), 2);
                float top = chartBottom - barHeight;
                rect.set(left, top, left + barWidth, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, expenseBarPaint);
            }

            // Draw income bar (right)
            if (i < incomeValues.size()) {
                long value = incomeValues.get(i) == null ? 0L : incomeValues.get(i);
                float ratio = max == 0 ? 0f : (float) value / max;
                float barHeight = Math.max(value > 0 ? UiUtils.dp(getContext(), 3) : 0, chartHeight * ratio);
                float left = slotCenter + UiUtils.dp(getContext(), 2);
                float top = chartBottom - barHeight;
                rect.set(left, top, left + barWidth, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, incomeBarPaint);
            }

            // Draw label
            if (i % labelStep == 0 && i < labels.size()) {
                canvas.drawText(labels.get(i), slotCenter,
                        height - UiUtils.dp(getContext(), 3) - legendHeight, labelPaint);
            }
        }

        drawLegend(canvas, width, chartBottom + labelHeight + (legendHeight / 2f));
    }

    private void drawLegend(Canvas canvas, float width, float y) {
        if (!showLegend) return;

        float legendY = y;
        float itemWidth = width / 2f;
        float swatchSize = UiUtils.dp(getContext(), 10);
        float textGap = UiUtils.dp(getContext(), 6);

        // Expense legend
        legendPaint.setColor(ContextCompat.getColor(getContext(), R.color.expense));
        float expenseX = itemWidth / 2f - (swatchSize + textGap + legendPaint.measureText("Expense")) / 2f;
        canvas.drawRect(expenseX, legendY - swatchSize / 2f, expenseX + swatchSize, legendY + swatchSize / 2f, expenseBarPaint);
        canvas.drawText("Expense", expenseX + swatchSize + textGap, legendY + legendPaint.getTextSize() / 3f, legendPaint);

        // Income legend
        legendPaint.setColor(ContextCompat.getColor(getContext(), R.color.income));
        float incomeX = itemWidth + itemWidth / 2f - (swatchSize + textGap + legendPaint.measureText("Income")) / 2f;
        canvas.drawRect(incomeX, legendY - swatchSize / 2f, incomeX + swatchSize, legendY + swatchSize / 2f, incomeBarPaint);
        canvas.drawText("Income", incomeX + swatchSize + textGap, legendY + legendPaint.getTextSize() / 3f, legendPaint);
    }
}