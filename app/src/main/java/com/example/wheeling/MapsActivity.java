package com.example.wheeling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.wheeling.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.PolyUtil;

import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.RoundCap;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private String selectedTravelMode = "walking";
    private LatLng lastDestination = null;
    private com.google.android.gms.maps.model.Polyline currentRoute;
    private com.google.android.gms.maps.model.Marker destinationMarker;
    private com.google.android.gms.maps.model.Marker userMarker;

    private int routeColor = Color.parseColor("#EA8C00"); // default orange for walking
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                // Reset all cards
                for (Map.Entry<CardView, ImageButton> entry : cardIconMap.entrySet()) {
                    entry.getKey().setCardBackgroundColor(ColorStateList.valueOf(Color.WHITE));
                    entry.getValue().setColorFilter(null);
                }

                // Highlight selected card
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#379FFF")));
                cardIconMap.get(card).setColorFilter(Color.WHITE);

                // 🔹 Set travel mode and color based on selection
                if (card.getId() == R.id.card_car) {
                    selectedTravelMode = "driving";
                    routeColor = Color.parseColor("#485AFF");
                } else {
                    selectedTravelMode = "walking";
                    routeColor = Color.parseColor("#EA8C00");
                }

                // 🔁 Re-draw the route if a destination was already selected
                if (lastDestination != null) {
                    drawRouteTo(lastDestination);
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

        if (hasLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        mMap.setOnMapClickListener(this::drawRouteTo);
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
                    Bitmap resized = Bitmap.createScaledBitmap(original, 80, 80, false);

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

    private void drawRouteTo(LatLng destination) {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        lastDestination = destination;

        // 🔹 Remove previous destination marker only
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        // 🔹 Add new destination marker
        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destination"));

        // 🔹 Keep user marker and clear only the route
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                String url = getDirectionsUrl(origin, destination);
                fetchRoute(url);
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
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
                                        .width(12)
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
                }
                showUserLocation();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
