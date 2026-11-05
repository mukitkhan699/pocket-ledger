package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onClick(Transaction transaction);

        void onLongClick(Transaction transaction);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public TransactionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Transaction> transactions) {
        rows.clear();
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            String date = DateUtils.localDateOf(transaction.occurredAt).toString();
            List<Transaction> list = grouped.get(date);
            if (list == null) {
                list = new ArrayList<>();
                grouped.put(date, list);
            }
            list.add(transaction);
        }
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            long net = 0L;
            for (Transaction transaction : entry.getValue()) {
                net += transaction.isIncome() ? transaction.amountMinor : -transaction.amountMinor;
            }
            rows.add(Row.header(entry.getKey(), net));
            for (Transaction transaction : entry.getValue()) {
                rows.add(Row.transaction(transaction));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).transaction == null ? TYPE_HEADER : TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_transaction_header, parent, false));
        }
        return new TransactionHolder(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            HeaderHolder headerHolder = (HeaderHolder) holder;
            headerHolder.date.setText(DateUtils.dayHeader(LocalDate.parse(row.date)));
            headerHolder.total.setText(Money.signed(row.totalMinor, row.totalMinor >= 0));
            headerHolder.total.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    row.totalMinor >= 0 ? R.color.income : R.color.expense));
        } else if (holder instanceof TransactionHolder) {
            TransactionHolder transactionHolder = (TransactionHolder) holder;
            Transaction transaction = row.transaction;
            transactionHolder.description.setText(transaction.description);
            transactionHolder.category.setText(transaction.categoryName == null
                    ? "Uncategorised" : transaction.categoryName);
            transactionHolder.amount.setText(Money.signed(transaction.amountMinor, transaction.isIncome()));
            transactionHolder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    transaction.isIncome() ? R.color.income : R.color.expense));
            transactionHolder.time.setText(DateUtils.timeForDisplay(transaction.occurredAt));
            transactionHolder.icon.setImageResource(CategoryIcons.drawable(transaction.categoryIcon));
            transactionHolder.icon.setColorFilter(CategoryIcons.color(transaction.categoryColor));
            transactionHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(transaction);
                }
            });
            transactionHolder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(transaction);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class TransactionHolder extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView category;
        final TextView amount;
        final TextView time;
        final android.widget.ImageView icon;

        TransactionHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.txDescription);
            category = itemView.findViewById(R.id.txCategory);
            amount = itemView.findViewById(R.id.txAmount);
            time = itemView.findViewById(R.id.txTime);
            icon = itemView.findViewById(R.id.txIcon);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView total;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.headerDate);
            total = itemView.findViewById(R.id.headerTotal);
        }
    }

    private static class Row {
        final String date;
        final long totalMinor;
        final Transaction transaction;

        private Row(String date, long totalMinor, Transaction transaction) {
            this.date = date;
            this.totalMinor = totalMinor;
            this.transaction = transaction;
        }

        static Row header(String date, long totalMinor) {
            return new Row(date, totalMinor, null);
        }

        static Row transaction(Transaction transaction) {
            return new Row(null, 0L, transaction);
        }
    }
}
