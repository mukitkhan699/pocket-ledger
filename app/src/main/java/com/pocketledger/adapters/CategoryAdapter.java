package com.pocketledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.models.Category;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {

    public interface Listener {
        void onEdit(Category category);

        void onDelete(Category category);
    }

    private final List<Category> items = new ArrayList<>();
    private final Listener listener;

    public CategoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Category> categories) {
        items.clear();
        items.addAll(categories);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_manage, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Category category = items.get(position);
        holder.name.setText(category.name);
        StringBuilder meta = new StringBuilder(Constants.TYPE_INCOME.equals(category.type) ? "Income" : "Expense");
        if (category.isFood) {
            meta.append(" · Food");
        }
        meta.append(category.isDefault ? " · Default" : " · Custom");
        holder.meta.setText(meta);
        holder.icon.setImageResource(CategoryIcons.drawable(category.icon));
        holder.icon.setColorFilter(CategoryIcons.color(category.color));
        holder.edit.setOnClickListener(v -> listener.onEdit(category));
        holder.delete.setVisibility(category.isDefault ? View.INVISIBLE : View.VISIBLE);
        holder.delete.setOnClickListener(v -> listener.onDelete(category));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView meta;
        final ImageButton edit;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.categoryIcon);
            name = itemView.findViewById(R.id.categoryName);
            meta = itemView.findViewById(R.id.categoryMeta);
            edit = itemView.findViewById(R.id.categoryEdit);
            delete = itemView.findViewById(R.id.categoryDelete);
        }
    }
}
