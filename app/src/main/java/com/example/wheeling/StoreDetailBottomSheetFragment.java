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

        // 2. Default text color white for both buttons (background tint set dynamically below)
        facilityBtn.setTextColor(Color.WHITE);
        pathBtn.setTextColor(Color.WHITE);

        // 3. Get parent LinearLayout (should be the direct parent of both buttons)
        LinearLayout parentLayout = (LinearLayout) facilityBtn.getParent();

        // 4. Create expandable info TextView for Accessible Facility
        TextView facilityInfo = new TextView(requireContext());
        facilityInfo.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);
        facilityInfo.setLayoutParams(params);
        facilityInfo.setVisibility(View.GONE);
        parentLayout.addView(facilityInfo, parentLayout.indexOfChild(facilityBtn) + 1);

        // 5. Create expandable info TextView for Accessible Path
        TextView pathInfo = new TextView(requireContext());
        pathInfo.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pathParams.setMargins(0, 0, 0, 0);
        pathInfo.setLayoutParams(pathParams);
        pathInfo.setVisibility(View.GONE);
        parentLayout.addView(pathInfo, parentLayout.indexOfChild(pathBtn) + 1);

        // --- Accessibility bar logic start ---

        TextView accessibilityBar = view.findViewById(R.id.accessibility_bar);

        boolean proximityAccessible = false;
        boolean entranceAccessible = false;
        boolean hasAccessibleRestroom = false;

        String storeName = requireArguments().getString(ARG_STORE_NAME);
        Store found = null;
        for (Store s : StoreList.getStores()) {
            if (s.getName().equals(storeName)) {
                found = s;
                break;
            }
        }
        if (found == null) return;

        proximityAccessible = found.isProximityAccessible();
        entranceAccessible = found.isEntranceAccessible();
        hasAccessibleRestroom = found.isHasAccessibleRestroom();

        int accessibleCount = 0;
        if (proximityAccessible) accessibleCount++;
        if (entranceAccessible) accessibleCount++;
        if (hasAccessibleRestroom) accessibleCount++;

        int inaccessibleCount = 3 - accessibleCount;

        int colorResId;
        if (inaccessibleCount >= 3) {
            accessibilityBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700));
            accessibilityBar.setText("INACCESSIBLE");
            colorResId = R.color.red_700;
        } else if (inaccessibleCount == 1 || inaccessibleCount == 2) {
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

        // Facility bullets
        String[] facilityBullets = new String[]{
                "Inaccessible Building (Stairs).",
                "Inaccessible outerspace (Stairs).",
                "Inaccessible toilet.",
                "No elevator for the second floor."
        };

        // Path bullets
        String[] pathBullets = new String[]{
                "Stairs",
                "Rough road",
                "Uphill"
        };

        Random random = new Random();

        // Determine bullet count based on accessibility color:
        int facilityBulletCount;
        int pathBulletCount;

        if (colorResId == R.color.green_700) {
            facilityBulletCount = 0;
            pathBulletCount = 0;
        } else if (colorResId == R.color.orange_700) {
            // 1 or 2 bullets randomly
            facilityBulletCount = 1 + random.nextInt(2); // 1 or 2
            pathBulletCount = 1 + random.nextInt(2);
        } else { // red
            // 3 or 4 bullets for facility (max 4)
            facilityBulletCount = 3 + random.nextInt(2); // 3 or 4
            pathBulletCount = 3; // max 3 for path bullets
        }

        // Build facility bullet text
        StringBuilder facilityBulletTextBuilder = new StringBuilder();
        if (facilityBulletCount > 0) {
            List<String> facilityList = new ArrayList<>(Arrays.asList(facilityBullets));
            Collections.shuffle(facilityList);
            List<String> selectedFacilityBullets = facilityList.subList(0, Math.min(facilityBulletCount, facilityList.size()));

            for (String bullet : selectedFacilityBullets) {
                facilityBulletTextBuilder.append("• ").append(bullet).append("\n");
            }
        }
        String facilityBulletText = facilityBulletTextBuilder.toString().trim();

        // Build path bullet text
        StringBuilder pathBulletTextBuilder = new StringBuilder();
        if (pathBulletCount > 0) {
            List<String> pathList = new ArrayList<>(Arrays.asList(pathBullets));
            Collections.shuffle(pathList);
            List<String> selectedPathBullets = pathList.subList(0, Math.min(pathBulletCount, pathList.size()));

            for (String bullet : selectedPathBullets) {
                pathBulletTextBuilder.append("• ").append(bullet).append("\n");
            }
        }
        String pathBulletText = pathBulletTextBuilder.toString().trim();

        // Setup facility info background drawable with rounded corners
        GradientDrawable facilityBg = new GradientDrawable();
        facilityBg.setColor(color);
        float radiusInDp = 8f;
        float radiusInPx = radiusInDp * getResources().getDisplayMetrics().density;
        facilityBg.setCornerRadius(radiusInPx);

        // Setup path info background drawable with rounded corners
        GradientDrawable pathBg = new GradientDrawable();
        pathBg.setColor(color);
        pathBg.setCornerRadius(radiusInPx);

        // Apply colors and text to facilityBtn and facilityInfo
        facilityBtn.setBackgroundTintList(ColorStateList.valueOf(color));
        facilityInfo.setBackground(facilityBg);
        facilityInfo.setTextColor(Color.WHITE);
        if (facilityBulletCount == 0) {
            facilityInfo.setVisibility(View.GONE);
        } else {
            facilityInfo.setText(facilityBulletText);
            facilityInfo.setVisibility(View.GONE); // start hidden
        }

        // Apply colors and text to pathBtn and pathInfo
        pathBtn.setBackgroundTintList(ColorStateList.valueOf(color));
        pathInfo.setBackground(pathBg);
        pathInfo.setTextColor(Color.WHITE);
        if (pathBulletCount == 0) {
            pathInfo.setVisibility(View.GONE);
        } else {
            pathInfo.setText(pathBulletText);
            pathInfo.setVisibility(View.GONE); // start hidden
        }

        // Toggle info visibility on button clicks
        facilityBtn.setOnClickListener(v -> {
            facilityInfo.setVisibility(facilityInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        pathBtn.setOnClickListener(v -> {
            pathInfo.setVisibility(pathInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Bind other views and populate as before...
        final Store store = found;

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

        nameView.setText(store.getName());
        statusView.setText(store.isProximityAccessible() ? "Accessible" : "Not Accessible");
        addressView.setText(store.getAddress());
        websiteView.setText(store.getWebsite());

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

        btnDirections.setOnClickListener(v -> {
            if (requireActivity() instanceof MapsActivity) {
                ((MapsActivity) requireActivity()).showStoreMarkerAndDirections(store);
            }
            dismiss();
        });

        btnCall.setOnClickListener(v -> {
            Intent dial = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + store.getPhone())
            );
            startActivity(dial);
            dismiss();
        });

        // Assistant button remains as before…
    }
}
