package com.example.wheeling;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StoreDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_STORE_NAME = "store_name";

    public static StoreDetailBottomSheetFragment newInstance(String storeName) {
        StoreDetailBottomSheetFragment f = new StoreDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORE_NAME, storeName);
        f.setArguments(args);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.detail_card_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the buttons
        MaterialButton facilityBtn = view.findViewById(R.id.tv_accessible_facility);
        MaterialButton pathBtn = view.findViewById(R.id.btn_path);

        // 2. Default text color white for both buttons (background tint will be set dynamically below)
        facilityBtn.setTextColor(Color.WHITE);
        pathBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black));
        pathBtn.setTextColor(Color.WHITE);

        // 3. Get parent LinearLayout (should be the direct parent of both buttons)
        LinearLayout parentLayout = (LinearLayout) facilityBtn.getParent();

        // 4. Create the extra info TextView for "Accessible Facility" (hidden by default)
        TextView facilityInfo = new TextView(requireContext());

        // Set padding with zero top padding to remove gap
        facilityInfo.setPadding(32, 24, 32, 24);

        // Ensure no margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);
        facilityInfo.setLayoutParams(params);

        facilityInfo.setVisibility(View.GONE);
        parentLayout.addView(facilityInfo, parentLayout.indexOfChild(facilityBtn) + 1);

        // --- Accessibility bar logic start ---

        TextView accessibilityBar = view.findViewById(R.id.accessibility_bar);

        // Assume these booleans come from the Store data, will set below
        boolean proximityAccessible = false;
        boolean entranceAccessible = false;
        boolean hasAccessibleRestroom = false;

        // 1) Look up the Store by name
        String storeName = requireArguments().getString(ARG_STORE_NAME);
        Store found = null;
        for (Store s : StoreList.getStores()) {
            if (s.getName().equals(storeName)) {
                found = s;
                break;
            }
        }
        if (found == null) return;

        // Set booleans based on the found store
        proximityAccessible = found.isProximityAccessible();
        entranceAccessible = found.isEntranceAccessible();
        hasAccessibleRestroom = found.isHasAccessibleRestroom();

        // Count how many accessibility features are available
        int accessibleCount = 0;
        if (proximityAccessible) accessibleCount++;
        if (entranceAccessible) accessibleCount++;
        if (hasAccessibleRestroom) accessibleCount++;

        // Number of inaccessible features
        int inaccessibleCount = 3 - accessibleCount;

        // Set color and text of accessibility_bar accordingly, based on accessibleCount
        int colorResId;
        if (inaccessibleCount >= 3) {
            accessibilityBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700));
            accessibilityBar.setText("INACCESSIBLE");
            colorResId = R.color.red_700;
        } else if (inaccessibleCount == 2) {
            accessibilityBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange_700));
            accessibilityBar.setText("PARTIALLY ACCESSIBLE");
            colorResId = R.color.orange_700;
        } else {
            accessibilityBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_700));
            accessibilityBar.setText("ACCESSIBLE");
            colorResId = R.color.green_700;
        }

        int color = ContextCompat.getColor(requireContext(), colorResId);

        // --- Accessibility bar logic end ---

        // Bullets list
        String[] facilityBullets = new String[]{
                "Inaccessible Building (Stairs).",
                "Inaccessible outerspace (Stairs).",
                "Inaccessible toilet.",
                "No elevator for the second floor."
        };

        // Determine bullet count based on inaccessibleCount/color (reversed)
        int bulletCount;
        if (inaccessibleCount >= 3) {
            // red: show 3 or 4 bullets randomly (more problems)
            bulletCount = 3 + (new Random().nextBoolean() ? 1 : 0);
        } else if (inaccessibleCount == 2) {
            // orange: 2 bullets
            bulletCount = 2;
        } else {
            // green: 1 bullet
            bulletCount = 1;
        }

        // Pick random unique bullets
        List<String> bulletList = new ArrayList<>(Arrays.asList(facilityBullets));
        Collections.shuffle(bulletList);
        List<String> selectedBullets = bulletList.subList(0, bulletCount);

        // Build bullet text string
        StringBuilder bulletTextBuilder = new StringBuilder();
        for (String bullet : selectedBullets) {
            bulletTextBuilder.append("• ").append(bullet).append("\n");
        }
        String bulletText = bulletTextBuilder.toString().trim();

        // Apply colors and text to facilityBtn and expandable bullet info
        facilityBtn.setBackgroundTintList(ColorStateList.valueOf(color));
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(color);
        float radiusInDp = 8f;
        float radiusInPx = radiusInDp * getResources().getDisplayMetrics().density;
        bgDrawable.setCornerRadius(radiusInPx);
        facilityInfo.setBackground(bgDrawable);        facilityInfo.setTextColor(Color.WHITE);
        facilityInfo.setText(bulletText);

        // 5. Create the extra info TextView for "Accessible Path to the Facility" (hidden by default)
        TextView pathInfo = new TextView(requireContext());
        pathInfo.setText("Example additional info for accessible path here."); // Update as needed
        pathInfo.setTextColor(Color.WHITE);
        pathInfo.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black));
        pathInfo.setPadding(32, 24, 32, 24);
        pathInfo.setVisibility(View.GONE);
        parentLayout.addView(pathInfo, parentLayout.indexOfChild(pathBtn) + 1);









        // 6. Toggle info visibility on button click
        facilityBtn.setOnClickListener(v -> {
            facilityInfo.setVisibility(facilityInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        pathBtn.setOnClickListener(v -> {
            pathInfo.setVisibility(pathInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // 2) Create a final local so it can be captured by lambdas
        final Store store = found;

        // 3) Bind views
        TextView nameView = view.findViewById(R.id.place_name);
        TextView statusView = view.findViewById(R.id.place_status);
        TextView addressView = view.findViewById(R.id.place_address);
        TextView websiteView = view.findViewById(R.id.place_website);
        MaterialButton btnDirections = view.findViewById(R.id.btn_directions);
        MaterialButton btnAssistant = view.findViewById(R.id.btn_assistant);
        MaterialButton btnCall = view.findViewById(R.id.btn_call);
        MaterialButton btnPath2 = view.findViewById(R.id.btn_path); // already defined as pathBtn, but keep if needed
        ImageView mainImageView = view.findViewById(R.id.main_image);
        LinearLayout thumbRow = view.findViewById(R.id.image_row);

        // 4) Populate fields
        nameView.setText(store.getName());
        statusView.setText(store.isProximityAccessible() ? "Accessible" : "Not Accessible");
        addressView.setText(store.getAddress());
        websiteView.setText(store.getWebsite());

        // 5) Load images
        List<String> urls = store.getImageUrls();
        if (!urls.isEmpty()) {
            Glide.with(this).load(urls.get(0)).into(mainImageView);
            thumbRow.removeAllViews();
            for (String u : urls) {
                ImageView iv = new ImageView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200, 200);
                lp.setMargins(8, 0, 8, 0);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(this).load(u).into(iv);
                thumbRow.addView(iv);
            }
        }

        // 6) Wire Directions → MapsActivity (now-public) helper
        btnDirections.setOnClickListener(v -> {
            if (requireActivity() instanceof MapsActivity) {
                ((MapsActivity) requireActivity()).showStoreMarkerAndDirections(store);
            }
            dismiss();
        });

        // 7) Wire Call → dialer
        btnCall.setOnClickListener(v -> {
            Intent dial = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + store.getPhone())
            );
            startActivity(dial);
            dismiss();
        });

        // 8) Assistant button remains as before…
    }
}
