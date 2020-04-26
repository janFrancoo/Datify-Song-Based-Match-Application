package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private CurrentUser currentUser;
    private FirebaseFirestore db;
    private ArrayList<Chat> chats;
    private ArrayList<String> matches;
    private ChatListRecyclerAdapter chatListRecyclerAdapter;
    private ListenerRegistration registration, registrationChatMsg;
    private SQLiteDatabase localDb;

    // ToDo: If a new chat is opened -> its color is green or sth

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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
                getRandomUser(Constants.RAND_LIM);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.chatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        chats = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();

        chatListRecyclerAdapter = new ChatListRecyclerAdapter(getApplicationContext(),
                chats,
                currentUser.getUser().getUsername());
        recyclerView.setAdapter(chatListRecyclerAdapter);

        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_CHAT +
                "(chatName VARCHAR PRIMARY KEY, basedOn INT, username1 VARCHAR, username2 VARCHAR, " +
                "avatar1 VARCHAR, avatar2 VARCHAR, lastMessage VARCHAR, lastMessageDate LONG)");

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override public void onItemClick(View view, int position) {
                                Intent intentToChat = new Intent(HomeActivity.this, ChatActivity.class);
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

        ArrayList<String> newMatchesSafe = currentUser.getUser().getMatches();
        ArrayList<String> newMatches = new ArrayList<>(newMatchesSafe);

        matches = getFromLocalDb();

        if (matches.isEmpty())
            matches = newMatches;
        else
            newMatches.removeAll(matches);

        getFromCloud(newMatches);
        listen();
        listenChatMessages();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() {
        super.onStart();

        listenChatMessages();
        sortList(chats);
        chatListRecyclerAdapter.notifyDataSetChanged();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private ArrayList<String> getFromLocalDb() {
        ArrayList<String> newMatches = new ArrayList<>();
        chats.clear();
        String sql = "SELECT * FROM " + Constants.TABLE_CHAT;
        Cursor cursor = localDb.rawQuery(sql, null);
        int cName = cursor.getColumnIndex("chatName");
        int basedOn = cursor.getColumnIndex("basedOn");
        int username1 = cursor.getColumnIndex("username1");
        int username2 = cursor.getColumnIndex("username2");
        int avatar1 = cursor.getColumnIndex("avatar1");
        int avatar2 = cursor.getColumnIndex("avatar2");
        int lastMessage = cursor.getColumnIndex("lastMessage");
        int lastMessageDate = cursor.getColumnIndex("lastMessageDate");

        String currentUserMail = currentUser.getUser().geteMail();
        while (cursor.moveToNext()) {
            String chatNames = cursor.getString(cName);
            int offset = getOffsetOfCharSequence(
                    chatNames,
                    currentUserMail);
            if (offset == 0)
                newMatches.add(chatNames.substring(currentUserMail.length()));
            else if (offset != -1)
                newMatches.add(chatNames.substring(0, chatNames.length() - currentUserMail.length() - 1));
            else
                continue;

            chats.add(new Chat(
                    chatNames,
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

        sortList(chats);
        chatListRecyclerAdapter.notifyDataSetChanged();
        cursor.close();

        return newMatches;
    }

    private void getRandomUser(int randLim) {
        Random random = new Random();
        if (randLim == 0) {
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
            return;
        }
        final int randomVal = random.nextInt(randLim);

        db.collection("userDetail").whereGreaterThanOrEqualTo("random", randomVal)
                .orderBy("random").limit(1).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot document = task.getResult();
                            if (document != null && !document.isEmpty()) {
                                ArrayList<User> users = (ArrayList<User>) document.toObjects(User.class);
                                User randUser = users.get(0);
                                if (randUser.geteMail().equals(currentUser.getUser().geteMail()))
                                    getRandomUser(randomVal);
                                else
                                    checkIfChatExists(randUser, randomVal);
                            } else {
                                getRandomUser(randomVal);
                                Log.d("RANDOM", "TRY AGAIN!");
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "Error!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkIfChatExists(final User randUser, final int pastLim) {
        String chatName;
        String currentUserMail = currentUser.getUser().geteMail();

        if (currentUserMail.compareTo(randUser.geteMail()) < 0)
            chatName = currentUserMail + "_" + randUser.geteMail();
        else
            chatName = randUser.geteMail() + "_" + currentUserMail;

        db.collection("chat").document(chatName).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (Objects.requireNonNull(task.getResult()).exists()) {
                        getRandomUser(pastLim);
                    } else {
                        final String randUsername = randUser.getUsername();
                        final String randEmail = randUser.geteMail();
                        final String randAvatar = randUser.getAvatarUrl();

                        createChat(randEmail, randUsername, randAvatar, Constants.BASED_RANDOM);
                    }
                }
            }
        });
    }

    private void createChat(final String matchMail, String matchUsername, String matchAvatar, int basedOn) {
        String currentUserMail = currentUser.getUser().geteMail();
        String currentUsername = currentUser.getUser().getUsername();
        String currentAvatar = currentUser.getUser().getAvatarUrl();
        String chatName, username1, username2, avatar1, avatar2;
        String lastMessage = "You are matched totally random!";

        if (currentUserMail.compareTo(matchMail) < 0) {
            chatName = currentUserMail + "_" + matchMail;
            username1 = currentUsername;
            username2 = matchUsername;
            avatar1 = currentAvatar;
            avatar2 = matchAvatar;
        }
        else {
            chatName = matchMail + "_" + currentUserMail;
            username1 = matchUsername;
            username2 = currentUsername;
            avatar1 = matchAvatar;
            avatar2 = currentAvatar;
        }

        long createDate = Timestamp.now().getSeconds();
        final Chat chat = new Chat(chatName, basedOn, username1, username2,
                avatar1, avatar2, lastMessage, createDate, createDate);

        db.collection("chat").document(chat.getChatName()).set(chat)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateMatchField(matchMail, chat);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateMatchField(final String matchMail, final Chat chat) {
        db.collection("userDetail").document(currentUser.getUser().geteMail())
                .update("matches", FieldValue.arrayUnion(matchMail))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("userDetail").document(matchMail)
                                .update("matches", FieldValue.arrayUnion(currentUser.getUser().geteMail()))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        currentUser.getUser().getMatches().add(matchMail);

                                        Intent intentToChat = new Intent(HomeActivity.this, ChatActivity.class);
                                        intentToChat.putExtra("chatName", chat.getChatName());

                                        int offset = getOffsetOfCharSequence(
                                                chat.getChatName(),
                                                currentUser.getUser().geteMail());
                                        if (offset == 0) {
                                            intentToChat.putExtra("chatAvatar", chat.getAvatar2());
                                            intentToChat.putExtra("chatUsername", chat.getUsername2());
                                        } else {
                                            intentToChat.putExtra("chatAvatar", chat.getAvatar1());
                                            intentToChat.putExtra("chatUsername", chat.getUsername1());
                                        }
                                        Log.d("INTENT", chat.getAvatar1() + " " + chat.getAvatar2() + " " + chat.getUsername1() + " " + chat.getUsername2());
                                        startActivity(intentToChat);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(HomeActivity.this, e.getLocalizedMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private ArrayList<String> generateChatNames(ArrayList<String> matches) {
        String currentUserMail = currentUser.getUser().geteMail();
        ArrayList<String> chatNames = new ArrayList<>();
        for (int i=0; i<matches.size(); i++) {
            String matchMail = matches.get(i);
            if (currentUserMail.compareTo(matchMail) < 0)
                chatNames.add(currentUserMail + "_" + matchMail);
            else
                chatNames.add(matchMail + "_" + currentUserMail);
        }
        return chatNames;
    }

    private void getFromCloud(ArrayList<String> matches) {
        if (matches == null || matches.isEmpty())
            return;

        ArrayList<String> chatNames = generateChatNames(matches);

        db.collection("chat")
                .whereIn(FieldPath.documentId(), chatNames)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Chat chat = document.toObject(Chat.class);
                            for (int i=0; i<chats.size(); i++)
                                if (chats.get(i).getChatName().equals(chat.getChatName())) {
                                    chats.remove(i);
                                    break;
                                }
                            chats.add(chat);
                        }
                        sortList(chats);
                        chatListRecyclerAdapter.notifyDataSetChanged();
                        listenChatMessages();
                        writeToLocalDb();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void listen() {
        final DocumentReference ref = db.collection("userDetail")
                .document(currentUser.getUser().geteMail());
        registration = ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(HomeActivity.this, "Real time listening error!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (snapshot != null && snapshot.exists() && snapshot.getData() != null) {
                    User updatedUser = snapshot.toObject(User.class);
                    if (updatedUser != null) {
                        currentUser.setUser(updatedUser);
                        ArrayList<String> newMatchesSafe = updatedUser.getMatches();
                        ArrayList<String> newMatches = new ArrayList<>(newMatchesSafe);
                        newMatches.removeAll(matches);
                        getFromCloud(newMatches);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (registration != null)
            registration.remove();

        if (registrationChatMsg != null)
            registrationChatMsg.remove();

        localDb.close();
    }

    private void writeToLocalDb() {
        String query = "REPLACE INTO " + Constants.TABLE_CHAT + "(chatName, basedOn, " +
                "username1, username2, avatar1, avatar2, lastMessage, lastMessageDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);

        for (int i=0; i<chats.size(); i++) {
            Chat c = chats.get(i);
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
    }

    private void listenChatMessages() {
        if (registrationChatMsg != null)
            registrationChatMsg.remove();

        ArrayList<String> chatNames = new ArrayList<>();
        for (int i=0; i<chats.size(); i++) {
            chatNames.add(chats.get(i).getChatName());
        }

        if (chatNames.isEmpty())
            return;

        registrationChatMsg = db.collection("chat")
                .whereIn(FieldPath.documentId(), chatNames)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(HomeActivity.this, "Live listening error!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        assert queryDocumentSnapshots != null;
                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Chat updatedChat = dc.getDocument().toObject(Chat.class);
                                for (int i=0; i<chats.size(); i++) {
                                    if (chats.get(i).getChatName().equals(updatedChat.getChatName())) {
                                        chats.remove(i);
                                        break;
                                    }
                                }
                                chats.add(updatedChat);
                                sortList(chats);
                                chatListRecyclerAdapter.notifyDataSetChanged();
                                writeToLocalDb();
                            }
                        }
                    }
                });
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sortList(ArrayList<Chat> chats) {
        chats.sort(new Comparator<Chat>() {
            @Override
            public int compare(Chat o1, Chat o2) {
                if (o1.getLastMessageDate() > o2.getLastMessageDate())
                    return -1;
                else if (o1.getLastMessageDate() < o2.getLastMessageDate())
                    return 1;
                return 0;
            }
        });
    }

}
