package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Iterator;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    private String chatName, avatarUrl, chatUsername;
    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private Button sendMessageBtn;
    private EditText messageInput;
    private CurrentUser currentUser;
    private ArrayList<ChatMessage> messages;
    private MessageListRecyclerAdapter messageListAdapter;
    private RecyclerView messageList;
    private TextView headerUsername;
    private ImageView headerAvatar;
    private SQLiteDatabase localDb;

    // ToDo: Add long click listener, if long clicked -> copy contents to input

    // ToDo: Add viewed or transmitted into messages using transmitted field!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatName = getIntent().getStringExtra("chatName");
        messageInput = findViewById(R.id.messageInput);
        sendMessageBtn = findViewById(R.id.sendMessageBtn);
        sendMessageBtn.setEnabled(false);

        avatarUrl = getIntent().getStringExtra("chatAvatar");
        chatUsername = getIntent().getStringExtra("chatUsername");

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
        updateHeader();

        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_MESSAGES +
                "(chatName VARCHAR, sender VARCHAR, message VARCHAR, sendDate LONG, PRIMARY KEY (message, sendDate))");

        messages = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();

        messageListAdapter = new MessageListRecyclerAdapter(currentUser.getUser().getUsername(), messages);
        messageList.setAdapter(messageListAdapter);
        getMessagesFromLocal();
        getMessages();
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
    protected void onStart() {
        super.onStart();

        messageListAdapter.notifyDataSetChanged();
    }

    private void getMessagesFromLocal() {
        messages.clear();

        String sql = "SELECT * FROM " + Constants.TABLE_MESSAGES;
        Cursor cursor = localDb.rawQuery(sql, null);

        int cName = cursor.getColumnIndex("chatName");
        int sender = cursor.getColumnIndex("sender");
        int message = cursor.getColumnIndex("message");
        int sendDate = cursor.getColumnIndex("sendDate");

        while (cursor.moveToNext()) {
            if (chatName.equals(cursor.getString(cName))) {
                ChatMessage cm = new ChatMessage(
                        cursor.getString(sender),
                        cursor.getString(message),
                        cursor.getLong(sendDate)
                );
                cm.setTransmitted(true);
                messages.add(cm);
            }
        }

        messageListAdapter.notifyDataSetChanged();
        cursor.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (registration != null)
            registration.remove();

        removeTransmittedMessagesFromCloud();
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

                if (snapshot != null && snapshot.exists()) {
                    Chat updatedChat = snapshot.toObject(Chat.class);
                    if (updatedChat != null) {
                        ArrayList<ChatMessage> fromDb = updatedChat.getMessages();
                        for (int i=0; i<fromDb.size(); i++) {
                            ChatMessage cm = fromDb.get(i);
                            if (!messages.contains(cm)) {
                                messages.add(cm);
                                writeToLocal(cm);
                            }
                            if (!cm.getSender().equals(currentUser.getUser().getUsername()))
                                fromDb.get(i).setTransmitted(true);
                        }
                        messageListAdapter.notifyDataSetChanged();

                        /*for (Iterator<ChatMessage> iterator = fromDb.iterator(); iterator.hasNext(); ) {
                            ChatMessage cm = iterator.next();
                            if (cm.isTransmitted()) {
                                iterator.remove();
                            }
                        }
                        */
                        db.collection("chat").document(chatName).update("messages", fromDb);
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
        final ChatMessage chatMessage = new ChatMessage(currentUser.getUser().getUsername(), message, createDate);
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
        messages.add(chatMessage);
        messageListAdapter.notifyDataSetChanged();
        writeToLocal(chatMessage);
    }

    private void getMessages() {
        db.collection("chat").document(chatName).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snapshot) {
                        Chat chat = snapshot.toObject(Chat.class);
                        if (chat != null) {
                            ArrayList<ChatMessage> newMessages = chat.getMessages();
                            processNewMessages(newMessages);
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

    private void processNewMessages(ArrayList<ChatMessage> newMessages) {
        for (int i=0; i<newMessages.size(); i++) {
            ChatMessage cm = newMessages.get(i);
            if (!messages.contains(cm)) {
                messages.add(cm);
                writeToLocal(cm);
            }
            if (!cm.getSender().equals(currentUser.getUser().getUsername()))
                newMessages.get(i).setTransmitted(true);
        }
        messageListAdapter.notifyDataSetChanged();

        /*for (Iterator<ChatMessage> iterator = newMessages.iterator(); iterator.hasNext(); ) {
            ChatMessage cm = iterator.next();
            if (cm.isTransmitted()) {
                iterator.remove();
            }
        }*/

        db.collection("chat").document(chatName).update("messages", newMessages);
    }

    private void writeToLocal(ChatMessage chatMessage) {
        String query = "REPLACE INTO " + Constants.TABLE_MESSAGES + "(chatName, " +
                "sender, message, sendDate) " +
                "VALUES (?, ?, ?, ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);
         sqLiteStatement.bindString(1, chatName);
         sqLiteStatement.bindString(2, chatMessage.getSender());
         sqLiteStatement.bindString(3, chatMessage.getMessage());
         sqLiteStatement.bindLong(4, chatMessage.getSendDate());
         sqLiteStatement.execute();
    }

    private void updateHeader() {
        headerUsername.setText(chatUsername);
        if (!avatarUrl.equals("default"))
            Picasso.get().load(avatarUrl).into(headerAvatar);
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

    public void removeTransmittedMessagesFromCloud() {
        final String currentUsername = currentUser.getUser().getUsername();
        db.collection("chat").document(chatName).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Chat chat = Objects.requireNonNull(task.getResult()).toObject(Chat.class);
                    assert chat != null;
                    ArrayList<ChatMessage> cmList = chat.getMessages();
                    for (Iterator<ChatMessage> iterator = cmList.iterator(); iterator.hasNext();) {
                        ChatMessage cm = iterator.next();
                        if (cm.isTransmitted() && !cm.getSender().equals(currentUsername))
                            iterator.remove();
                    }
                    db.collection("chat").document(chatName).update("messages", cmList);
                }
            }
        });
    }

}
