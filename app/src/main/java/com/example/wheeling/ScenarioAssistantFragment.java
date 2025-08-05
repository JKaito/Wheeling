package com.example.wheeling;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ScenarioAssistantFragment extends Fragment {
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText input;
    private ImageButton sendBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scenario_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // wire up your views exactly as they're named in the XML
        chatScroll    = view.findViewById(R.id.chat_scroll_user);
        chatContainer = view.findViewById(R.id.chat_container_user);
        input         = view.findViewById(R.id.chat_input_user);
        sendBtn       = view.findViewById(R.id.send_button_user);

        // preload your three assistant messages
        addMessageToChat("Hello there!", false);
        addMessageToChat("How can I assist you today?", false);
        addMessageToChat("I’m here to help.", false);

        // make Send actually work
        sendBtn.setOnClickListener(v -> {
            Log.d("ScenarioFrag", "Send clicked!");
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(), "Sending: " + msg, Toast.LENGTH_SHORT).show();
            addMessageToChat(msg, true);
            input.setText("");
            // hide keyboard
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        });

        // also let the keyboard’s Send button fire it
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendBtn.performClick();
                return true;
            }
            return false;
        });
    }

    /** Inflate & insert a bubble, then scroll to bottom. */
    private void addMessageToChat(String text, boolean isUser) {
        int layoutId = isUser
                ? R.layout.chat_message       // your “orange” user bubble
                : R.layout.chat_message_user; // your “grey” assistant bubble

        View bubble = LayoutInflater.from(requireContext())
                .inflate(layoutId, chatContainer, false);

        TextView tv = bubble.findViewById(R.id.message_text);
        tv.setText(text);

        // align it left or right
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) bubble.getLayoutParams();
        lp.gravity = isUser ? Gravity.END : Gravity.START;
        bubble.setLayoutParams(lp);

        chatContainer.addView(bubble);
        // and scroll down
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}
