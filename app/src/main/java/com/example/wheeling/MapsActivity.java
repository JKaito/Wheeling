package com.example.wheeling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 🔹 Bottom Sheet setup
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<LinearLayout> sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(true); // This allows STATE_HIDDEN
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Start hidden



        RecyclerView recyclerView = findViewById(R.id.result_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allStores = StoreList.getStores();  // Save original list
        storeAdapter = new StoreAdapter(allStores);  // Initialize adapter with full list
        recyclerView.setAdapter(storeAdapter);



        // 🔹 Find filter buttons by ID
        MaterialButton foodButton = findViewById(R.id.food_button);
        MaterialButton drinksButton = findViewById(R.id.drink_button);
        MaterialButton coffeeButton = findViewById(R.id.coffee_button);
        MaterialButton hotelsButton = findViewById(R.id.hotel_button);

        // 🔹 Shared listener to open the bottom sheet
        foodButton.setOnClickListener(view -> {
            filterAndDisplayStores("Restaurant"); // or "Food" if you name types that way
            bottomSheet.setVisibility(View.VISIBLE);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        drinksButton.setOnClickListener(view -> {
            filterAndDisplayStores("Drinks");
            bottomSheet.setVisibility(View.VISIBLE);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        coffeeButton.setOnClickListener(view -> {
            filterAndDisplayStores("Cafe");
            bottomSheet.setVisibility(View.VISIBLE);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        hotelsButton.setOnClickListener(view -> {
            filterAndDisplayStores("Hotel");
            bottomSheet.setVisibility(View.VISIBLE);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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
        cardIconMap.put(findViewById(R.id.card_wheelchair), findViewById(R.id.icon_wheelchair));
        cardIconMap.put(findViewById(R.id.card_car), findViewById(R.id.icon_car));
        cardIconMap.put(findViewById(R.id.card_walk), findViewById(R.id.icon_card_walk));
        cardIconMap.put(findViewById(R.id.card_home), findViewById(R.id.icon_home));

        // 🔹 Handle selection logic
        for (CardView card : cardIconMap.keySet()) {
            card.setOnClickListener(view -> {
                int id = card.getId();

                // ✅ Only apply travel mode logic for specific cards
                if (id != R.id.card_car && id != R.id.card_wheelchair) {
                    // For card_walk and card_home: just highlight, do nothing else
                    for (Map.Entry<CardView, ImageButton> entry : cardIconMap.entrySet()) {
                        entry.getKey().setCardBackgroundColor(ColorStateList.valueOf(Color.WHITE));
                        entry.getValue().setColorFilter(null);
                    }
                    card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#379FFF")));
                    cardIconMap.get(card).setColorFilter(Color.WHITE);
                    return;
                }

                // Reset all cards
                for (Map.Entry<CardView, ImageButton> entry : cardIconMap.entrySet()) {
                    entry.getKey().setCardBackgroundColor(ColorStateList.valueOf(Color.WHITE));
                    entry.getValue().setColorFilter(null);
                }

                // Highlight selected card
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#379FFF")));
                cardIconMap.get(card).setColorFilter(Color.WHITE);

                // 🔹 Set travel mode and color
                if (id == R.id.card_car) {
                    selectedTravelMode = "driving";
                    routeColor = Color.parseColor("#485AFF");
                } else {
                    selectedTravelMode = "walking";
                    routeColor = Color.parseColor("#EA8C00");
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

                    if ("walking".equals(selectedTravelMode)) {
                        if (hasLocationPermission()) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                               return;
                            }
                            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                                if (location != null) {
                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    fetchAccessibleRoute(origin, lastDestination);
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
            mMap.setMyLocationEnabled(true);
            enableMapInteractions(); // ✅ now only added if permission is granted
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void enableMapInteractions() {
        mMap.setOnMapClickListener(destination -> {
            lastDestination = destination;

            drawGoogleRoute(destination);

            if (!"walking".equals(selectedTravelMode)) return;

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
                mMap.clear();
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
        String modeParam = "mode=" + selectedTravelMode;
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

    private void fetchRoute(String url) {
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
                                // Remove existing route if present
                                if (currentRoute != null) {
                                    currentRoute.remove();
                                }

                                // Draw new route and keep reference
                                currentRoute = mMap.addPolyline(new PolylineOptions()
                                        .addAll(points)
                                        .color(routeColor)
                                        .width(14)
                                        .pattern(Arrays.asList(new Dash(30), new Gap(20)))
                                        .startCap(new RoundCap())
                                        .endCap(new RoundCap()));
                            });
                        }
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
                    mMap.setMyLocationEnabled(true);
                    enableMapInteractions(); // ✅ set the click listener after permission is granted
                }
                showUserLocation();
                showUserLocation();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterAndDisplayStores(String type) {
        List<Store> filtered = new java.util.ArrayList<>();
        for (Store store : allStores) {
            if (store.getType().equalsIgnoreCase(type)) {
                filtered.add(store);
            }
        }

        storeAdapter = new StoreAdapter(filtered);
        RecyclerView recyclerView = findViewById(R.id.result_recycler);
        recyclerView.setAdapter(storeAdapter);
    }

}