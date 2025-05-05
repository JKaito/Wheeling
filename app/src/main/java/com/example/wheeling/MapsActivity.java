package com.example.wheeling;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.wheeling.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

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
                for (Map.Entry<CardView, ImageButton> entry : cardIconMap.entrySet()) {
                    entry.getKey().setCardBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                    );
                    entry.getValue().setColorFilter(null); // Reset icon tint
                }

                card.setCardBackgroundColor(
                        ColorStateList.valueOf(Color.parseColor("#379FFF"))
                );
                cardIconMap.get(card).setColorFilter(Color.WHITE);
            });
        }

        // 🔹 Preselect one by default (e.g., wheelchair)
        ((CardView) findViewById(R.id.card_wheelchair)).setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#379FFF"))
        );
        ((ImageButton) findViewById(R.id.icon_wheelchair)).setColorFilter(Color.WHITE);
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (hasLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(false);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void showUserLocation() {
        if (!hasLocationPermission()) return;

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));

                            Toast.makeText(
                                    MapsActivity.this,
                                    "Your location: " + currentLatLng.latitude + ", " + currentLatLng.longitude,
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            Toast.makeText(MapsActivity.this, "Could not get your location.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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
