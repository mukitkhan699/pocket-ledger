package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.MealEntry;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.Money;

import java.util.ArrayList;
import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.Holder> {

    public interface Listener {
        void onConsume(MealEntry entry);

        void onSkip(MealEntry entry);

        void onEdit(MealEntry entry);
    }

    private final List<MealEntry> items = new ArrayList<>();
    private final Listener listener;

    public MealAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<MealEntry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MealEntry entry = items.get(position);
        holder.label.setText(entry.label());
        holder.amount.setText(Money.format(entry.amountMinor));
        holder.icon.setImageResource(CategoryIcons.drawable(entry.iconKey()));
        if (entry.isConsumed()) {
            holder.state.setText(R.string.meal_consumed);
            holder.state.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.consumed));
            holder.state.setBackgroundResource(R.drawable.bg_status_consumed);
        } else if (entry.isSkipped()) {
            holder.state.setText(R.string.meal_skipped);
            holder.state.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.skipped));
            holder.state.setBackgroundResource(R.drawable.bg_status_skipped);
        } else {
            holder.state.setText(R.string.meal_planned);
            holder.state.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.planned));
            holder.state.setBackgroundResource(R.drawable.bg_status_planned);
        }
        holder.consume.setAlpha(entry.isConsumed() ? 0.45f : 1f);
        holder.skip.setAlpha(entry.isSkipped() ? 0.45f : 1f);
        holder.consume.setOnClickListener(v -> listener.onConsume(entry));
        holder.skip.setOnClickListener(v -> listener.onSkip(entry));
        holder.itemView.setOnClickListener(v -> listener.onEdit(entry));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView amount;
        final TextView state;
        final ImageButton consume;
        final ImageButton skip;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.mealIcon);
            label = itemView.findViewById(R.id.mealLabel);
            amount = itemView.findViewById(R.id.mealAmount);
            state = itemView.findViewById(R.id.mealState);
            consume = itemView.findViewById(R.id.mealConsume);
            skip = itemView.findViewById(R.id.mealSkip);
        }
    }
}
