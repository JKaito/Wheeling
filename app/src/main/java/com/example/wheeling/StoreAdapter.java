package com.example.wheeling;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final List<Store> stores;

    public StoreAdapter(List<Store> stores) {
        this.stores = stores;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.result_card_item, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = stores.get(position);
        holder.placeName.setText(store.getName());
        holder.status.setText("Open - Closes at 00:00"); // placeholder

        // Accessibility color logic
        if (holder.accessibilityBar != null) {
            holder.accessibilityBar.setBackgroundColor(getAccessibilityColor(store));
        }

        // Image placeholders
        holder.imageRow.removeAllViews();
        for (int i = 0; i < 5; i++) {
            ImageView image = new ImageView(holder.itemView.getContext());
            image.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageResource(R.drawable.storeimage); // Replace with Glide if needed
            holder.imageRow.addView(image);
        }
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    // Color logic based on accessibility fields
    private int getAccessibilityColor(Store store) {
        boolean entrance = store.isEntranceAccessible();
        boolean proximity = store.isProximityAccessible();
        boolean restroom = store.isHasAccessibleRestroom();

        if (entrance && proximity && restroom) {
            return Color.parseColor("#4CAF50"); // Green
        } else if (!entrance && !proximity && !restroom) {
            return Color.parseColor("#F44336"); // Red
        } else {
            return Color.parseColor("#FFC107"); // Orange
        }
    }

    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, status;
        LinearLayout imageRow;
        View accessibilityBar;

        StoreViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.place_name);
            status = itemView.findViewById(R.id.place_status);
            imageRow = itemView.findViewById(R.id.image_row);
            accessibilityBar = itemView.findViewById(R.id.accessibility_bar); // <-- make sure this matches your layout
        }
    }
}
