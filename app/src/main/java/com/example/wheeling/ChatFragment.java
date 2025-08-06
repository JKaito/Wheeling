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

    private static final String ARG_IS_ASSISTANT = "isAssistant";
    private boolean isAssistant = false;

    /**
     * Create as a help-seeker (default).
     */
    public static ChatFragment newInstance() {
        return newInstance(false);
    }

    /**
     * @param isAssistant true to launch in assistant mode.
     */
    public static ChatFragment newInstance(boolean isAssistant) {
        ChatFragment frag = new ChatFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_ASSISTANT, isAssistant);
        frag.setArguments(args);
        return frag;
    }

    // Onboarding & reason-picker layouts
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

    // Skip/Request-help button
    private Button skipButton;
    private CharSequence skipOriginalText;
    private float buttonCornerRadius;

    // Chat-bot logic
    private ChatBot chatBot;
    private Handler botHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Always inflate the unified chat layout (with chat_input + send_button)
        return inflater.inflate(R.layout.chat_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Read role flag
        Bundle args = getArguments();
        if (args != null && args.getBoolean(ARG_IS_ASSISTANT, false)) {
            isAssistant = true;
        }

        // Bind all views
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

        // Save original Skip text & compute corner radius
        skipOriginalText = skipButton.getText();
        buttonCornerRadius = 12f * getResources().getDisplayMetrics().density;

        // Init bot & handler
        chatBot    = new ChatBot();
        botHandler = new Handler(Looper.getMainLooper());

        // If assistant: hide onboarding, show chat immediately
        if (isAssistant) {
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.GONE);
            chatScroll.setVisibility(View.VISIBLE);
            chatContainer.setVisibility(View.VISIBLE);
        } else {
            // Help-seeker path: show pickers first
            updateSkipButtonState();
            setupReasonSelection();
            setupSkipButton();
        }

        // Always wire up the send button listener
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
        // Deselect if tapping the already-selected
        if (button == selectedButton) {
            if (button == btnStairs) {
                button.setImageResource(R.drawable.ic_stairs);
            } else if (button == btnRough) {
                button.setImageResource(R.drawable.ic_roughroad);
            } else {
                button.setImageResource(R.drawable.ic_uphill);
            }
            selectedButton = null;
            updateSkipButtonState();
            return;
        }

        // Clear previous selection
        if (selectedButton != null) {
            if (selectedButton == btnStairs) {
                selectedButton.setImageResource(R.drawable.ic_stairs);
            } else if (selectedButton == btnRough) {
                selectedButton.setImageResource(R.drawable.ic_roughroad);
            } else {
                selectedButton.setImageResource(R.drawable.ic_uphill);
            }
        }

        // Highlight new selection
        if (button == btnStairs) {
            button.setImageResource(R.drawable.ic_stairs_orange);
        } else if (button == btnRough) {
            button.setImageResource(R.drawable.ic_roughroad_orange);
        } else {
            button.setImageResource(R.drawable.ic_uphill_orange);
        }
        selectedButton = button;
        updateSkipButtonState();
    }

    private void updateSkipButtonState() {
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
            // Hide onboarding
            layoutGiveLocation.setVisibility(View.GONE);
            layoutReasonPicker.setVisibility(View.GONE);
            // Show chat
            chatScroll.setVisibility(View.VISIBLE);
            chatContainer.setVisibility(View.VISIBLE);

            // Build initial user message
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
            addMessageToChat(message, true);
        });
    }

    private void setupSendLogic() {
        sendButton.setOnClickListener(v -> {
            String userText = chatInput.getText().toString().trim();
            if (userText.isEmpty()) return;

            // 1) show user message
            addMessageToChat(userText, true);
            chatInput.setText("");

            // 2) schedule bot reply
            String botReply = chatBot.getNextReply();
            botHandler.postDelayed(() -> addMessageToChat(botReply, false), 500);
        });
    }

    /**
     * @param text   The message text
     * @param isUser true if this is the “typing” side
     */
    private void addMessageToChat(String text, boolean isUser) {
        // In assistant mode, invert who shows as “user”
        boolean showAsUser = isAssistant ? !isUser : isUser;

        // Inflate correct bubble layout
        int layoutRes = showAsUser
                ? R.layout.chat_message       // orange/right
                : R.layout.chat_message_user; // grey/left
        LayoutInflater.from(getContext())
                .inflate(layoutRes, chatContainer, true);

        // Populate text
        int lastIndex = chatContainer.getChildCount() - 1;
        View msgView = chatContainer.getChildAt(lastIndex);
        TextView tv = msgView.findViewById(R.id.message_text);
        tv.setText(text);

        // Align bubble
        LinearLayout bubble = (LinearLayout) msgView;
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) bubble.getLayoutParams();
        lp.gravity = showAsUser ? Gravity.END : Gravity.START;
        bubble.setLayoutParams(lp);

        // Scroll down
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        chatScroll.setVisibility(View.VISIBLE);
    }
}
