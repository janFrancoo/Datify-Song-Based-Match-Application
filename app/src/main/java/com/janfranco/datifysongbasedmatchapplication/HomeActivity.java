package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private CurrentUser currentUser;
    private FirebaseFirestore db;
    private ArrayList<Chat> chats;
    private ArrayList<String> chatNames;
    private ChatListRecyclerAdapter chatListRecyclerAdapter;
    private ListenerRegistration chatListener;
    private SQLiteDatabase localDb;
    private int randomChatLimit;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_CHAT +
                "(chatName VARCHAR PRIMARY KEY, basedOn INT, username1 VARCHAR, username2 VARCHAR, " +
                "avatar1 VARCHAR, avatar2 VARCHAR, lastMessage VARCHAR, lastMessageDate LONG)");

        initialize();

        ArrayList<String> matches = currentUser.getUser().getMatches();
        for (int i=0; i<matches.size(); i++)
            chatNames.add(generateChatName(matches.get(i)));

        getFromLocalDb();
        listen();
        listenChats();
    }

    @Override
    protected void onStart() {
        super.onStart();

        chatListRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*if (registration != null)
            registration.remove();

        if (registrationChatMsg != null)
            registrationChatMsg.remove();*/

        // localDb.close();
    }

    private void initialize() {
        chats = new ArrayList<>();
        chatNames = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();
        randomChatLimit = 0;

        imageSavePermission();

        Button tempSettings = findViewById(R.id.tempSettingsBtn);
        tempSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToSettings = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intentToSettings);
            }
        });

        Button tempSignOut = findViewById(R.id.tempSignout);
        tempSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
            }
        });

        Button tempRandomChat = findViewById(R.id.tempRandomChat);
        tempRandomChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRandomUser();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.chatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatListRecyclerAdapter = new ChatListRecyclerAdapter(getApplicationContext(),
                chats,
                currentUser.getUser().getUsername());
        recyclerView.setAdapter(chatListRecyclerAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override public void onItemClick(View view, int position) {
                                Intent intentToChat = new Intent(HomeActivity.this, ChatActivity.class);
                                if (notificationManager != null)
                                    notificationManager.cancel(position);
                                intentToChat.putExtra("chatName", chats.get(position).getChatName());
                                int offset = getOffsetOfCharSequence(
                                        chats.get(position).getChatName(),
                                        currentUser.getUser().geteMail());
                                if (offset == 0) {
                                    intentToChat.putExtra("chatAvatar", chats.get(position).getAvatar2());
                                    intentToChat.putExtra("chatUsername", chats.get(position).getUsername2());
                                } else {
                                    intentToChat.putExtra("chatAvatar", chats.get(position).getAvatar1());
                                    intentToChat.putExtra("chatUsername", chats.get(position).getUsername1());
                                }
                                startActivity(intentToChat);
                            }

                            @Override public void onLongItemClick(View view, int position) { }
                        })
        );
    }

    private void getFromLocalDb() {
        String sql = "SELECT * FROM " + Constants.TABLE_CHAT + " ORDER BY lastMessageDate DESC";
        Cursor cursor = localDb.rawQuery(sql, null);

        int cName = cursor.getColumnIndex("chatName");
        int basedOn = cursor.getColumnIndex("basedOn");
        int username1 = cursor.getColumnIndex("username1");
        int username2 = cursor.getColumnIndex("username2");
        int avatar1 = cursor.getColumnIndex("avatar1");
        int avatar2 = cursor.getColumnIndex("avatar2");
        int lastMessage = cursor.getColumnIndex("lastMessage");
        int lastMessageDate = cursor.getColumnIndex("lastMessageDate");

        while (cursor.moveToNext()) {
            chats.add(new Chat(
                    cursor.getString(cName),
                    cursor.getInt(basedOn),
                    cursor.getString(username1),
                    cursor.getString(username2),
                    cursor.getString(avatar1),
                    cursor.getString(avatar2),
                    cursor.getString(lastMessage),
                    cursor.getLong(lastMessageDate),
                    cursor.getLong(lastMessageDate)
            ));
        }

        chatListRecyclerAdapter.notifyDataSetChanged();
        cursor.close();
    }

    private void listen() {
        db.collection("userDetail").document(currentUser.getUser().geteMail())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(HomeActivity.this, "Firebase listen error!",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (snapshot != null) {
                            if (snapshot.getMetadata().hasPendingWrites())
                                return;
                            User user = snapshot.toObject(User.class);
                            assert user != null;
                            ArrayList<String> matches = user.getMatches();
                            for (int i=0; i<matches.size(); i++) {
                                boolean get = true;
                                String chatName = generateChatName(matches.get(i));
                                for (int j=0; j<chats.size(); j++) {
                                    if (chats.get(j).getChatName().equals(chatName)) {
                                        get = false;
                                        break;
                                    }
                                }
                                if (get) {
                                    getNewChatFromCloud(chatName);
                                }
                            }
                        }

                    }
                });
    }

    private void getNewChatFromCloud(String chatName) {
        db.collection("chat").document(chatName).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snapshot) {
                        Chat chat = snapshot.toObject(Chat.class);
                        assert chat != null;
                        chats.add(0, chat);
                        int idx = chats.indexOf(chat);
                        chatListRecyclerAdapter.notifyDataSetChanged();
                        if (chat.getChatName().equals(currentUser.getCurrentChat())) {
                            if (chat.getUsername1().equals(currentUser.getUser().getUsername()))
                                notification(
                                        idx,
                                        chat.getUsername2(),
                                        chat.getLastMessage(),
                                        chat.getAvatar2()
                                );
                            else
                                notification(
                                        idx,
                                        chat.getUsername1(),
                                        chat.getLastMessage(),
                                        chat.getAvatar1()
                                );
                        }
                        if (chatListener != null) {
                            chatListener.remove();
                        }
                        listenChats();
                        writeToLocalDb(chat);
                    }
                });
    }

    private void listenChats() {
        chatListener = db.collection("chat")
                .whereIn(FieldPath.documentId(), chatNames)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        assert queryDocumentSnapshots != null;
                        for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                             Chat chat = snapshot.toObject(Chat.class);
                             if (!chats.contains(chat)) {
                                 // if a new message or an avatar change
                                 // get chat in chats via chatName and update
                                 // write chat to local db
                                 // also notify new message
                                 for (int i=0; i<chats.size(); i++) {
                                     if (chats.get(i).getChatName().equals(chat.getChatName())) {
                                         chats.set(i, chat);
                                         if (!chat.getChatName().equals(currentUser.getCurrentChat())) {
                                             if (chat.getUsername1().equals(currentUser.getUser().getUsername()))
                                                 notification(
                                                         i,
                                                         chat.getUsername2(),
                                                         chat.getLastMessage(),
                                                         chat.getAvatar2());
                                             else
                                                 notification(
                                                         i,
                                                         chat.getUsername1(),
                                                         chat.getLastMessage(),
                                                         chat.getAvatar1());
                                         }
                                         chatListRecyclerAdapter.notifyDataSetChanged();
                                         writeToLocalDb(chat);
                                     }
                                 }
                             }
                         }
                    }
                });
    }

    private void writeToLocalDb(Chat c) {
        String query = "REPLACE INTO " + Constants.TABLE_CHAT + "(chatName, basedOn, " +
                "username1, username2, avatar1, avatar2, lastMessage, lastMessageDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);

        sqLiteStatement.clearBindings();
        sqLiteStatement.bindString(1, c.getChatName());
        sqLiteStatement.bindLong(2, c.getBasedOn());
        sqLiteStatement.bindString(3, c.getUsername1());
        sqLiteStatement.bindString(4, c.getUsername2());
        sqLiteStatement.bindString(5, c.getAvatar1());
        sqLiteStatement.bindString(6, c.getAvatar2());
        sqLiteStatement.bindString(7, c.getLastMessage());
        sqLiteStatement.bindLong(8, c.getLastMessageDate());

        try {
            sqLiteStatement.execute();
        } catch (Exception e) {
            Log.d("LocalDBError", Objects.requireNonNull(e.getLocalizedMessage()));
        }
    }

    private String generateChatName(String match) {
        String chatName;
        String currentUserMail = currentUser.getUser().geteMail();
        if (currentUserMail.compareTo(match) < 0)
            chatName = currentUserMail + "_" + match;
        else
            chatName = match + "_" + currentUserMail;
        return chatName;
    }

    private int getOffsetOfCharSequence(String chatName, String mail) {
        int match = 0;
        int mailLen = mail.length();

        for (int i=0; i<chatName.length(); i++) {
            if (chatName.charAt(i) == mail.charAt(match)) {
                match += 1;
                if (match == mailLen)
                    return i - mailLen + 1;
            }
            else
                match = 0;
        }

        return -1;
    }

    // Get random user via random number
    // Every user has a random number
    // Decide -> up/down and number
    // Ex -> 3000 - up -> Get first user whose rand num is greater than 3000
    // Ex -> 3000 - down -> Get first user whose rand num is less than 3000
    private void getRandomUser() {
        if (randomChatLimit == Constants.RAND_TRY_LIM)
            return;

        Random random = new Random();
        int randomVal = random.nextInt(Constants.RAND_LIM);
        int randomDir = (random.nextInt(2) == 1) ?
                Constants.RAND_UP : Constants.RAND_DOWN;

        Log.d("RANDOM_CHAT", randomVal + " " + randomDir);

        if (randomDir == Constants.RAND_UP) {
            db.collection("userDetail")
                    .whereGreaterThanOrEqualTo("random", randomVal)
                    .orderBy("random", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                                User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                                assert user != null;
                                String userMail = user.geteMail();
                                String chatName = generateChatName(userMail);
                                if (!userMail.equals(currentUser.getUser().geteMail())
                                        && !chatNames.contains(generateChatName(userMail)))
                                    createChat(chatName, user);
                                else {
                                    Log.d("RANDOM_CHAT", "Empty! Trying again!");
                                    randomChatLimit--;
                                    getRandomUser();
                                }
                            } else {
                                Log.d("RANDOM_CHAT", "Empty! Trying again!");
                                randomChatLimit--;
                                getRandomUser();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(HomeActivity.this, "Random error!",
                                    Toast.LENGTH_LONG).show();
                            Log.d("RANDOM_CHAT", "Error!" + e.getLocalizedMessage());
                        }
                    });
        } else {
            db.collection("userDetail")
                    .whereLessThanOrEqualTo("random", randomVal)
                    .orderBy("random", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                                User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                                assert user != null;
                                String userMail = user.geteMail();
                                String chatName = generateChatName(userMail);
                                if (!userMail.equals(currentUser.getUser().geteMail())
                                        && !chatNames.contains(chatName)) {
                                    createChat(chatName, user);
                                } else {
                                    Log.d("RANDOM_CHAT", "Empty! Trying again!");
                                    getRandomUser();
                                    randomChatLimit--;
                                }
                            } else {
                                Log.d("RANDOM_CHAT", "Empty! Trying again!");
                                getRandomUser();
                                randomChatLimit--;
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(HomeActivity.this, "Random error!",
                                    Toast.LENGTH_LONG).show();
                            Log.d("RANDOM_CHAT", "Error!" + e.getLocalizedMessage());
                        }
                    });
        }
    }

    private void createChat(String chatName, final User user) {
        final Chat chat;
        long createDate = Timestamp.now().getSeconds();
        int offset = getOffsetOfCharSequence(chatName, currentUser.getUser().geteMail());
        if (offset == 0) {
            chat = new Chat(
                    chatName,
                    Constants.BASED_RANDOM,
                    currentUser.getUser().getUsername(),
                    user.getUsername(),
                    currentUser.getUser().getAvatarUrl(),
                    user.getAvatarUrl(),
                    "You are matched totally random!",
                    createDate,
                    createDate
            );
        } else {
            chat = new Chat(
                    chatName,
                    Constants.BASED_RANDOM,
                    user.getUsername(),
                    currentUser.getUser().getUsername(),
                    user.getAvatarUrl(),
                    currentUser.getUser().getAvatarUrl(),
                    "You are matched totally random!",
                    createDate,
                    createDate
            );
        }

        db.collection("chat").document(chatName).set(chat)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        chatNames.add(chat.getChatName());
                        chats.add(0, chat);
                        chatListRecyclerAdapter.notifyDataSetChanged();
                        writeToLocalDb(chat);
                        listenChats();
                        updateMatchField(user.geteMail());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // generate random chat again
                        Toast.makeText(HomeActivity.this, "Create chat error!",
                                Toast.LENGTH_LONG).show();
                        Log.d("RANDOM_CHAT", Objects.requireNonNull(e.getLocalizedMessage()));
                    }
                });
    }

    private void updateMatchField(String matchMail) {
        db.collection("userDetail").document(matchMail)
                .update("matches", FieldValue.arrayUnion(currentUser.getUser().geteMail()));
        db.collection("userDetail").document(currentUser.getUser().geteMail())
                .update("matches", FieldValue.arrayUnion(matchMail));
    }

    private void notification(int id, String title, String message, String avatarUrl) {
        notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Real time notifications";
            String description = "New chat or new message";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("NCMD", name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        final Bitmap[] avatar = new Bitmap[1];
        Picasso.get().load(avatarUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                avatar[0] = bitmap;
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        });

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "NCMD")
                .setSmallIcon(R.drawable.firebase)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(avatar[0])
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(id, builder.build());
    }

    private void imageSavePermission() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

}
