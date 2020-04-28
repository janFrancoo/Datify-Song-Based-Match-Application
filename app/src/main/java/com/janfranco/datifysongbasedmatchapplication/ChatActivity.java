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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Iterator;
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

    // ToDo: Fix transmit, read, notifications (probably caused by localDb)

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

        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_MESSAGES +
                "(chatName VARCHAR, sender VARCHAR, message VARCHAR, sendDate LONG, " +
                "transmitted INT, read INT, hasImage INT, imgUrl VARCHAR, PRIMARY KEY (message, sendDate))");

        messages = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        fStorage = FirebaseStorage.getInstance();
        currentUser = CurrentUser.getInstance();
        currentUser.setCurrentChat(chatName);

        messageListAdapter = new MessageListRecyclerAdapter(getApplicationContext(), currentUser.getUser().getUsername(), messages);
        messageList.setAdapter(messageListAdapter);
        getMessagesFromLocal();
        getMessages();

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
                            public void onItemClick(View view, int position) {
                                if (messages.get(position).isHasImage())
                                    createPopUp(messages.get(position).getImgUrl());
                            }

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

    @Override
    protected void onStart() {
        super.onStart();

        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
    }

    private void getMessagesFromLocal() {
        messages.clear();

        String sql = "SELECT * FROM " + Constants.TABLE_MESSAGES;
        Cursor cursor = localDb.rawQuery(sql, null);

        int cName = cursor.getColumnIndex("chatName");
        int sender = cursor.getColumnIndex("sender");
        int message = cursor.getColumnIndex("message");
        int sendDate = cursor.getColumnIndex("sendDate");
        int transmitted = cursor.getColumnIndex("transmitted");
        int read = cursor.getColumnIndex("read");
        int hasImage = cursor.getColumnIndex("hasImage");
        int imageUrl = cursor.getColumnIndex("imgUrl");

        while (cursor.moveToNext()) {
            boolean setTransmitted, setRead, setHasImage;

            setTransmitted = cursor.getInt(transmitted) == 1;
            setRead = cursor.getInt(read) == 1;
            setHasImage = cursor.getInt(hasImage) == 1;

            if (chatName.equals(cursor.getString(cName))) {
                ChatMessage cm;
                if (setHasImage) {
                    cm = new ChatMessage(
                            cursor.getString(sender),
                            cursor.getString(message),
                            cursor.getString(imageUrl),
                            cursor.getLong(sendDate)
                    );
                } else {
                    cm = new ChatMessage(
                            cursor.getString(sender),
                            cursor.getString(message),
                            cursor.getLong(sendDate),
                            setTransmitted,
                            setRead
                    );
                }

                messages.add(cm);
            }
        }

        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
        cursor.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*if (registration != null)
            registration.remove();*/

        // localDb.close();

        currentUser.setCurrentChat("");
        removeTransmittedMessagesFromCloud();
    }

    /* void showNotification(String title, String message, Drawable avatar) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("NM",
                    "NEW_MESSAGE",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("New message notification");
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(channel);
        }
        BitmapDrawable drawable = (BitmapDrawable) avatar;
        Bitmap bitmap = drawable.getBitmap();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "NM")
                .setSmallIcon(R.drawable.firebase) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(message)// message for notification
                .setLargeIcon(bitmap) // avatar (large icon) for notification
                .setAutoCancel(true); // clear notification after click
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        assert mNotificationManager != null;
        mNotificationManager.notify(0, mBuilder.build());
    }
    */

    private void listen() {
        if (chatName == null)
            return;

        final DocumentReference ref = db.collection("chat").document(chatName);
        ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
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
                                /* if (!cm.getSender().equals(currentUser.getUser().getUsername()))
                                    showNotification(
                                            cm.getSender(),
                                            cm.getMessage(),
                                            headerAvatar.getDrawable()); */
                            } else if (cm.isRead()) {
                                int oldMsgIdx = messages.indexOf(cm);
                                ChatMessage oldMsg = messages.get(oldMsgIdx);
                                oldMsg.setRead(true);
                                messages.set(oldMsgIdx, oldMsg);
                            }
                            if (!cm.getSender().equals(currentUser.getUser().getUsername()))
                                fromDb.get(i).setRead(true);
                            writeToLocal(cm);
                        }
                        messageListAdapter.notifyDataSetChanged();
                        messageList.scrollToPosition(messages.size() - 1);

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
        messages.add(chatMessage);
        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
        db.collection("chat").document(chatName).update(
                "messages",
                FieldValue.arrayUnion(chatMessage),
                "lastMessage",
                message,
                "lastMessageDate",
                createDate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            messages.get(messages.indexOf(chatMessage)).setTransmitted(true);
                            messageListAdapter.notifyDataSetChanged();
                            chatMessage.setTransmitted(true);
                            writeToLocal(chatMessage);
                        } else {
                            Toast.makeText(ChatActivity.this, "Error while sending a message",
                                    Toast.LENGTH_SHORT).show();
                        }
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
                newMessages.get(i).setRead(true);
        }
        messageListAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);

        /*for (Iterator<ChatMessage> iterator = newMessages.iterator(); iterator.hasNext(); ) {
            ChatMessage cm = iterator.next();
            if (cm.isTransmitted()) {
                iterator.remove();
            }
        }*/

        db.collection("chat").document(chatName).update("messages", newMessages)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        listen();
                    }
                });
    }

    private void writeToLocal(ChatMessage chatMessage) {
        String query = "REPLACE INTO " + Constants.TABLE_MESSAGES + "(chatName, " +
                "sender, message, sendDate, transmitted, read, hasImage, imgUrl) " +
                "VALUES (?, ?, ?, ?, " + (chatMessage.isTransmitted() ? 1 : 0) + ", " +
                (chatMessage.isRead() ? 1 : 0) + ", " + (chatMessage.isHasImage() ? 1 : 0) + ", ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);
        sqLiteStatement.bindString(1, chatName);
        sqLiteStatement.bindString(2, chatMessage.getSender());
        sqLiteStatement.bindString(3, chatMessage.getMessage());
        sqLiteStatement.bindLong(4, chatMessage.getSendDate());
        sqLiteStatement.bindString(5, chatMessage.getImgUrl());
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
                        if (cm.isRead() && !cm.getSender().equals(currentUsername))
                            iterator.remove();
                    }
                    db.collection("chat").document(chatName).update("messages", cmList);
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
