package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

//ToDo: Don't forget the release the sources onDestroy like CurrentUser Singleton Class

public class HomeActivity extends AppCompatActivity {

    private User user;
    private CurrentUser currentUser;
    private FirebaseFirestore db;
    private ArrayList<Chat> chats;
    private RecyclerView recyclerView;
    private ChatListRecyclerAdapter chatListRecyclerAdapter;

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

        recyclerView = findViewById(R.id.chatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        chats = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        currentUser = CurrentUser.getInstance();
        user = currentUser.getUser();
        updateUser();

        getChats();
        chatListRecyclerAdapter = new ChatListRecyclerAdapter(chats, user.getUsername());
        recyclerView.setAdapter(chatListRecyclerAdapter);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        Intent intentToChat = new Intent(HomeActivity.this, ChatActivity.class);
                        intentToChat.putExtra("chatName", chats.get(position).getChatName());
                        startActivity(intentToChat);
                    }

                    @Override public void onLongItemClick(View view, int position) { }
                })
        );
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
                                if (randUser.geteMail().equals(user.geteMail()))
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
        String currentUserMail = user.geteMail();

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
                        randomChat(randUser);
                    }
                }
            }
        });
    }

    private void randomChat(User randUser) {
        final String randUsername = randUser.getUsername();
        final String randEmail = randUser.geteMail();
        final String randAvatar = randUser.getAvatarUrl();

        db.collection("userDetail").document(user.geteMail())
                .update("matches", FieldValue.arrayUnion(randEmail))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("userDetail").document(randEmail)
                                .update("matches", FieldValue.arrayUnion(user.geteMail()))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        ArrayList<String> matches = user.getMatches();
                                        matches.add(randEmail);
                                        user.setMatches(matches);
                                        currentUser.setUser(user);
                                        createChat(randEmail, randUsername, randAvatar, Constants.BASED_RANDOM);
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

    private void createChat(String matchMail, String matchUsername, String matchAvatar, int basedOn) {
        String currentUserMail = user.geteMail();
        String currentUsername = user.getUsername();
        String currentAvatar = user.getAvatarUrl();
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

        Chat chat = new Chat(chatName, basedOn, username1, username2,
                avatar1, avatar2, lastMessage);
        addChatToDb(chat, matchMail);
    }

    private void addChatToDb(final Chat chat, final String matchMail) {
        db.collection("chat").document(chat.getChatName()).set(chat)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intentToChat = new Intent(HomeActivity.this, ChatActivity.class);
                        intentToChat.putExtra("chatName", chat.getChatName());
                        startActivity(intentToChat);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Remove match from db
                        db.collection("userDetail").document(user.geteMail())
                                .update("matches", FieldValue.arrayRemove(matchMail));
                        Toast.makeText(HomeActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUser() {
        db.collection("userDetail").document(user.geteMail()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        assert user != null;
                        CurrentUser currentUser = CurrentUser.getInstance();
                        currentUser.setUser(user);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(HomeActivity.this, "Error loading chats!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private ArrayList<String> generateChatNames() {
        String currentUserMail = user.geteMail();
        ArrayList<String> matches = user.getMatches();
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

    private void getChats() {
        ArrayList<String> chatNames = generateChatNames();

        if (user.getMatches().size() == 0)
            return;

        db.collection("chat").whereIn(FieldPath.documentId(), chatNames).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                Chat chat = document.toObject(Chat.class);
                                chats.add(chat);
                            }
                            chatListRecyclerAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(HomeActivity.this, "Error while loading chats!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}
