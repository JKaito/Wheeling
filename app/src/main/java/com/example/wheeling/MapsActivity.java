package com.example.wheeling;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

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
import com.google.android.gms.maps.model.Marker;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import androidx.core.splashscreen.SplashScreen;


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
    private Marker currentLocationMarker;
    private static final LatLng FIXED_STOP = new LatLng(37.442687, 24.945161);
    private List<Polyline> currentPolylines = new ArrayList<>();

    private boolean isLocationEnabledSystem() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // 1) Fast check from cached value
        if (WheelingApp.isShutdown(this)) {
            startActivity(new Intent(this, BlockedActivity.class));
            finish();
            return;
        }

        // 2) Refresh in background, then re-check when fetch completes  👈 ADD THIS
        com.google.firebase.remoteconfig.FirebaseRemoteConfig rc =
                com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance();
        rc.fetchAndActivate().addOnCompleteListener(task -> {
            if (rc.getBoolean(WheelingApp.KEY_SHUTDOWN)) {
                startActivity(new Intent(this, BlockedActivity.class));
                finish();
            }
        });


        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1) Attach the fragment once, even if chatOverlay starts GONE
        if (getSupportFragmentManager().findFragmentByTag("SCENARIO") == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chat_overlay, ScenarioPicker.newInstance(), "SCENARIO")
                    .commit();
        }

        // 2) Now grab your overlay and toggle it on button taps
        FrameLayout chatOverlay = findViewById(R.id.chat_overlay);
        CardView    cardWalk    = findViewById(R.id.chat_button);

        cardWalk.setOnClickListener(v -> {
            if (chatOverlay.getVisibility() == View.VISIBLE) {
                chatOverlay.setVisibility(View.GONE);
            } else {
                chatOverlay.setVisibility(View.VISIBLE);
                chatOverlay.bringToFront();
                chatOverlay.setClickable(true);
                chatOverlay.setFocusable(true);

                // ← NEW: highlight the nav button
                highlightNavButton(R.id.chat_button);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.chat_overlay, ScenarioPicker.newInstance(), "SCENARIO")
                        .commit();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    "A2_CHANNEL",                      // must match the ID you use when building
                    "Assistant Notifications",         // user-visible name
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for Scenario Assistant");
            nm.createNotificationChannel(channel);
        }

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





        // 6) SUGGESTIONS list (name‐only) under the search bar
        RecyclerView suggestionRecycler = findViewById(R.id.search_results);
        suggestionRecycler.setLayoutManager(new LinearLayoutManager(this));
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(name -> {
            // hide suggestions & show on map when tapped
            suggestionRecycler.setVisibility(View.GONE);
            for (Store s : allStores) {
                if (s.getName().equals(name)) {
                    showStoreMarkerAndDirections(s);
                    break;
                }
            }
        });
        suggestionRecycler.setAdapter(suggestionAdapter);
        suggestionRecycler.setVisibility(View.GONE);

        // 7) Hook up the EditText to filter
        EditText searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                if (q.isEmpty()) {
                    suggestionRecycler.setVisibility(View.GONE);
                    suggestionAdapter.updateData(Collections.emptyList());
                } else {
                    List<String> names = new ArrayList<>();
                    for (Store store : allStores) {
                        if (store.getName().toLowerCase(Locale.ROOT).contains(q)) {
                            names.add(store.getName());
                        }
                    }
                    if (names.isEmpty()) {
                        suggestionRecycler.setVisibility(View.GONE);
                    } else {
                        suggestionRecycler.setVisibility(View.VISIBLE);
                        suggestionRecycler.bringToFront();
                    }
                    suggestionAdapter.updateData(names);
                }
            }
        });

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
            if      (id == R.id.food_button) {
                filterAndDisplayStores("Food");
                filterAndDisplayStores("Restaurant");
                bottomSheet.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                return; // skip the below code
            }
            // no drinks to display
            else if (id == R.id.drink_button)  tag = "Cafe";
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SCENARIO_ASSISTANT_CHANNEL",    // unique ID
                    "Assistant Notifications",       // user‐visible name
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications to launch the Scenario Assistant");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.myLocation.setOnClickListener(v -> {
            if (!isLocationEnabledSystem()) {
                Toast.makeText(this, "Turn on Location to use this feature", Toast.LENGTH_SHORT).show();
                promptEnableLocationServices();
                return;
            }
            if (hasLocationPermission()) {
                showUserLocation();
            } else {
                ActivityCompat.requestPermissions(
                        MapsActivity.this,
                        new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
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
        cardIconMap.put(findViewById(R.id.chat_button),
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
                boolean isWalk = (id == R.id.chat_button);
                chatOverlay.setVisibility(isWalk ? View.VISIBLE : View.GONE);

                // 🔹 Always swap in a brand‑new ChatFragment when Walk is selected
                           if (isWalk) {
                           getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.chat_overlay, ScenarioPicker.newInstance(), "SCENARIO")
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
        handleIncomingIntent(getIntent());
    }


    //-------------------- FIREBASE --------------------

    private final android.os.Handler shutdownHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable shutdownPoll = new Runnable() {
        @Override public void run() {
            com.google.firebase.remoteconfig.FirebaseRemoteConfig rc =
                    com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance();
            rc.fetchAndActivate().addOnCompleteListener(task -> {
                if (rc.getBoolean(WheelingApp.KEY_SHUTDOWN)) {
                    startActivity(new Intent(MapsActivity.this, BlockedActivity.class));
                    finish();
                } else {
                    // poll again in 30s (match this to your fetch interval)
                    shutdownHandler.postDelayed(shutdownPoll, 30_000);
                }
            });
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // start the shutdown poller while this screen is visible
        shutdownHandler.post(shutdownPoll);
    }


    @Override
    protected void onStop() {
        super.onStop();
        shutdownHandler.removeCallbacks(shutdownPoll); // stop when not visible
    }
    //-------------------- FIREBASE --------------------


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);            // update the stored intent
        handleIncomingIntent(intent);
    }

    private void showStoreDetails(Store store) {
        // 1) collapse the existing sheet
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // 2) launch the modal bottom sheet with the store’s name
        StoreDetailBottomSheetFragment
                .newInstance(store.getName())
                .show(getSupportFragmentManager(), "STORE_DETAIL");
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
            // Remove any old destination pins (green/orange/red)
            if (destinationMarker != null) {
                destinationMarker.remove();
                destinationMarker = null;
            }

            lastDestination = destination;

            // Require system location services to be ON
            if (!isLocationEnabledSystem()) {
                Toast.makeText(this, "Turn on Location to draw a route", Toast.LENGTH_SHORT).show();
                promptEnableLocationServices();
                return;
            }

            // Draw Google route (your existing logic)
            drawGoogleRoute(destination);

            // For walking/wheelchair also fetch accessible overlay
            if ("walking".equals(selectedTravelMode) || "wheelchair".equals(selectedTravelMode)) {
                if (!hasLocationPermission()) return;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                        fetchAccessibleRoute(origin, destination);
                    }
                });
            }
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

    // Open the system screen to enable Location
    private void promptEnableLocationServices() {
        try {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (ActivityNotFoundException e) {
            // Extremely rare, but fallback to general settings
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void drawGoogleRoute(LatLng destination) {
        if (!hasLocationPermission()) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;
            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

            // ── CLEAR out any old route lines ─────────────────────────────
            runOnUiThread(() -> {
                for (Polyline pl : currentPolylines) {
                    pl.remove();
                }
                currentPolylines.clear();
            });

            OkHttpClient client = new OkHttpClient();

            if ("driving".equals(selectedTravelMode)) {
                // ── LEG 1: origin → FIXED_STOP (blue) ─────────────────────────
                String url1 = getDirectionsUrl(origin, FIXED_STOP);
                client.newCall(new Request.Builder().url(url1).build())
                        .enqueue(new DirectionsCallback(polyPoints -> {
                            runOnUiThread(() -> {
                                Polyline p = mMap.addPolyline(new PolylineOptions()
                                        .addAll(polyPoints)
                                        .width(15)
                                        .color(routeColor)  // your existing blue
                                );
                                currentPolylines.add(p);
                            });
                        }));

                // ── LEG 2: FIXED_STOP → destination (green) ────────────────────
                String url2 = getDirectionsUrl(FIXED_STOP, destination);
                client.newCall(new Request.Builder().url(url2).build())
                        .enqueue(new DirectionsCallback(polyPoints -> {
                            runOnUiThread(() -> {
                                Polyline p = mMap.addPolyline(new PolylineOptions()
                                        .addAll(polyPoints)
                                        .width(15)
                                        .color(Color.GREEN)
                                );
                                currentPolylines.add(p);
                            });
                        }));

            } else {
                // ── WALKING / WHEELCHAIR: single-leg, forced blue for wheelchair ──
                int color = "wheelchair".equals(selectedTravelMode) ? Color.BLUE : routeColor;
                String url = getDirectionsUrl(origin, destination);
                client.newCall(new Request.Builder().url(url).build())
                        .enqueue(new DirectionsCallback(polyPoints -> {
                            runOnUiThread(() -> {
                                Polyline p = mMap.addPolyline(new PolylineOptions()
                                        .addAll(polyPoints)
                                        .width(15)
                                        .color(color)
                                );
                                currentPolylines.add(p);
                            });
                        }));
            }
        });
    }

    private class DirectionsCallback implements Callback {
        private final Consumer<List<LatLng>> onSuccess;

        DirectionsCallback(Consumer<List<LatLng>> onSuccess) {
            this.onSuccess = onSuccess;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            // you can log or show an error here
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (!response.isSuccessful() || response.body() == null) return;
            try {
                JSONObject json = new JSONObject(response.body().string());
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    String poly = routes.getJSONObject(0)
                            .getJSONObject("overview_polyline")
                            .getString("points");
                    List<LatLng> pts = PolyUtil.decode(poly);
                    onSuccess.accept(pts);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
        String apiKey = getString(R.string.osm_key);
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

        // 5) SUGGESTIONS list (name‐only)
        RecyclerView suggestionRecycler = findViewById(R.id.search_results);
        suggestionRecycler.setLayoutManager(new LinearLayoutManager(this));
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(name -> {
            // when tapped: hide suggestions & show on map
            suggestionRecycler.setVisibility(View.GONE);
            for (Store s : allStores) {
                if (s.getName().equals(name)) {
                    showStoreMarkerAndDirections(s);
                    break;
                }
            }
        });
        suggestionRecycler.setAdapter(suggestionAdapter);
        suggestionRecycler.setVisibility(View.GONE);

        // 6) Hook up search_input to filter
        EditText searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);

                // 1) collect ALL matches
                List<String> matches = new ArrayList<>();
                for (Store store : allStores) {
                    if (store.getName().toLowerCase(Locale.ROOT).contains(q)) {
                        matches.add(store.getName());
                    }
                }

                // 2) take at most 5 into a fresh list
                List<String> displayList = new ArrayList<>(Math.min(matches.size(), 5));
                for (int i = 0; i < matches.size() && i < 5; i++) {
                    displayList.add(matches.get(i));
                }

                // 3) show or hide the suggestions panel
                if (displayList.isEmpty()) {
                    suggestionRecycler.setVisibility(View.GONE);
                } else {
                    suggestionRecycler.setVisibility(View.VISIBLE);
                    suggestionRecycler.bringToFront();
                }

                // 4) hand the adapter exactly 5 (or fewer) items
                suggestionAdapter.updateData(displayList);
            }
        });

    }

    private void highlightNavButton(int selectedCardId) {
        // The four cards, in the same order you wired before
        int[] cardIds = {
                R.id.card_wheelchair,
                R.id.card_car,
                R.id.chat_button,
                R.id.card_home
        };
        int[] iconIds = {
                R.id.icon_wheelchair,
                R.id.icon_car,
                R.id.icon_card_walk,
                R.id.icon_home
        };

        for (int i = 0; i < cardIds.length; i++) {
            CardView card = findViewById(cardIds[i]);
            ImageButton icon = findViewById(iconIds[i]);

            if (cardIds[i] == selectedCardId) {
                // selected
                card.setCardBackgroundColor(Color.parseColor("#379FFF"));
                icon.setColorFilter(Color.WHITE);
            } else {
                // reset
                card.setCardBackgroundColor(Color.WHITE);
                icon.clearColorFilter();
            }
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if ("OPEN_ASSISTANT".equals(intent.getAction())
                && "ASSISTANT".equals(intent.getStringExtra("openFragment"))) {

            FrameLayout chatOverlay = findViewById(R.id.chat_overlay);
            chatOverlay.setVisibility(View.VISIBLE);
            chatOverlay.bringToFront();
            chatOverlay.setClickable(true);
            chatOverlay.setFocusable(true);

            // ← NEW: highlight the nav button when opened via notification
            highlightNavButton(R.id.chat_button);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.chat_overlay, ChatFragment.newInstance(true), "CHAT")
                    .commit();
        }
    }


    /** Called when the chat’s first‐bubble image is tapped. */
    public void onMinimapClicked() {
        // 1) Hide the chat overlay
        FrameLayout overlay = findViewById(R.id.chat_overlay);
        overlay.setVisibility(View.GONE);

        // 2) Highlight the wheelchair nav‐card
        highlightNavButton(R.id.card_wheelchair);

        // 3) Switch to wheelchair mode
        selectedTravelMode = "wheelchair";
        routeColor = Color.parseColor("#FF9900");

        // 4) Clear any existing routes
        if (googleRoutePolyline != null) {
            googleRoutePolyline.remove();
            googleRoutePolyline = null;
        }
        if (accessibleRoutePolyline != null) {
            accessibleRoutePolyline.remove();
            accessibleRoutePolyline = null;
        }

        // 5) Require system location to be ON
        if (!isLocationEnabledSystem()) {
            Toast.makeText(this, "Turn on Location to generate a route", Toast.LENGTH_SHORT).show();
            promptEnableLocationServices();
            return;
        }

        // 6) Permission check (keep your existing flow)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 7) Fetch current location, compute random dest ≤150m, place markers, fit bounds, draw
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                return;
            }
            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

            // Generate random destination within 150 m
            double radiusMeters = 150.0;
            double earthRadius = 6371000.0; // meters
            Random rand = new Random();
            double u = rand.nextDouble(), v = rand.nextDouble();
            double w = radiusMeters * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t), y = w * Math.sin(t);

            double newLat = origin.latitude  + (y / earthRadius) * (180.0 / Math.PI);
            double newLng = origin.longitude + (x / earthRadius) * (180.0 / Math.PI)
                    / Math.cos(origin.latitude * Math.PI / 180.0);
            LatLng dest = new LatLng(newLat, newLng);
            lastDestination = dest;

            // Origin marker (ic_my_location, ~60px)
            if (userMarker != null) userMarker.remove();
            Bitmap myLocBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location);
            Bitmap myLocSmall = Bitmap.createScaledBitmap(myLocBmp, 60, 60, true);
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(origin)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.fromBitmap(myLocSmall)));

            // Destination marker (scaled pin_green, ~36dp, anchored at tip)
            if (destinationMarker != null) destinationMarker.remove();
            float density = getResources().getDisplayMetrics().density;
            int pinW = (int) (36 * density), pinH = (int) (36 * density);
            Bitmap pinBmp = BitmapFactory.decodeResource(getResources(), R.drawable.pin_green);
            Bitmap pinSmall = Bitmap.createScaledBitmap(pinBmp, pinW, pinH, true);
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(dest)
                    .icon(BitmapDescriptorFactory.fromBitmap(pinSmall))
                    .anchor(0.5f, 1f));

            // Fit camera to show the whole route (origin + dest) with 50dp padding
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(origin)
                    .include(dest)
                    .build();
            int paddingPx = (int) (10 * density);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx));

            // Draw polylines
            drawGoogleRoute(dest);
            fetchAccessibleRoute(origin, dest);
        });
    }


    private void clearDestinationMarker() {
        if (destinationMarker != null) {
            destinationMarker.remove();
            destinationMarker = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.google.firebase.remoteconfig.FirebaseRemoteConfig rc =
                com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance();
        rc.fetchAndActivate().addOnCompleteListener(task -> {
            if (rc.getBoolean(WheelingApp.KEY_SHUTDOWN)) {
                startActivity(new Intent(this, BlockedActivity.class));
                finish();
            }
        });
    }



    public void showStoreMarkerAndDirections(Store store) {
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