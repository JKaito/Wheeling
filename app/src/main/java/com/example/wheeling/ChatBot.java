package com.example.wheeling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatBot {
    // keep your old replies here as the assistant’s replies
    private final List<String> assistantReplies;
    private int assistantIndex = 0;

    // a new list for the help-seeker’s “bot” responses
    private final List<String> seekerReplies;
    private int seekerIndex = 0;

    public ChatBot() {
        assistantReplies = new ArrayList<>(Arrays.asList(
                "Help",
                "Help",
                "Help",
                "Help",
                "Help"
        ));

        // populate this with whatever you want the “seeker-side” replies to be:
        seekerReplies = new ArrayList<>(Arrays.asList(
                "Sure thing, I’m on my way!",
                "Got it, let me look into that.",
                "One sec please…",
                /* …etc… your custom messages… */
                "That’s all I’ve got.",
                "Talk soon!"
        ));
    }

    /**
     * Pulls the next reply from the appropriate list.
     *
     * @param forAssistant true ⇒ pull from assistantReplies, false ⇒ pull from seekerReplies
     */
    public String getNextReply(boolean forAssistant) {
        if (forAssistant) {
            if (assistantIndex < assistantReplies.size()) {
                return assistantReplies.get(assistantIndex++);
            } else {
                return "No more assistant replies";
            }
        } else {
            if (seekerIndex < seekerReplies.size()) {
                return seekerReplies.get(seekerIndex++);
            } else {
                return "No more seeker replies";
            }
        }
    }

    /** reset both indices if you ever want to start over */
    public void reset() {
        assistantIndex = 0;
        seekerIndex    = 0;
    }
}