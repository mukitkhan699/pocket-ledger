package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.ReportSummary;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Money;

import java.util.ArrayList;
import java.util.List;

public class ReportCategoryAdapter extends RecyclerView.Adapter<ReportCategoryAdapter.Holder> {

    private final List<ReportSummary.CategoryTotal> items = new ArrayList<>();
    private long totalMinor = 1L;

    public void setItems(List<ReportSummary.CategoryTotal> totals, long totalMinor) {
        items.clear();
        items.addAll(totals);
        this.totalMinor = Math.max(1L, totalMinor);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ReportSummary.CategoryTotal total = items.get(position);
        holder.name.setText(total.name);
        holder.amount.setText(Money.format(total.amountMinor));
        holder.icon.setImageResource(CategoryIcons.drawable(total.icon));
        holder.icon.setColorFilter(CategoryIcons.color(total.color));
        holder.bar.setProgress((int) Math.min(100,
                Math.round(total.amountMinor * 100.0 / totalMinor)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView amount;
        final ProgressBar bar;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.reportCategoryIcon);
            name = itemView.findViewById(R.id.reportCategoryName);
            amount = itemView.findViewById(R.id.reportCategoryAmount);
            bar = itemView.findViewById(R.id.reportCategoryBar);
        }
    }
}
