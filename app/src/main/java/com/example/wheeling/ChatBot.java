package com.example.wheeling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatBot {
    private final List<String> replies;
    private int currentIndex = 0;

    public ChatBot() {
        // TODO: Customize these 10–15 replies however you like
        replies = new ArrayList<>(Arrays.asList(
                "Hello there!",
                "How can I assist you today?",
                "I’m here to help.",
                "Can you tell me more?",
                "Interesting!",
                "Got it.",
                "Let me check that for you.",
                "One moment please…",
                "Here’s what I found.",
                "Does that answer your question?",
                "Feel free to ask anything else.",
                "Happy to help!",
                "Thanks for chatting with me.",
                "Have a great day!",
                "Goodbye!"
        ));
    }

    /**
     * Returns the next reply in sequence, or "No more replies"
     * once the list is exhausted.
     */
    public String getNextReply() {
        if (currentIndex < replies.size()) {
            String reply = replies.get(currentIndex);
            currentIndex++;
            return reply;
        } else {
            return "No more replies";
        }
    }

    /**
     * Optional: Reset back to the start of the list.
     */
    public void reset() {
        currentIndex = 0;
    }
}
