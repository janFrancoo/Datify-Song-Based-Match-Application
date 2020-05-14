package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class SpotifyActivity extends AppCompatActivity {

    private Track currTrack;
    private static final String CLIENT_ID = "fb4680b5b1384bcaaf3febd991797ecc";
    private static final String REDIRECT_URI = "com.janfranco.datifysongbasedmatchapplication://callback";
    private SpotifyAppRemote mSpotifyAppRemote;

    private FirebaseFirestore db;
    private CurrentUser currentUser;
    private SQLiteDatabase localDb;
    private ArrayList<Song> songs;

    private TextView trackName;
    private TrackListRecyclerAdapter adapter;
    private ImageView trackCover;

    // Todo: Add link to spotify from song list
    // Todo: Listen while not viewing the app
    // Todo: Update currTrack field on cloud while not listening and prevent matching
    //                                                              based on previous songs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify);

        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();

        localDb = openOrCreateDatabase(Constants.DB_NAME_2, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_TRACKS +
                "(userMail VARCHAR, trackName VARCHAR, artistName VARCHAR, uri VARCHAR, createDate LONG, " +
                "PRIMARY KEY (userMail, createDate))");

        songs = new ArrayList<>();
        RecyclerView songList = findViewById(R.id.songList);
        songList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackListRecyclerAdapter(getApplicationContext(), songs);
        songList.setAdapter(adapter);

        songList.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), songList,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override public void onItemClick(View view, int position) {
                                /*Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(songs.get(position).getUri()));
                                intent.putExtra(Intent.EXTRA_REFERRER,
                                        Uri.parse("android-app://" + getPackageName()));
                                startActivity(intent);*/
                                mSpotifyAppRemote.getPlayerApi().play(songs.get(position).getUri());
                            }

                            @Override public void onLongItemClick(View view, int position) {
                                removeFromCloud(songs.get(position));
                                songs.remove(position);
                                adapter.notifyDataSetChanged();
                            }
                        })
        );

        getFromLocalDb();

        init();
    }

    private void init() {
        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        trackName = findViewById(R.id.trackName);
        trackName.setTypeface(metropolisLight);
        Button matchSongBtn = findViewById(R.id.matchSongBtn);
        matchSongBtn.setOnClickListener(v -> getUsersBySong());

        BottomNavigationView navBottom = findViewById(R.id.spotifyBottomNav);
        navBottom.setSelectedItemId(R.id.page_match);
        navBottom.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.page_settings) {
                Intent intentToMatchAct = new Intent(this, SettingsActivity.class);
                startActivity(intentToMatchAct);
            }
            if (item.getItemId() == R.id.page_home) {
                Intent intentToHome = new Intent(this, HomeActivity.class);
                startActivity(intentToHome);
                finish();
            }
            return false;
        });

        trackCover = findViewById(R.id.trackCover);
        trackCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(currTrack.uri));
            intent.putExtra(Intent.EXTRA_REFERRER,
                    Uri.parse("android-app://" + getPackageName()));
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        connected();
                    }

                    public void onFailure(Throwable throwable) {
                        Toast.makeText(SpotifyActivity.this, "Spotify listen error!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getFromLocalDb() {
        String sql = "SELECT * FROM " + Constants.TABLE_TRACKS + " ORDER BY createDate DESC";
        Cursor cursor = localDb.rawQuery(sql, null);

        int userMail = cursor.getColumnIndex("userMail");
        int trackName = cursor.getColumnIndex("trackName");
        int artistName = cursor.getColumnIndex("artistName");
        int uri = cursor.getColumnIndex("uri");
        int createDate = cursor.getColumnIndex("createDate");

        while (cursor.moveToNext()) {
            if (cursor.getString(userMail).equals(currentUser.getUser().geteMail())) {
                Song song = new Song(
                        currentUser.getUser().geteMail(),
                        cursor.getString(trackName),
                        cursor.getString(artistName),
                        cursor.getString(uri),
                        cursor.getLong(createDate)
                );
                songs.add(song);
                Log.d("LISTTT", song.getArtistName() + " " + song.getTrackName());
            }
        }

        adapter.notifyDataSetChanged();
        cursor.close();
    }

    @SuppressLint("SetTextI18n")
    private void connected() {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    currTrack = playerState.track;
                    if (currTrack != null) {
                        trackName.setText("You are listening:\n" + currTrack.name +
                                " by " + currTrack.artist.name);
                        Picasso.get().load("https://i.scdn.co/image/" + currTrack.imageUri.toString()).into(trackCover);
                        mSpotifyAppRemote.getImagesApi().getImage(currTrack.imageUri)
                                .setResultCallback(bitmap -> trackCover.setImageBitmap(bitmap));
                        updateCurrentTrack();
                    }
                });
    }

    private void updateCurrentTrack() {
        db.collection("userDetail").document(currentUser.getUser().geteMail()).update(
                "currTrack",
                currTrack.artist.name + "___" + currTrack.name
        ).addOnSuccessListener(aVoid -> {
            currentUser.getUser().setCurrTrack(currTrack.name);
            addTrackToList();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Track update error!",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void addTrackToList() {
        long addDate = Timestamp.now().getSeconds();
        Song song = new Song(
                currentUser.getUser().geteMail(),
                currTrack.name,
                currTrack.artist.name,
                currTrack.uri,
                addDate
        );

        for (int i=0; i<songs.size(); i++)
            if (songs.get(i).getTrackName().equals(song.getTrackName()))
                return;

        songs.add(0, song);
        adapter.notifyDataSetChanged();
        db.collection("track").document(currentUser.getUser().geteMail())
                .collection("list")
                .document(song.getArtistName() + "_" + song.getTrackName()).set(song)
                .addOnSuccessListener(aVoid -> {
                    writeToLocalDb(song);
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Cloud add to list error",
                            Toast.LENGTH_LONG).show();
                    songs.remove(song);
                    adapter.notifyDataSetChanged();
        });
    }

    private void writeToLocalDb(Song song) {
        String query = "REPLACE INTO " + Constants.TABLE_TRACKS + " (userMail, trackName, " +
                "artistName, uri, createDate) VALUES (?, ?, ?, ?, ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);

        sqLiteStatement.clearBindings();
        sqLiteStatement.bindString(1, currentUser.getUser().geteMail());
        sqLiteStatement.bindString(2, song.getTrackName());
        sqLiteStatement.bindString(3, song.getArtistName());
        sqLiteStatement.bindString(4, song.getUri());
        sqLiteStatement.bindLong(5, song.getAddDate());

        try {
            sqLiteStatement.execute();
        } catch (Exception e) {
            Log.d("LocalDBError", Objects.requireNonNull(e.getLocalizedMessage()));
        }
    }

    private void removeFromCloud(Song song) {
        String documentName = song.getArtistName() + "_" + song.getTrackName();
        db.collection("track").document(currentUser.getUser().geteMail())
                .collection("list").document(documentName).delete()
                .addOnSuccessListener(aVoid -> {
                    removeFromLocalDb(song);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                    songs.add(0, song);
                    adapter.notifyDataSetChanged();
                });
    }

    private void removeFromLocalDb(Song song) {
        String query = "DELETE FROM " + Constants.TABLE_TRACKS + " WHERE userMail = '" +
                currentUser.getUser().geteMail() + "' AND createDate = '" + song.getAddDate() + "'";
        localDb.execSQL(query);
    }

    private void getUsersBySong() {
        if (currTrack == null)
            return;

        Track safeTrack = currTrack;
        ArrayList<User> userList = new ArrayList<>();

        db.collection("userDetail")
                .whereEqualTo("currTrack", safeTrack.artist.name + "___" + safeTrack.name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        User user = snapshot.toObject(User.class);
                        if (!user.geteMail().matches(currentUser.getUser().geteMail()))
                            userList.add(user);
                    }
                    if (userList.size() != 0)
                        selectUser(safeTrack, userList);
                    else
                        Toast.makeText(this, "No users have found =(",
                                Toast.LENGTH_LONG).show();
                });
    }

    private void selectUser(Track track, ArrayList<User> users) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.popup_select_user, null);

        final AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.setView(dialogLayout);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        TextView infoLabel = dialogLayout.findViewById(R.id.userSelectListInfoLabel);
        infoLabel.setTypeface(metropolisLight);

        RecyclerView userList = dialogLayout.findViewById(R.id.userSelectList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        userList.setLayoutManager(layoutManager);
        UserSelectListRecyclerAdapter adapter = new UserSelectListRecyclerAdapter(
                getApplicationContext(), users);
        userList.setAdapter(adapter);

        userList.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), userList,
                        new RecyclerItemClickListener.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                createChat(track, users.get(position));
                            }

                            @Override
                            public void onLongItemClick(View view, int position) { }
                        })
        );

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.CENTER);
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

    private void createChat(Track track, User user) {
        String chatName = generateChatName(user.geteMail());
        long createDate = Timestamp.now().getSeconds();
        int offset = getOffsetOfCharSequence(chatName, currentUser.getUser().geteMail());

        Chat chat;
        if (offset == 0) {
            chat = new Chat(
              chatName,
              Constants.BASED_SONG,
              currentUser.getUser().getUsername(),
              user.getUsername(),
              currentUser.getUser().getAvatarUrl(),
              user.getAvatarUrl(),
              "Your are matched by song: " + track.artist.name + " - " + track.name,
              createDate,
              createDate
            );
        } else {
            chat = new Chat(
                    chatName,
                    Constants.BASED_SONG,
                    user.getUsername(),
                    currentUser.getUser().getUsername(),
                    user.getAvatarUrl(),
                    currentUser.getUser().getAvatarUrl(),
                    "Your are matched by song: " + track.artist.name + " - " + track.name,
                    createDate,
                    createDate
            );
        }

        db.collection("chat").document(chatName).set(chat)
                .addOnSuccessListener(aVoid -> {
                    updateMatchField(user.geteMail());
                });
    }

    private void updateMatchField(String matchMail) {
        db.collection("userDetail").document(matchMail)
                .update("matches", FieldValue.arrayUnion(currentUser.getUser().geteMail()));
        db.collection("userDetail").document(currentUser.getUser().geteMail())
                .update("matches", FieldValue.arrayUnion(matchMail))
                .addOnCompleteListener(task -> {
                    Intent intentToHome = new Intent(SpotifyActivity.this, HomeActivity.class);
                    startActivity(intentToHome);
                    finish();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

}
