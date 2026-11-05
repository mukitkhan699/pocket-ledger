package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.util.List;

public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions;

    public RecentTransactionsAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.description.setText(transaction.description);
        holder.category.setText(transaction.categoryName == null ? "Uncategorised" : transaction.categoryName);
        holder.amount.setText(Money.signed(transaction.amountMinor, transaction.isIncome()));
        holder.amount.setTextColor(holder.itemView.getContext().getColor(
                transaction.isIncome() ? R.color.income : R.color.expense));
        holder.time.setText(DateUtils.timeForDisplay(transaction.occurredAt));
        holder.icon.setImageResource(CategoryIcons.drawable(transaction.categoryIcon));
        holder.icon.setColorFilter(CategoryIcons.color(transaction.categoryColor));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView description;
        final TextView category;
        final TextView amount;
        final TextView time;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.txIcon);
            description = itemView.findViewById(R.id.txDescription);
            category = itemView.findViewById(R.id.txCategory);
            amount = itemView.findViewById(R.id.txAmount);
            time = itemView.findViewById(R.id.txTime);
        }
    }
}
