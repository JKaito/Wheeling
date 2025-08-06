package com.example.wheeling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ScenarioAssistant extends Fragment {
    /**
     * Factory method — always use this to get a fresh instance.
     */
    public static ScenarioAssistant newInstance() {
        return new ScenarioAssistant();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout you already made: chat_activity
        return inflater.inflate(R.layout.chat_activity, container, false);
    }
}
