package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

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
    private TextView headerUsername;
    private ImageView headerAvatar;
    private String avatarUrl;

    // ToDo: Add long click listener, if long clicked -> copy contents to input

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                messageList.scrollToPosition(messages.size() - 1);
                // ToDo: This is a temporary solution, fix when keyboard is showed
            }

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

        headerUsername = findViewById(R.id.userHeaderUsername);
        Button headerGoBackBtn = findViewById(R.id.userHeaderBackBtn);
        headerGoBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatActivity.this.finish();
            }
        });

        headerAvatar = findViewById(R.id.userHeaderAvatar);
        headerAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopUp();
            }
        });
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

        listen();

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString().trim();
                sendMessage(message);
                messageInput.setText("");
                // Hide keyboard after sending
                View view = ChatActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    assert imm != null;
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                // Hide keyboard after sending
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
                        messages.addAll(fromDb);
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
        long createDate = Timestamp.now().getSeconds();
        final ChatMessage chatMessage = new ChatMessage(user.getUsername(), message, createDate);
        db.collection("chat").document(chatName).update(
                "messages",
                FieldValue.arrayUnion(chatMessage),
                "lastMessage",
                message,
                "lastMessageDate",
                createDate
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
                            updateHeader(chat);
                            messages.clear();
                            ArrayList<ChatMessage> fromDb = chat.getMessages();
                            messages.addAll(fromDb);
                            messageListAdapter.notifyDataSetChanged();
                            messageList.scrollToPosition(messages.size() - 1);
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

    private void updateHeader(Chat chat) {
        String[] mails = chatName.split("_");
        if (mails[0].matches(user.geteMail())) {
            avatarUrl = chat.getAvatar2();
            if (!avatarUrl.matches("default"))
                Picasso.get().load(avatarUrl).into(headerAvatar);
            headerUsername.setText(chat.getUsername2());
        } else {
            avatarUrl = chat.getAvatar1();
            if (!avatarUrl.matches("default"))
                Picasso.get().load(avatarUrl).into(headerAvatar);
            headerUsername.setText(chat.getUsername1());
        }
    }

    private void createPopUp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_avatar,
                null);

        ImageView avatar = popupView.findViewById(R.id.popUpAvatar);
        if (!avatarUrl.matches("default"))
            Picasso.get().load(avatarUrl).into(avatar);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        popupWindow.setElevation(50);
        popupWindow.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),
                Gravity.CENTER, 0, 0);

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

}
