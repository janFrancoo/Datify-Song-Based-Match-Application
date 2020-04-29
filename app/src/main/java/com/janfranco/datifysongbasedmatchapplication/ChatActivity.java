package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private String chatName, avatarUrl, chatUsername;
    private FirebaseFirestore db;
    private FirebaseStorage fStorage;
    private Uri imgData;
    private Button sendMessageBtn;
    private EditText messageInput;
    private CurrentUser currentUser;
    private ArrayList<ChatMessage> messages;
    private MessageListRecyclerAdapter messageListAdapter;
    private RecyclerView messageList;
    private TextView headerUsername;
    private ImageView headerAvatar;
    private SQLiteDatabase localDb;
    private ListenerRegistration registration;

    //ToDo: SendMessage function -> Update chat lastMessage & lastMessageDate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_MESSAGES +
                "(chatName VARCHAR, sender VARCHAR, message VARCHAR, sendDate LONG, " +
                "read INT, imgUrl VARCHAR, PRIMARY KEY (message, sendDate))");

        initialize();

        db = FirebaseFirestore.getInstance();
        fStorage = FirebaseStorage.getInstance();
        currentUser = CurrentUser.getInstance();
        currentUser.setCurrentChat(chatName);

        messageListAdapter = new MessageListRecyclerAdapter(getApplicationContext(),
                currentUser.getUser().getUsername(), messages);
        messageList.setAdapter(messageListAdapter);
        getMessagesFromLocal();
        // getMessages();
        listen();
    }

    @Override
    protected void onStart() {
        super.onStart();

        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (registration != null)
            registration.remove();

        // localDb.close();

        currentUser.setCurrentChat("");
        removeRedundantMessagesFromCloud();
    }

    private void initialize() {
        chatName = getIntent().getStringExtra("chatName");
        messageInput = findViewById(R.id.messageInput);
        sendMessageBtn = findViewById(R.id.sendMessageBtn);
        sendMessageBtn.setEnabled(false);

        avatarUrl = getIntent().getStringExtra("chatAvatar");
        chatUsername = getIntent().getStringExtra("chatUsername");

        messages = new ArrayList<>();

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
                if (!avatarUrl.matches("default"))
                    createPopUp(avatarUrl);
            }
        });
        updateHeader();

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
                messageList.scrollToPosition(messages.size() - 1);
            }
        });

        messageList.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), messageList,
                        new RecyclerItemClickListener.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) { }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                messageInput.setText(messages.get(position).getMessage());
                            }
                        })
        );

        messageList.scrollToPosition(messages.size() - 1);

        messageList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    messageList.scrollBy(0, oldBottom - bottom);
                }
            }
        });

        Button attachmentBtn = findViewById(R.id.sendAttachmentBtn);
        attachmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPic();
            }
        });
    }

    private void getMessagesFromLocal() {
        String sql = "SELECT * FROM " + Constants.TABLE_MESSAGES;
        Cursor cursor = localDb.rawQuery(sql, null);

        int cName = cursor.getColumnIndex("chatName");
        int sender = cursor.getColumnIndex("sender");
        int message = cursor.getColumnIndex("message");
        int sendDate = cursor.getColumnIndex("sendDate");
        int read = cursor.getColumnIndex("read");
        int imageUrl = cursor.getColumnIndex("imgUrl");

        while (cursor.moveToNext()) {
            boolean setRead;
            setRead = cursor.getInt(read) == 1;
            String img = cursor.getString(imageUrl);

            if (chatName.equals(cursor.getString(cName))) {
                ChatMessage cm;
                if (!img.equals("")) {
                    cm = new ChatMessage(
                            cursor.getString(sender),
                            cursor.getString(message),
                            img,
                            cursor.getLong(sendDate)
                    );
                } else {
                    cm = new ChatMessage(
                            cursor.getString(sender),
                            cursor.getString(message),
                            cursor.getLong(sendDate),
                            true,
                            setRead
                    );
                }

                messages.add(cm);
            }
        }

        cursor.close();
        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
    }

    /*
    private void getMessages() {
        db.collection("chat").document(chatName)
                .collection("message").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                ChatMessage cm = document.toObject(ChatMessage.class);
                                if (!cm.getSender().equals(currentUser.getUser().getUsername())
                                        && !cm.isRead()) {
                                    // New messages from other side
                                    // Update cloud and add message to list
                                    // Also write to local db
                                    Log.d("CHAT_TEST", cm.getMessage());
                                    messages.add(cm);
                                    messageListAdapter.notifyDataSetChanged();
                                    messageList.scrollToPosition(messages.size() - 1);
                                    db.collection("chat").document(chatName)
                                            .collection("message").document(document.getId())
                                            .update("read", true);
                                    writeToLocal(cm);
                                }
                                else if (cm.isRead()) {
                                    // Messages that current user sent and read by other side
                                    // Update message in message list and update local db
                                    int idx = messages.indexOf(cm);
                                    messages.get(idx).setRead(true);
                                    messageListAdapter.notifyDataSetChanged();
                                    messageList.scrollToPosition(messages.size() - 1);
                                    writeToLocal(cm);
                                }
                            }
                            // Listen new messages
                            listen();
                        }
                        else
                            Toast.makeText(ChatActivity.this, "Error while getting messages!",
                                    Toast.LENGTH_LONG).show();
                    }
                });
    }
    */

    private void listen() {
        registration = db.collection("chat").document(chatName).collection("message")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(ChatActivity.this, "Firebase listen error!",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        assert queryDocumentSnapshots != null;
                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                            if (dc.getDocument().getMetadata().hasPendingWrites())
                                continue;

                            ChatMessage cm;
                            switch (dc.getType()) {
                                case ADDED:
                                    cm = dc.getDocument().toObject(ChatMessage.class);
                                    if (!cm.getSender().equals(currentUser.getUser().getUsername())) {
                                        // new message from other side
                                        // add message to list and write to local db
                                        // also set read true in cloud
                                        messages.add(cm);
                                        messageListAdapter.notifyDataSetChanged();
                                        messageList.scrollToPosition(messages.size() - 1);
                                        writeToLocal(cm);
                                        db.collection("chat").document(chatName)
                                                .collection("message").document(dc.getDocument().getId())
                                                .update("read", true);
                                    }
                                    break;
                                case MODIFIED:
                                    cm = dc.getDocument().toObject(ChatMessage.class);
                                    if (cm.getSender().equals(currentUser.getUser().getUsername())) {
                                        // other side read message of current user
                                        // update message list and local db
                                        int idx = messages.indexOf(cm);
                                        if (idx != -1) {
                                            messages.get(idx).setRead(true);
                                            messageListAdapter.notifyDataSetChanged();
                                        }
                                        writeToLocal(cm);
                                    }
                            }
                            break;
                        }
                    }
                });
    }

    private void sendMessage(String message) {
        long createDate = Timestamp.now().getSeconds();
        ChatMessage cm = new ChatMessage(
                currentUser.getUser().getUsername(),
                message,
                createDate
        );
        messages.add(cm);

        final ChatMessage transmittedCm = new ChatMessage(cm);
        transmittedCm.setTransmitted(true);

        db.collection("chat").document(chatName).collection("message")
                .add(transmittedCm)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        int idx = messages.indexOf(transmittedCm);
                        messages.get(idx).setTransmitted(true);
                        messageListAdapter.notifyDataSetChanged();
                        writeToLocal(transmittedCm);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void writeToLocal(ChatMessage cm) {
        String query = "REPLACE INTO " + Constants.TABLE_MESSAGES +
                "(chatName, sender, message, sendDate, read, imgUrl) VALUES" +
                "(?, ?, ?, ?, " + (cm.isRead() ? 1 : 0) + ", ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);
        sqLiteStatement.bindString(1, chatName);
        sqLiteStatement.bindString(2, cm.getSender());
        sqLiteStatement.bindString(3, cm.getMessage());
        sqLiteStatement.bindLong(4, cm.getSendDate());
        sqLiteStatement.bindString(5, cm.getImgUrl());
        sqLiteStatement.execute();
    }

    private void updateHeader() {
        headerUsername.setText(chatUsername);
        if (!avatarUrl.equals("default"))
            Picasso.get().load(avatarUrl).into(headerAvatar);
    }

    private void createPopUp(String url) {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_avatar,
                null);

        ImageView avatar = popupView.findViewById(R.id.popUpAvatar);
        Picasso.get().load(url).into(avatar);

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

    private void removeRedundantMessagesFromCloud() {
        final WriteBatch batch = db.batch();

        db.collection("chat").document(chatName)
                .collection("message")
                .whereEqualTo("sender", currentUser.getUser().getUsername())
                .whereEqualTo("read", true).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                                ChatMessage cm = documentSnapshot.toObject(ChatMessage.class);
                                int idx = messages.indexOf(cm);
                                if (messages.get(idx).isRead())
                                    batch.delete(
                                            db.collection("chat").document(chatName)
                                                    .collection("message").document(documentSnapshot.getId())
                                    );
                            }

                            batch.commit();
                        }
                    }
                });
    }

    private void sendPic() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent toGallery = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(toGallery, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent toGallery = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(toGallery, 2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 2 && resultCode == RESULT_OK && data != null) {
            imgData = data.getData();
            uploadImage();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadImage() {
        if (imgData != null) {
            UUID uuid = UUID.randomUUID();
            final String imgName = "chatMessages/" + uuid + ".jpg";
            fStorage.getReference().child(imgName).putFile(imgData).addOnSuccessListener(
                    new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            StorageReference newRef = FirebaseStorage.getInstance().getReference(imgName);
                            newRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String downloadUrl = uri.toString();
                                    long createDate = Timestamp.now().getSeconds();
                                    String message = messageInput.getText().toString();
                                    if (message.equals(""))
                                        message = "A pretty new image...";

                                    final ChatMessage cm = new ChatMessage(
                                            currentUser.getUser().getUsername(),
                                            message,
                                            downloadUrl,
                                            createDate
                                    );

                                    messages.add(cm);
                                    messageListAdapter.notifyDataSetChanged();
                                    messageList.scrollToPosition(messages.size() - 1);
                                    db.collection("chat").document(chatName).update(
                                            "messages",
                                            FieldValue.arrayUnion(cm),
                                            "lastMessage",
                                            message,
                                            "lastMessageDate",
                                            createDate
                                    ).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                messages.get(messages.indexOf(cm)).setTransmitted(true);
                                                messageListAdapter.notifyDataSetChanged();
                                                cm.setTransmitted(true);
                                                writeToLocal(cm);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ChatActivity.this, "Image sending error!",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
