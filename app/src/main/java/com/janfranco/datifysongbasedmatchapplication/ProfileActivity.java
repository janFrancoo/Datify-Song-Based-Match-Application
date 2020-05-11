package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private String mail, username, avatarUrl, chatName;
    private TextView profileBio, profileCurrTrack;

    private CurrentUser currentUser;
    private FirebaseFirestore db;
    private TrackListRecyclerAdapter adapter;
    private ArrayList<Song> songs;
    private SQLiteDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mail = getIntent().getStringExtra("mail");
        username = getIntent().getStringExtra("username");
        avatarUrl = getIntent().getStringExtra("avatarUrl");
        chatName = getIntent().getStringExtra("chatName");

        init();
    }

    private void init() {
        profileBio = findViewById(R.id.profileBio);
        profileCurrTrack = findViewById(R.id.profileCurrTrack);

        db = FirebaseFirestore.getInstance();
        getProfileDetailFromCloud();
        getTracksFromCloud();
        currentUser = CurrentUser.getInstance();
        localDb = openOrCreateDatabase(Constants.DB_NAME, Context.MODE_PRIVATE, null);

        TextView profileUsername = findViewById(R.id.profileUsername);
        profileUsername.setText(username);

        ImageView profileAvatar = findViewById(R.id.profileAvatar);
        Picasso.get().load(avatarUrl).into(profileAvatar);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        profileUsername.setTypeface(metropolisLight);
        profileBio.setTypeface(metropolisLight);
        profileCurrTrack.setTypeface(metropolisLight);

        songs = new ArrayList<>();
        RecyclerView profileTrackList = findViewById(R.id.profileTrackList);
        profileTrackList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackListRecyclerAdapter(getApplicationContext(), songs);
        profileTrackList.setAdapter(adapter);

        Button btnGoBack = findViewById(R.id.profileBackBtn);
        btnGoBack.setOnClickListener(v -> {
            finish();
        });

        Button blockBtn = findViewById(R.id.profileBlockBtn);
        blockBtn.setOnClickListener(v -> {
            blockPopUp();
        });
    }

    @SuppressLint("SetTextI18n")
    private void getProfileDetailFromCloud() {
        db.collection("userDetail").document(mail).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        if (user.getBio().equals(""))
                            profileBio.setText("~~~~");
                        else
                            profileBio.setText(user.getBio());
                        profileCurrTrack.setText("Currently listening: " + user.getCurrTrack());
                    } else {
                        Toast.makeText(this, "Profile getting error!",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void getTracksFromCloud() {
        db.collection("track").document(mail).collection("list").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Song song = snapshot.toObject(Song.class);
                        songs.add(song);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void removeChatFromLocalDb(String chatName) {
        String query = "DELETE FROM " + Constants.TABLE_CHAT + " WHERE chatName = '" +
                chatName + "'";
        localDb.execSQL(query);

        Toast.makeText(this, "You can remove block and access chat from settings.",
                Toast.LENGTH_LONG).show();
        Intent intentToHome = new Intent(this, HomeActivity.class);
        startActivity(intentToHome);
        finish();
    }

    private void blockPopUp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_block,
                null);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        TextView popUpBlockInfo = popupView.findViewById(R.id.popUpBlockInfo);
        popUpBlockInfo.setTypeface(metropolisLight);
        popUpBlockInfo.setTextColor(Constants.DARK_PURPLE);

        Button blockBtn = popupView.findViewById(R.id.popUpBlock);
        final EditText reason = popupView.findViewById(R.id.popUpBlockInput);
        reason.setTypeface(metropolisLight);
        blockBtn.setOnClickListener(v -> {
            long createDate = Timestamp.now().getSeconds();
            Block block = new Block(
                    mail,
                    username,
                    avatarUrl,
                    reason.getText().toString(),
                    createDate
            );
            db.collection("userDetail").document(currentUser.getUser().geteMail())
                    .update("blockedMails", FieldValue.arrayUnion(block),
                            "matches", FieldValue.arrayRemove(mail))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentUser.getUser().getBlockedMails().add(block);
                            currentUser.getUser().getMatches().remove(mail);
                            removeChatFromLocalDb(chatName);
                        } else {
                            Toast.makeText(this, "Blocking failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

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
