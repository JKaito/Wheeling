package com.example.wheeling;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {

    /**
     * Always returns a brand-new ChatFragment instance
     */
    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    // Onboarding & reason‑picker layouts
    private LinearLayout layoutGiveLocation;
    private LinearLayout layoutReasonPicker;

    // Chat UI
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText chatInput;
    private ImageButton sendButton;

    // Reason buttons
    private ImageView locationIcon;
    private ImageButton btnStairs, btnRough, btnUphill;
    private ImageButton selectedButton = null;

    // Skip/Request‑help button
    private Button skipButton;
    private CharSequence skipOriginalText;
    private float buttonCornerRadius;

    // Chat‑bot logic
    private ChatBot chatBot;
    private Handler botHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate your main chat_activity layout
        return inflater.inflate(R.layout.chat_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        layoutGiveLocation = view.findViewById(R.id.layout_give_location);
        layoutReasonPicker = view.findViewById(R.id.layout_reason_picker);
        chatContainer      = view.findViewById(R.id.chat_container);
        chatScroll         = view.findViewById(R.id.chat_scroll);
        chatInput          = view.findViewById(R.id.chat_input);
        sendButton         = view.findViewById(R.id.send_button);
        locationIcon       = view.findViewById(R.id.location_icon);
        btnStairs          = view.findViewById(R.id.btn_stairs);
        btnRough           = view.findViewById(R.id.btn_roughroad);
        btnUphill          = view.findViewById(R.id.btn_uphill);
        skipButton         = view.findViewById(R.id.btn_skip_reason);

        // Capture Skip button's original text
        skipOriginalText = skipButton.getText();
        // Compute 12dp corner radius in pixels
        buttonCornerRadius =
                12f * getResources().getDisplayMetrics().density;

        // Instantiate the bot & its handler
        chatBot    = new ChatBot();
        botHandler = new Handler(Looper.getMainLooper());

        // Draw initial Skip button (blue pill)
        updateSkipButtonState();

        // Wire up all behaviors
        setupReasonSelection();
        setupSkipButton();
        setupSendLogic();
    }

    private void setupReasonSelection() {
        btnStairs.setOnClickListener(v -> selectReason(btnStairs));
        btnRough .setOnClickListener(v -> selectReason(btnRough));
        btnUphill.setOnClickListener(v -> selectReason(btnUphill));

        locationIcon.setOnClickListener(v -> {
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.VISIBLE);
        });
    }

    private void selectReason(ImageButton button) {
        // Deselect if tapping already-selected
        if (button == selectedButton) {
            if (button == btnStairs) {
                button.setImageResource(R.drawable.ic_stairs);
            } else if (button == btnRough) {
                button.setImageResource(R.drawable.ic_roughroad);
            } else if (button == btnUphill) {
                button.setImageResource(R.drawable.ic_uphill);
            }
            selectedButton = null;
            updateSkipButtonState();
            return;
        }

        // Revert previous selection
        if (selectedButton != null) {
            if (selectedButton == btnStairs) {
                selectedButton.setImageResource(R.drawable.ic_stairs);
            } else if (selectedButton == btnRough) {
                selectedButton.setImageResource(R.drawable.ic_roughroad);
            } else if (selectedButton == btnUphill) {
                selectedButton.setImageResource(R.drawable.ic_uphill);
            }
        }

        // Highlight new selection
        if (button == btnStairs) {
            button.setImageResource(R.drawable.ic_stairs_orange);
        } else if (button == btnRough) {
            button.setImageResource(R.drawable.ic_roughroad_orange);
        } else if (button == btnUphill) {
            button.setImageResource(R.drawable.ic_uphill_orange);
        }
        selectedButton = button;

        updateSkipButtonState();
    }

    private void updateSkipButtonState() {
        // Build a pill‑shaped background
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

    private void setupSkipButton() {
        skipButton.setOnClickListener(v -> {
            // Hide onboarding UI
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.GONE);

            // Show chat UI
            chatScroll.setVisibility(View.VISIBLE);
            chatContainer.setVisibility(View.VISIBLE);

            // Build the “user” message
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

            // Add it as a user message
            addMessageToChat(message, true);
        });
    }

    private void setupSendLogic() {
        sendButton.setOnClickListener(v -> {
            String userText = chatInput.getText()
                    .toString()
                    .trim();
            if (userText.isEmpty()) {
                return;
            }

            // 1) Add user message immediately
            addMessageToChat(userText, true);
            chatInput.setText("");

            // 2) Schedule bot reply after 500 ms
            String botReply = chatBot.getNextReply();
            botHandler.postDelayed(() -> {
                addMessageToChat(botReply, false);
            }, 500);
        });
    }

    /**
     * Inflate a chat_message bubble, set its text,
     * and position it left or right with appropriate color.
     *
     * @param text   The message text
     * @param isUser true if this is a user bubble (right/orange),
     *               false if a bot bubble (left/grey)
     */
    private void addMessageToChat(String text, boolean isUser) {
        // Inflate into the container
        LayoutInflater.from(getContext())
                .inflate(R.layout.chat_message, chatContainer, true);

        // Get the newly added bubble view
        int lastIndex = chatContainer.getChildCount() - 1;
        View msgView = chatContainer.getChildAt(lastIndex);

        // Set the text
        TextView tv = msgView.findViewById(R.id.message_text);
        tv.setText(text);

        // Cast the bubble root and adjust its LayoutParams
        LinearLayout bubble = (LinearLayout) msgView;
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) bubble.getLayoutParams();

        if (isUser) {
            // Align right + orange background
            lp.gravity = Gravity.END;
            bubble.setBackgroundColor(Color.parseColor("#FFA500"));
        } else {
            // Align left + light grey background
            lp.gravity = Gravity.START;
            bubble.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }
        bubble.setLayoutParams(lp);

        // Scroll to the bottom
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        chatScroll.setVisibility(View.VISIBLE);
    }
}
