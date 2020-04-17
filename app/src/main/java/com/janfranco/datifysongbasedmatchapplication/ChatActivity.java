package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private String chatName;
    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private Button sendMessageBtn;
    private EditText messageInput;
    private User user;
    private ArrayList<ChatMessage> messages;
    private MessageListRecyclerAdapter messageListAdapter;
    private RecyclerView messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatName = getIntent().getStringExtra("chatName");
        messageInput = findViewById(R.id.messageInput);
        sendMessageBtn = findViewById(R.id.sendMessageBtn);
        sendMessageBtn.setEnabled(false);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0)
                    sendMessageBtn.setEnabled(true);
                else
                    sendMessageBtn.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };
        messageInput.addTextChangedListener(textWatcher);

        messageList = findViewById(R.id.messageList);
        messageList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        messages = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        CurrentUser currentUser = CurrentUser.getInstance();
        user = currentUser.getUser();

        getMessages();
        messageListAdapter = new MessageListRecyclerAdapter(user.getUsername(), messages);
        messageList.setAdapter(messageListAdapter);

        if (chatName != null) {
            // ToDo: Add header to chat (avatar, username, user settings/details etc
        }

        listen();

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString().trim();
                sendMessage(message);
                messageInput.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (registration != null)
            registration.remove();
    }

    private void listen() {
        if (chatName == null)
            return;

        final DocumentReference ref = db.collection("chat").document(chatName);
        registration = ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(ChatActivity.this, "Real time listening error!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (snapshot != null && snapshot.exists() && snapshot.getData() != null) {
                    Chat updatedChat = snapshot.toObject(Chat.class);
                    if (updatedChat != null) {
                        ArrayList<ChatMessage> fromDb = updatedChat.getMessages();
                        messages.clear();
                        messages.addAll(updatedChat.getMessages());
                        messageListAdapter.notifyDataSetChanged();
                    }
                    else
                        Toast.makeText(ChatActivity.this, "lastMessage null error!",
                                Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendMessage(final String message) {
        final ChatMessage chatMessage = new ChatMessage(user.getUsername(), message);
        db.collection("chat").document(chatName).update(
                "messages",
                FieldValue.arrayUnion(chatMessage)
        )
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                       Toast.makeText(ChatActivity.this, "Error while sending message!",
                               Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getMessages() {
        db.collection("chat").document(chatName).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snapshot) {
                        Chat chat = snapshot.toObject(Chat.class);
                        if (chat != null) {
                            messages.clear();
                            ArrayList<ChatMessage> fromDb = chat.getMessages();
                            messages.addAll(fromDb);
                            messageListAdapter.notifyDataSetChanged();
                        }
                        else
                            Toast.makeText(ChatActivity.this, "Firebase error while getting messages!",
                                    Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Error while loading messages.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}
