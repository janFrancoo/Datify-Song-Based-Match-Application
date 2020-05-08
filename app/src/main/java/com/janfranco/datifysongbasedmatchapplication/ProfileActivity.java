package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private String mail, username, avatarUrl;
    private TextView profileBio, profileCurrTrack;

    private FirebaseFirestore db;
    private TrackListRecyclerAdapter adapter;
    private ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mail = getIntent().getStringExtra("mail");
        username = getIntent().getStringExtra("username");
        avatarUrl = getIntent().getStringExtra("avatarUrl");

        init();
    }

    private void init() {
        profileBio = findViewById(R.id.profileBio);
        profileCurrTrack = findViewById(R.id.profileCurrTrack);

        db = FirebaseFirestore.getInstance();
        getProfileDetailFromCloud();
        getTracksFromCloud();

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
            blockUser();
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

    private void blockUser() {
        // ToDo: Block by e-mail
    }

}
