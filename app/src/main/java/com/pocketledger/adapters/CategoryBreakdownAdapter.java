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
import com.pocketledger.models.CategoryBreakdownItem;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Money;

import java.util.List;

public class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.CategoryViewHolder> {

    private final List<CategoryBreakdownItem> items;

    public CategoryBreakdownAdapter(List<CategoryBreakdownItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryBreakdownItem item = items.get(position);
        holder.name.setText(item.name);
        holder.amount.setText(Money.format(item.amount));
        holder.icon.setImageResource(CategoryIcons.drawable("other")); // Simplified for now
        holder.icon.setColorFilter(CategoryIcons.color(item.color));
        holder.bar.setProgress((int) Math.min(100, Math.round(
                item.amount * 100.0 / item.total)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView amount;
        final ProgressBar bar;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.reportCategoryIcon);
            name = itemView.findViewById(R.id.reportCategoryName);
            amount = itemView.findViewById(R.id.reportCategoryAmount);
            bar = itemView.findViewById(R.id.reportCategoryBar);
        }
    }
}
