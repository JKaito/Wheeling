package com.example.wheeling;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ScenarioPicker extends Fragment {
    private static final int NOTIF_REQUEST_CODE = 3001;

    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            postAssistantNotification();
                        }
                    }
            );

    public static ScenarioPicker newInstance() {
        return new ScenarioPicker();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scenario_picker,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnChat      = view.findViewById(R.id.btn_scenario_chat);
        Button btnAssistant = view.findViewById(R.id.btn_scenario_assistant);

        FragmentManager fm = requireActivity().getSupportFragmentManager();

        btnChat.setOnClickListener(v -> {
            fm.beginTransaction()
                    .replace(R.id.chat_overlay,
                            ChatFragment.newInstance(),
                            "CHAT")
                    .commit();
        });

        btnAssistant.setOnClickListener(v -> {
            Context ctx = requireContext();
            Log.d("ScenarioPicker", "btnAssistant clicked");

            // Build the PendingIntent
            Intent intent = new Intent(ctx, MapsActivity.class)
                    .setAction("OPEN_ASSISTANT")
                    .putExtra("openFragment", "ASSISTANT");
            PendingIntent pi = PendingIntent.getActivity(
                    ctx, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Log.d("ScenarioPicker", "PendingIntent built");

            // Build the notification
            NotificationCompat.Builder notif = new NotificationCompat.Builder(ctx, "A2_CHANNEL")
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentTitle("Trou")
                    .setContentText("I need help!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            // Check app‑level notification settings
            boolean enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled();

            // Guard for runtime POST_NOTIFICATIONS on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int perm = ContextCompat.checkSelfPermission(ctx,
                        Manifest.permission.POST_NOTIFICATIONS);
                if (perm == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(ctx)
                            .notify(NOTIF_REQUEST_CODE, notif.build());
                } else {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                NotificationManagerCompat.from(ctx)
                        .notify(NOTIF_REQUEST_CODE, notif.build());
            }
        });
    }

    private void postAssistantNotification() {
        Context ctx = requireContext();

        Intent intent = new Intent(ctx, MapsActivity.class)
                .setAction("OPEN_ASSISTANT")
                .putExtra("openFragment", "ASSISTANT")
                // ensure we reuse or restart MapsActivity with our extras
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notif = new NotificationCompat.Builder(ctx, "A2_CHANNEL")
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("")
                .setContentText("I need of help!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(requireContext())
                        .notify(NOTIF_REQUEST_CODE, notif.build());
            } else {
                // fire off the permission dialog owned by this fragment
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // older Android: no runtime notif permission needed
            NotificationManagerCompat.from(requireContext())
                    .notify(NOTIF_REQUEST_CODE, notif.build());
        }
    }
}
