package com.example.wheeling;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {

    private LinearLayout layoutGiveLocation;
    private LinearLayout layoutReasonPicker;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText chatInput;
    private ImageButton sendButton;
    private ImageView locationIcon;
    private ImageButton btnStairs, btnRough, btnUphill;
    private Button skipButton;
    private ImageButton selectedButton = null; // Currently-selected reason button
    private CharSequence skipOriginalText;
    private float buttonCornerRadius;
    private Drawable skipOriginalBackground;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate your chat_activity layout
        return inflater.inflate(R.layout.chat_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        layoutGiveLocation    = view.findViewById(R.id.layout_give_location);
        layoutReasonPicker    = view.findViewById(R.id.layout_reason_picker);
        chatContainer         = view.findViewById(R.id.chat_container);
        chatScroll            = view.findViewById(R.id.chat_scroll);
        chatInput             = view.findViewById(R.id.chat_input);
        sendButton            = view.findViewById(R.id.send_button);
        locationIcon          = view.findViewById(R.id.location_icon);
        btnStairs             = view.findViewById(R.id.btn_stairs);
        btnRough              = view.findViewById(R.id.btn_roughroad);
        btnUphill             = view.findViewById(R.id.btn_uphill);
        skipButton            = view.findViewById(R.id.btn_skip_reason);
        skipOriginalText      = skipButton.getText();
        skipOriginalBackground= skipButton.getBackground();
;
        setupReasonSelection();
        setupSkipButton();
        setupSendLogic();
        skipOriginalText = skipButton.getText();
        skipButton.setBackgroundColor(Color.parseColor("#379FFF"));
        buttonCornerRadius = 12f * getResources().getDisplayMetrics().density;
        updateSkipButtonState();
    }

    private void setupReasonSelection() {
        btnStairs.setOnClickListener(v -> selectReason(btnStairs));
        btnRough.setOnClickListener(v -> selectReason(btnRough));
        btnUphill.setOnClickListener(v -> selectReason(btnUphill));

        locationIcon.setOnClickListener(v -> {
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.VISIBLE);
        });
    }

    private void updateSkipButtonState() {
        // Create a little pill‑shaped background
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(buttonCornerRadius);

        if (selectedButton != null) {
            // Orange “Request help”
            gd.setColor(Color.parseColor("#FFA723"));
            skipButton.setText("Request help");
        } else {
            // Blue “Skip”
            gd.setColor(Color.parseColor("#379FFF"));
            skipButton.setText(skipOriginalText);
        }

        skipButton.setBackground(gd);
    }

    private void selectReason(ImageButton button) {
        // 1) If tapping the already-selected button, deselect it
        if (button == selectedButton) {
            // revert its icon back to default
            if (button == btnStairs) {
                button.setImageResource(R.drawable.ic_stairs);
            } else if (button == btnRough) {
                button.setImageResource(R.drawable.ic_roughroad);
            } else if (button == btnUphill) {
                button.setImageResource(R.drawable.ic_uphill);
            }

            // clear selection
            selectedButton = null;

            // update Skip button now that nothing is selected
            updateSkipButtonState();
            return;
        }

        // 2) If there was a previous selection, revert its icon
        if (selectedButton != null) {
            if (selectedButton == btnStairs) {
                selectedButton.setImageResource(R.drawable.ic_stairs);
            } else if (selectedButton == btnRough) {
                selectedButton.setImageResource(R.drawable.ic_roughroad);
            } else if (selectedButton == btnUphill) {
                selectedButton.setImageResource(R.drawable.ic_uphill);
            }
        }

        // 3) Highlight the newly-tapped button
        if (button == btnStairs) {
            button.setImageResource(R.drawable.ic_stairs_orange);
        } else if (button == btnRough) {
            button.setImageResource(R.drawable.ic_roughroad_orange);
        } else if (button == btnUphill) {
            button.setImageResource(R.drawable.ic_uphill_orange);
        }

        // remember this as the currently-selected button
        selectedButton = button;

        // update Skip button now that something is selected
        updateSkipButtonState();
    }


    private void setupSkipButton() {
        skipButton.setOnClickListener(v -> {
            // Hide onboarding layouts
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.GONE);

            // Show chat UI
            chatScroll.setVisibility(View.VISIBLE);
            chatContainer.setVisibility(View.VISIBLE);

            // Build your message based on the selection
            String message;
            if (selectedButton == btnStairs) {
                message = "In need of help with some stairs";
            } else if (selectedButton == btnRough) {
                message = "In need of help with a rough road";
            } else if (selectedButton == btnUphill) {
                message = "In need of help with an uphill";
            } else {
                message = "Trou’s location is here";
            }

            addMessageToChat(message);
        });
    }

    private void setupSendLogic() {
        sendButton.setOnClickListener(v -> {
            String message = chatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageToChat(message);
                chatInput.setText("");
            }
        });
    }

    private void addMessageToChat(String message) {
        // Inflate your chat_message.xml directly into the container
        LayoutInflater.from(getContext())
                .inflate(R.layout.chat_message, chatContainer, true);

        // Grab the last child and set its text
        View messageView = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
        TextView tv = messageView.findViewById(R.id.message_text);
        tv.setText(message);

        // Scroll down
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        chatScroll.setVisibility(View.VISIBLE);
    }
}
