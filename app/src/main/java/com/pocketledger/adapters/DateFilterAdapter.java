package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.pocketledger.R;

public class DateFilterAdapter extends RecyclerView.Adapter<DateFilterAdapter.DateFilterViewHolder> {

    public interface OnDateFilterSelectedListener {
        void onDateFilterSelected(int position, String filter);
    }

    private final String[] dates = {"This month", "Last month", "This year", "Last year", "Custom"};
    private int selectedPosition = 0;
    private OnDateFilterSelectedListener listener;

    public void setOnDateFilterSelectedListener(OnDateFilterSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public String getSelectedFilter() {
        return dates[selectedPosition];
    }

    @NonNull
    @Override
    public DateFilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = (Chip) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date_filter, parent, false);
        return new DateFilterViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull DateFilterViewHolder holder, int position) {
        holder.chip.setText(dates[position]);
        holder.chip.setChecked(position == selectedPosition);
        holder.chip.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition != selectedPosition) {
                selectedPosition = adapterPosition;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onDateFilterSelected(adapterPosition, dates[adapterPosition]);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.length;
    }

    static class DateFilterViewHolder extends RecyclerView.ViewHolder {
        final Chip chip;

        public DateFilterViewHolder(@NonNull Chip chip) {
            super(chip);
            this.chip = chip;
        }
    }
}