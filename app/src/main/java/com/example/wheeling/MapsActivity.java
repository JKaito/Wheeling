package com.example.wheeling;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.wheeling.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.button.MaterialButton;
import com.google.maps.android.PolyUtil;
import com.google.android.gms.maps.model.Gap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.widget.TextView;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private String selectedTravelMode = "walking";
    private LatLng lastDestination = null;
    private com.google.android.gms.maps.model.Polyline currentRoute;
    private com.google.android.gms.maps.model.Marker destinationMarker;
    private com.google.android.gms.maps.model.Marker userMarker;
    private com.google.android.gms.maps.model.Polyline googleRoutePolyline;
    private com.google.android.gms.maps.model.Polyline accessibleRoutePolyline;
    private int routeColor = Color.parseColor("#EA8C00"); // default orange for walking
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private List<Store> allStores;
    private StoreAdapter storeAdapter;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private FrameLayout chatOverlay;
    private ImageButton iconWalk;
    private boolean isChatOverlayVisible = false;
    private LinearLayout layoutGiveLocation, layoutReasonPicker;
    private ImageView locationIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1) Attach the fragment once, even if chatOverlay starts GONE
        if (getSupportFragmentManager().findFragmentByTag("CHAT") == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chat_overlay,
                            ChatFragment.newInstance(),   // ← use newInstance()
                            "CHAT")
                    .commit();
        }

        // 2) Now grab your overlay and toggle it on button taps
        FrameLayout chatOverlay = findViewById(R.id.chat_overlay);
        CardView    cardWalk    = findViewById(R.id.card_walk);

        cardWalk.setOnClickListener(v -> {
            if (chatOverlay.getVisibility() == View.VISIBLE) {
                // Just hide if it's already up
                chatOverlay.setVisibility(View.GONE);
            } else {
                // Show the overlay…
                chatOverlay.setVisibility(View.VISIBLE);

                // …and swap in a brand-new ChatFragment instance:
                FragmentManager    fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(
                        R.id.chat_overlay,            // the FrameLayout container in activity_maps.xml
                        ChatFragment.newInstance(),   // fresh instance
                        "CHAT"                        // you can use any tag you like
                );
                ft.commit();
            }
        });

        // ↓ THIS must match the view with app:layout_behavior above
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Now you can set initial state, peek height, callbacks, etc.
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        RecyclerView recyclerView = findViewById(R.id.result_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allStores = StoreList.getStores();  // Save original list
        storeAdapter = new StoreAdapter(allStores,
                this::showStoreDetails,
                this::showStoreMarkerAndDirections);
        SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        Set<String> favNames = prefs.getStringSet("favStores", new HashSet<>());

        for (Store s : allStores) {
            s.setFavourite(favNames.contains(s.getName()));
        }
        recyclerView.setAdapter(storeAdapter);

        // 🔹 Find filter buttons by ID
        MaterialButton foodButton    = findViewById(R.id.food_button);
        MaterialButton drinksButton  = findViewById(R.id.drink_button);
        MaterialButton coffeeButton  = findViewById(R.id.coffee_button);
        MaterialButton hotelsButton  = findViewById(R.id.hotel_button);
        MaterialButton museumButton  = findViewById(R.id.museum_button);
        MaterialButton publicButton  = findViewById(R.id.public_button);
        MaterialButton shopButton    = findViewById(R.id.shop_button);

        // 🔹 Shared listener to open the bottom sheet
        View.OnClickListener openSheetListener = view -> {
            String tag = null;
            int id = view.getId();
            if      (id == R.id.food_button)   tag = "Food";
            else if (id == R.id.drink_button)  tag = "Drinks";
            else if (id == R.id.coffee_button) tag = "Cafe";
            else if (id == R.id.hotel_button)  tag = "Hotel";
            else if (id == R.id.museum_button) tag = "Museum";
            else if (id == R.id.public_button) tag = "Public";
            else if (id == R.id.shop_button)   tag = "Shop";

            if (tag != null) {
                filterAndDisplayStores(tag);
                bottomSheet.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        foodButton.setOnClickListener(openSheetListener);
        drinksButton.setOnClickListener(openSheetListener);
        coffeeButton.setOnClickListener(openSheetListener);
        hotelsButton.setOnClickListener(openSheetListener);
        museumButton.setOnClickListener(openSheetListener);
        publicButton.setOnClickListener(openSheetListener);
        shopButton.setOnClickListener(openSheetListener);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.myLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                showUserLocation();
            } else {
                ActivityCompat.requestPermissions(
                        MapsActivity.this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        });

        // 🔹 Map each card to its corresponding icon
        Map<CardView, ImageButton> cardIconMap = new HashMap<>();
        cardIconMap.put(findViewById(R.id.card_wheelchair),
                findViewById(R.id.icon_wheelchair));
        cardIconMap.put(findViewById(R.id.card_car),
                findViewById(R.id.icon_car));
        cardIconMap.put(findViewById(R.id.card_walk),
                findViewById(R.id.icon_card_walk));
        cardIconMap.put(findViewById(R.id.card_home),
                findViewById(R.id.icon_home));

        ImageButton homeButton = findViewById(R.id.icon_home);
        homeButton.setOnClickListener(view -> showFavouriteBottomSheet());

        // 🔹 Handle selection logic
        for (CardView card : cardIconMap.keySet()) {
            card.setOnClickListener(view -> {
                int id = view.getId();

                // Reset all cards
                for (Map.Entry<CardView, ImageButton> entry : cardIconMap.entrySet()) {
                    entry.getKey().setCardBackgroundColor(
                            ColorStateList.valueOf(Color.WHITE));
                    entry.getValue().setColorFilter(null);
                }

                // Highlight the selected card
                card.setCardBackgroundColor(
                        ColorStateList.valueOf(Color.parseColor("#379FFF")));
                cardIconMap.get(card).setColorFilter(Color.WHITE);

                // 🔹 Set travel mode and color
                if (id == R.id.card_car) {
                    selectedTravelMode = "driving";
                    routeColor = Color.parseColor("#485AFF");
                } else if (id == R.id.card_wheelchair) {
                    selectedTravelMode = "wheelchair";
                    routeColor = Color.parseColor("#FF9900");
                } else {
                    selectedTravelMode = "walking";
                    routeColor = Color.parseColor("#EA8C00");
                }

                // Show/hide the chat overlay
                boolean isWalk = (id == R.id.card_walk);
                chatOverlay.setVisibility(isWalk ? View.VISIBLE : View.GONE);

                // 🔹 Always swap in a brand‑new ChatFragment when Walk is selected
                           if (isWalk) {
                           getSupportFragmentManager().beginTransaction()
                                       .replace(
                                           R.id.chat_overlay,
                                           ChatFragment.newInstance(),  // new instance, clears old messages
                                           "CHAT"
                                               )
                                       .commit();
                       }

                // 🚨 Clear previous routes BEFORE drawing
                if (googleRoutePolyline != null) {
                    googleRoutePolyline.remove();
                    googleRoutePolyline = null;
                }
                if (accessibleRoutePolyline != null) {
                    accessibleRoutePolyline.remove();
                    accessibleRoutePolyline = null;
                }

                // 🔁 Redraw route if a destination was selected
                if (lastDestination != null) {
                    drawGoogleRoute(lastDestination);

                    if ("walking".equals(selectedTravelMode)|| "wheelchair".equals(selectedTravelMode)) {
                        if (hasLocationPermission()) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(location -> {
                                        if (location != null) {
                                            LatLng origin = new LatLng(
                                                    location.getLatitude(),
                                                    location.getLongitude());
                                            fetchAccessibleRoute(
                                                    origin, lastDestination);
                                        }
                                    });
                        }
                    }
                }
            });
        }

        // ✅ Trigger initial selection of wheelchair card through shared logic
        CardView defaultSelectedCard = findViewById(R.id.card_wheelchair);
        defaultSelectedCard.performClick(); // This ensures it uses the same styling logic
    }



    private void showStoreDetails(Store store) {
        // 1) grab the two half-sheets
        RecyclerView results     = findViewById(R.id.result_recycler);
        View         detailCard  = findViewById(R.id.detail_container);  // ← use the include’s ID!

        // 2) swap them
        results.setVisibility(View.GONE);
        detailCard.setVisibility(View.VISIBLE);

        // 3) pull out all of your sub-views (same IDs as in detail_card_item.xml)
        TextView    nameView       = detailCard.findViewById(R.id.place_name);
        TextView    statusView     = detailCard.findViewById(R.id.place_status);
        MaterialButton btnDirections = detailCard.findViewById(R.id.btn_directions);
        MaterialButton btnAssistant  = detailCard.findViewById(R.id.btn_assistant);
        ImageButton bookmarkView   = detailCard.findViewById(R.id.bookmark_icon);
        ImageView   mainImageView  = detailCard.findViewById(R.id.main_image);
        LinearLayout thumbRow      = detailCard.findViewById(R.id.image_row);
        TextView    addressView    = detailCard.findViewById(R.id.place_address);
        TextView    websiteView    = detailCard.findViewById(R.id.place_website);
        MaterialButton btnPath       = detailCard.findViewById(R.id.btn_path);

        // 4) populate with your Store model
        nameView.setText(store.getName());
        addressView.setText(store.getAddress());
        websiteView.setText(store.getWebsite());

        // 5) wire the buttons
        btnPath      .setEnabled(store.isProximityAccessible());

        // 6) load your images (using Glide)
        List<String> urls = store.getImageUrls();
        if (!urls.isEmpty()) {
            Glide.with(this).load(urls.get(0)).into(mainImageView);
            thumbRow.removeAllViews();
            for (String u : urls) {
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200, 200);
                lp.setMargins(8, 0, 8, 0);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(this).load(u).into(iv);
                thumbRow.addView(iv);
            }
        }

        // 7) finally expand the sheet
        BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
                .setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Fallback default location
        LatLng defaultLatLng = new LatLng(37.441234, 24.940296);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 15));


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(false);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.441234, 24.940296), 15));


        if (hasLocationPermission()) {
            mMap.setMyLocationEnabled(false);
            enableMapInteractions(); // ✅ now only added if permission is granted
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void showFavouriteBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        int currentState = bottomSheetBehavior.getState();

        if (currentState == BottomSheetBehavior.STATE_EXPANDED) {
            // Sheet is open → collapse it
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            // Sheet is closed → filter and show favourites, then expand it
            List<Store> favouriteStores = new ArrayList<>();
            for (Store store : allStores) {
                if (store.isFavourite()) {
                    favouriteStores.add(store);
                }
            }

            storeAdapter.updateData(favouriteStores);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void enableMapInteractions() {
        mMap.setOnMapClickListener(destination -> {
            lastDestination = destination;

            drawGoogleRoute(destination);

            if (!( "walking".equals(selectedTravelMode) || "wheelchair".equals(selectedTravelMode) )) return;

            if (!hasLocationPermission()) return;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                    fetchAccessibleRoute(origin, destination);
                }
            });
        });
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void showUserLocation() {
        if (!hasLocationPermission()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                //mMap.clear();
                if (userMarker != null) {
                    userMarker.setPosition(currentLatLng); // update position
                } else {
                    Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location);
                    Bitmap resized = Bitmap.createScaledBitmap(original, 60, 60, false);

                    userMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.fromBitmap(resized)));
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
                Toast.makeText(this, "Your location: " + currentLatLng.latitude + ", " + currentLatLng.longitude, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Could not get your location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawGoogleRoute(LatLng destination) {
        if (!hasLocationPermission()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                String url = getDirectionsUrl(origin, destination);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().string();
                                JSONObject jsonObject = new JSONObject(json);
                                JSONArray routes = jsonObject.getJSONArray("routes");
                                if (routes.length() > 0) {
                                    String polyline = routes.getJSONObject(0)
                                            .getJSONObject("overview_polyline")
                                            .getString("points");
                                    List<LatLng> points = PolyUtil.decode(polyline);

                                    runOnUiThread(() -> {
                                        if (googleRoutePolyline != null) googleRoutePolyline.remove();
                                        googleRoutePolyline = mMap.addPolyline(new PolylineOptions()
                                                .addAll(points)
                                                .color(Color.parseColor("#485AFF")) // ← REMOVE this hardcoded color
                                                .width(14));
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String originParam = "origin=" + origin.latitude + "," + origin.longitude;
        String destParam = "destination=" + dest.latitude + "," + dest.longitude;
        String modeParam = "mode=" + (selectedTravelMode.equals("wheelchair") ? "walking" : selectedTravelMode);
        String key = getString(R.string.google_maps_key);

        return "https://maps.googleapis.com/maps/api/directions/json?" +
                originParam + "&" + destParam + "&" + modeParam + "&key=" + key;
    }

    private void fetchAccessibleRoute(LatLng origin, LatLng destination) {
        String apiKey = getString(R.string.osm_key); // assuming you defined this in google_maps_api.xml
        String url = "https://api.openrouteservice.org/v2/directions/wheelchair?api_key=" + apiKey
                + "&start=" + origin.longitude + "," + origin.latitude
                + "&end=" + destination.longitude + "," + destination.latitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray coordinates = jsonObject
                                .getJSONArray("features")
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONArray("coordinates");

                        List<LatLng> path = new java.util.ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            path.add(new LatLng(coord.getDouble(1), coord.getDouble(0)));
                        }

                        runOnUiThread(() -> {
                            if (accessibleRoutePolyline != null) {
                                accessibleRoutePolyline.remove();
                            }
                            accessibleRoutePolyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(path)
                                    .color(Color.parseColor("#EA8C00"))  // orange
                                    .width(14f)
                                    .zIndex(10f)
                                    .pattern(Arrays.asList(
                                            new Dash(30f),
                                            new Gap(20f)
                                    ))
                                    .startCap(new RoundCap())
                                    .endCap(new RoundCap())
                                    .jointType(JointType.ROUND));
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(false);
                    enableMapInteractions();
                }
                showUserLocation();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterAndDisplayStores(String... acceptedTypes) {
        List<Store> filtered = new ArrayList<>();
        List<String> targetTypes = Arrays.asList(acceptedTypes);

        for (Store store : allStores) {
            for (String tag : store.getTypes()) {
                if (targetTypes.contains(tag)) {
                    filtered.add(store);
                    break;
                }
            }
        }
        Log.d("Filter", "Filtering for: " + Arrays.toString(acceptedTypes));
        for (Store store : filtered) {
            Log.d("Match", "Matched store: " + store.getName() + " with types: " + store.getTypes());
        }

        storeAdapter = new StoreAdapter(filtered,this::showStoreDetails,this::showStoreMarkerAndDirections);
        RecyclerView recyclerView = findViewById(R.id.result_recycler);
        recyclerView.setAdapter(storeAdapter);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showStoreMarkerAndDirections(Store store) {
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        LatLng storeLatLng = new LatLng(store.getLatitude(), store.getLongitude());
        lastDestination = storeLatLng;

        // Accessibility color logic (adjust filenames as needed)
        int pinIcon;
        if (store.isEntranceAccessible() && store.isProximityAccessible() && store.isHasAccessibleRestroom()) {
            pinIcon = R.drawable.pin_green;
        } else if (!store.isEntranceAccessible() && !store.isProximityAccessible() && !store.isHasAccessibleRestroom()) {
            pinIcon = R.drawable.pin_red;
        } else {
            pinIcon = R.drawable.pin_orange;
        }

        Bitmap original = BitmapFactory.decodeResource(getResources(), pinIcon);
        Bitmap resized = Bitmap.createScaledBitmap(original, 75, 75, false);

        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(storeLatLng)
                .title(store.getName())
                .icon(BitmapDescriptorFactory.fromBitmap(resized))
        );

        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

                    drawGoogleRoute(storeLatLng);

                    if ("walking".equals(selectedTravelMode)|| "wheelchair".equals(selectedTravelMode)) {
                        fetchAccessibleRoute(origin, storeLatLng);
                    }


                    // 🔍 Fit route in view
                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    boundsBuilder.include(origin);
                    boundsBuilder.include(storeLatLng);
                    LatLngBounds bounds = boundsBuilder.build();
                    int padding = 170;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                }
            });
        }
    }
}