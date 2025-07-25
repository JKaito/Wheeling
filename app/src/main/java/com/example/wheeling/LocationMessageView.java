package com.example.wheeling;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LocationMessageView extends LinearLayout {

    private TextView messageTextView;

    public LocationMessageView(Context context) {
        super(context);
        init(context);
    }

    public LocationMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocationMessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.chat_message, this, true);
        messageTextView = findViewById(R.id.message_text);
    }

    public void setMessage(String message) {
        messageTextView.setText(message);
    }

    public String getMessage() {
        return messageTextView.getText().toString();
    }
}
