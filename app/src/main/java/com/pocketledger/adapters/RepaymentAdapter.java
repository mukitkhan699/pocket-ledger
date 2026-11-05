package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.LoanRepayment;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.util.ArrayList;
import java.util.List;

public class RepaymentAdapter extends RecyclerView.Adapter<RepaymentAdapter.Holder> {

    public interface Listener {
        void onDelete(LoanRepayment repayment);
    }

    private final List<LoanRepayment> items = new ArrayList<>();
    private final Listener listener;

    public RepaymentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<LoanRepayment> repayments) {
        items.clear();
        items.addAll(repayments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repayment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LoanRepayment repayment = items.get(position);
        holder.date.setText(DateUtils.dateForDisplay(repayment.repaymentDate));
        holder.notes.setText(repayment.notes == null || repayment.notes.trim().isEmpty()
                ? "Repayment" : repayment.notes);
        holder.amount.setText(Money.format(repayment.amountMinor));
        holder.delete.setOnClickListener(v -> listener.onDelete(repayment));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView notes;
        final TextView amount;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.repaymentDate);
            notes = itemView.findViewById(R.id.repaymentNotes);
            amount = itemView.findViewById(R.id.repaymentAmount);
            delete = itemView.findViewById(R.id.repaymentDelete);
        }
    }
}
