package com.example.wheeling;

import java.util.List;
import java.util.Map;
public class Store {
    private String name;
    private String type;
    private double latitude;
    private double longitude;

    private String phone;
    private String address;
    private String website;

    private boolean proximityAccessible;     // Accessibility from current location
    private boolean entranceAccessible;      // Step-free, wide entrance
    private boolean hasAccessibleRestroom;   // Accessible toilets
    private List<String> imageUrls;
    private Map<String, String> openingHours; // e.g., {"Monday": "09:00–17:00"}

    // Constructor
    public Store(String name, String type, double latitude, double longitude, String phone, String address, String website, boolean proximityAccessible, boolean entranceAccessible, boolean hasAccessibleRestroom, List<String> imageUrls, Map<String, String> openingHours) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.address = address;
        this.website = website;
        this.proximityAccessible = proximityAccessible;
        this.entranceAccessible = entranceAccessible;
        this.hasAccessibleRestroom = hasAccessibleRestroom;
        this.imageUrls = imageUrls;
        this.openingHours = openingHours;
    }

    // Getters and setters (you can generate these automatically in Android Studio)
    public String getName() { return name; }
    public String getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getWebsite() { return website; }
    public boolean isProximityAccessible() { return proximityAccessible; }
    public boolean isEntranceAccessible() { return entranceAccessible; }
    public boolean isHasAccessibleRestroom() { return hasAccessibleRestroom; }
    public List<String> getImageUrls() { return imageUrls; }
    public Map<String, String> getOpeningHours() { return openingHours; }
}