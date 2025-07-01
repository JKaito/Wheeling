package com.example.wheeling;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(Store store);
    }
    public interface OnDirectionClickListener {
        void onDirectionClick(Store store);
    }

    private final List<Store> stores;
    private final OnItemClickListener itemClickListener;
    private final OnDirectionClickListener directionClickListener;

    public StoreAdapter(List<Store> stores,
                        OnItemClickListener itemClickListener,
                        OnDirectionClickListener directionClickListener) {
        this.stores = stores;
        this.itemClickListener = itemClickListener;
        this.directionClickListener = directionClickListener;
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

        // Opening status
        String statusText = getOpeningStatus(store);
        holder.status.setText(statusText);

        // 1️⃣ Card tap swaps to detail
        holder.itemView.setOnClickListener(v ->
                itemClickListener.onItemClick(store)
        );

        // 2️⃣ Directions icon still launches navigation
        holder.directionsButton.setOnClickListener(v ->
                directionClickListener.onDirectionClick(store)
        );
        // Accessibility color
        if (holder.accessibilityBar != null) {
            holder.accessibilityBar.setBackgroundColor(getAccessibilityColor(store));
        }

        // Image gallery
        holder.imageRow.removeAllViews();
        for (int i = 0; i < 5; i++) {
            ImageView image = new ImageView(holder.itemView.getContext());
            image.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageResource(R.drawable.storeimage);
            holder.imageRow.addView(image);
        }

        holder.callButton.setOnClickListener(v -> {
            String phone = store.getPhone(); // assuming getPhone() returns the phone number as a String
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            v.getContext().startActivity(intent);
        });
    }

    private String getOpeningStatus(Store store) {
        Map<String, String> hours = store.getOpeningHours();
        if (hours == null || hours.isEmpty()) {
            return "No Information";
        }

        // Current day and time
        Calendar calendar = Calendar.getInstance();
        String day = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.getTime()); // e.g. "Monday"
        String timeStr = hours.get(day);

        if (timeStr == null || !timeStr.contains("–")) {
            return "No Information";
        }

        try {
            String[] parts = timeStr.split("–");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

            Date now = timeFormat.parse(new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date()));
            Date openTime = timeFormat.parse(parts[0]);
            Date closeTime = timeFormat.parse(parts[1]);

            if (now != null && openTime != null && closeTime != null) {
                if (now.after(openTime) && now.before(closeTime)) {
                    return "Open - Closes at " + parts[1];
                } else {
                    return "Closed - Opens at " + parts[0];
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "No Information";
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
        Button directionsButton;
        MaterialButton callButton;
        TextView placeName, status;
        LinearLayout imageRow;
        View accessibilityBar;

        StoreViewHolder(View itemView) {
            super(itemView);
            directionsButton = itemView.findViewById(R.id.btn_directions);
            placeName = itemView.findViewById(R.id.place_name);
            status = itemView.findViewById(R.id.place_status);
            imageRow = itemView.findViewById(R.id.image_row);
            accessibilityBar = itemView.findViewById(R.id.accessibility_bar);
            callButton = itemView.findViewById(R.id.btn_call);
        }
    }
}
