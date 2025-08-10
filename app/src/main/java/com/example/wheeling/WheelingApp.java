package com.example.wheeling;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

public class WheelingApp extends Application {
    public static final String PREFS = "wheeling_rc";
    public static final String KEY_SHUTDOWN = "shutdown";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();
        rc.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(5) // use 3600 (1h) in production
                .build());

        Map<String, Object> defaults = new HashMap<>();
        defaults.put(KEY_SHUTDOWN, false);
        rc.setDefaultsAsync(defaults);

        fetchAndCache(this);
    }


    public static boolean isShutdown(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHUTDOWN, false);
    }

    public static void fetchAndCache(Context ctx) {
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();
        rc.fetchAndActivate().addOnCompleteListener(task -> {
            boolean shutdown = rc.getBoolean(KEY_SHUTDOWN);
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putBoolean(KEY_SHUTDOWN, shutdown).apply();
        });
    }
}
