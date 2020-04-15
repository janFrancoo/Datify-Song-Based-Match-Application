package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String chatName = getIntent().getStringExtra("chatName");
        if (chatName != null) {
            TextView textView = findViewById(R.id.tempChatName);
            textView.setText(chatName);
        }
    }
}
