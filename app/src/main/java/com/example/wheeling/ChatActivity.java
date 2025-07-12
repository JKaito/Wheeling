package com.example.wheeling;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.wheeling.R;

public class ChatActivity extends Activity {

    private ImageButton btnStairs;
    private ImageButton btnRoughRoad;
    private ImageButton btnUphill;
    private ImageButton currentlySelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        // Initialize the buttons
        btnStairs = findViewById(R.id.btn_stairs);
        btnRoughRoad = findViewById(R.id.btn_roughroad);
        btnUphill = findViewById(R.id.btn_uphill);

        // Attach click listeners
        btnStairs.setOnClickListener(buttonClickListener);
        btnRoughRoad.setOnClickListener(buttonClickListener);
        btnUphill.setOnClickListener(buttonClickListener);
    }

    private final View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(ChatActivity.this, "Clicked!", Toast.LENGTH_SHORT).show();
            ImageButton clickedButton = (ImageButton) v;

            // If clicked again, deselect
            if (currentlySelected == clickedButton) {
                resetButtonImage(clickedButton);
                currentlySelected = null;
                return;
            }

            // Deselect previous if exists
            if (currentlySelected != null) {
                resetButtonImage(currentlySelected);
            }

            // Select new one
            setSelectedImage(clickedButton);
            currentlySelected = clickedButton;
        }
    };

    // Change image to "orange" version
    private void setSelectedImage(ImageButton button) {
        if (button == btnStairs) {
            button.setImageResource(R.drawable.ic_stairs_orange);
        } else if (button == btnRoughRoad) {
            button.setImageResource(R.drawable.ic_roughroad_orange);
        } else if (button == btnUphill) {
            button.setImageResource(R.drawable.ic_uphill_orange);
        }
    }

    // Reset image to default
    private void resetButtonImage(ImageButton button) {
        if (button == btnStairs) {
            button.setImageResource(R.drawable.ic_stairs);
        } else if (button == btnRoughRoad) {
            button.setImageResource(R.drawable.ic_roughroad);
        } else if (button == btnUphill) {
            button.setImageResource(R.drawable.ic_uphill);
        }
    }
}
