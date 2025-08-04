package com.example.wheeling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SuggestionAdapter
        extends RecyclerView.Adapter<SuggestionAdapter.VH> {

    private List<String> items = new ArrayList<>();
    private final Consumer<String> onClick;

    public SuggestionAdapter(Consumer<String> onClick) {
        this.onClick = onClick;
    }

    public void updateData(List<String> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.suggestion_item, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String name = items.get(pos);
        h.text.setText(name);
        h.itemView.setOnClickListener(v -> onClick.accept(name));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        VH(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_suggestion);
        }
    }
}

