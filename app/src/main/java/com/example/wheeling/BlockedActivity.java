package com.example.wheeling;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class BlockedActivity extends AppCompatActivity {

    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        progress = findViewById(R.id.progress);

        Button btnTryAgain = findViewById(R.id.btnTryAgain);
        Button btnExit     = findViewById(R.id.btnExit);

        btnTryAgain.setOnClickListener(v -> {
            // show spinner while fetching the latest flag
            progress.setVisibility(View.VISIBLE);
            btnTryAgain.setEnabled(false);

            WheelingApp.fetchAndCache(this);
            // small delay to let RC finish; you can make this fancier if you want
            v.postDelayed(() -> {
                progress.setVisibility(View.GONE);
                btnTryAgain.setEnabled(true);

                if (!WheelingApp.isShutdown(this)) {
                    // reopen your app's main screen cleanly
                    Intent i = new Intent(this, MapsActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            |Intent.FLAG_ACTIVITY_NEW_TASK
                            |Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
            }, 1200);
        });

        btnExit.setOnClickListener(v -> {
            finishAffinity(); // close the whole app/task
        });
    }

    @Override
    public void onBackPressed() {
        // Don’t allow backing into the app while shut down
        finishAffinity();
    }
}
