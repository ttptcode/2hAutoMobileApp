package com.example.a2hauto.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a2hauto.R;
import com.example.a2hauto.model.ItemType;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<ItemType> itemTypes;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(ItemType itemType);
    }

    public CategoryAdapter(List<ItemType> itemTypes, OnCategoryClickListener listener) {
        this.itemTypes = itemTypes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ItemType type = itemTypes.get(position);
        holder.tvName.setText(type.getName());
        holder.tvDesc.setText(type.getDescription());
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(type));
    }

    @Override
    public int getItemCount() {
        return itemTypes == null ? 0 : itemTypes.size();
    }

    public void setCategories(List<ItemType> itemTypes) {
        this.itemTypes = itemTypes;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvDesc = itemView.findViewById(R.id.tvCategoryDesc);
        }
    }
}
