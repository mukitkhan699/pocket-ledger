package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.Loan;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.util.ArrayList;
import java.util.List;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.Holder> {

    public interface Listener {
        void onClick(Loan loan);
    }

    private final List<Loan> items = new ArrayList<>();
    private final Listener listener;

    public LoanAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Loan> loans) {
        items.clear();
        items.addAll(loans);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Loan loan = items.get(position);
        holder.initial.setText(initial(loan.personName));
        holder.person.setText(loan.personName);
        holder.meta.setText((loan.isBorrowed() ? "Borrowed · " : "Lent · ")
                + DateUtils.dateForDisplay(loan.loanDate));
        holder.amount.setText(Money.format(loan.principalMinor));
        holder.remaining.setText("Remaining " + Money.format(Math.max(0, loan.remainingMinor)));
        holder.remaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                loan.isBorrowed() ? R.color.expense : R.color.income));
        int progress = loan.principalMinor <= 0 ? 0
                : (int) Math.min(100, Math.round(loan.repaidMinor * 100.0 / loan.principalMinor));
        holder.progress.setProgress(progress);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(loan);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String initial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView initial;
        final TextView person;
        final TextView meta;
        final TextView amount;
        final TextView remaining;
        final ProgressBar progress;

        Holder(@NonNull View itemView) {
            super(itemView);
            initial = itemView.findViewById(R.id.loanInitial);
            person = itemView.findViewById(R.id.loanPerson);
            meta = itemView.findViewById(R.id.loanMeta);
            amount = itemView.findViewById(R.id.loanAmount);
            remaining = itemView.findViewById(R.id.loanRemaining);
            progress = itemView.findViewById(R.id.loanProgress);
        }
    }
}
