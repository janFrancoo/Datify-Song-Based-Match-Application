package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

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

    // Todo: Add link to spotify from song list
    // Todo: Listen while not viewing the app
    // Todo: Update currTrack field on cloud while not listening and prevent matching
    //                                                              based on previous songs
    // Todo: Get USERS via currTrack field not just ONE user and match by random idx

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify);

        db = FirebaseFirestore.getInstance();
        currentUser = CurrentUser.getInstance();

        localDb = openOrCreateDatabase(Constants.DB_NAME_2, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_TRACKS +
                "(userMail VARCHAR, trackName VARCHAR, artistName VARCHAR, createDate LONG, " +
                "PRIMARY KEY (userMail, createDate))");

        songs = new ArrayList<>();
        RecyclerView songList = findViewById(R.id.songList);
        songList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackListRecyclerAdapter(getApplicationContext(), songs);
        songList.setAdapter(adapter);

        songList.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), songList,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override public void onItemClick(View view, int position) { }

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
        trackName = findViewById(R.id.trackName);
        Button matchSongBtn = findViewById(R.id.matchSongBtn);
        matchSongBtn.setOnClickListener(v -> matchBySong());
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
        int createDate = cursor.getColumnIndex("createDate");

        while (cursor.moveToNext()) {
            if (cursor.getString(userMail).equals(currentUser.getUser().geteMail())) {
                Song song = new Song(
                        currentUser.getUser().geteMail(),
                        cursor.getString(trackName),
                        cursor.getString(artistName),
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
                        trackName.setText("You are listening: " + currTrack.name +
                                " by " + currTrack.artist.name);
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
                "artistName, createDate) VALUES (?, ?, ?, ?)";

        SQLiteStatement sqLiteStatement = localDb.compileStatement(query);

        sqLiteStatement.clearBindings();
        sqLiteStatement.bindString(1, currentUser.getUser().geteMail());
        sqLiteStatement.bindString(2, song.getTrackName());
        sqLiteStatement.bindString(3, song.getArtistName());
        sqLiteStatement.bindLong(4, song.getAddDate());

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

    private void matchBySong() {
        if (currTrack == null)
            return;

        db.collection("userDetail")
                .whereEqualTo("currTrack", currTrack.artist.name + "___" + currTrack.name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        User user = snapshot.toObject(User.class);
                        if (!user.geteMail().matches(currentUser.getUser().geteMail()))
                            createChat(currTrack, user);
                    }
                });
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
