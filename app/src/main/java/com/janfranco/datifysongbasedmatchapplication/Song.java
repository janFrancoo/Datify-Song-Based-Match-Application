package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.Nullable;

public class Song {

    private String userMail, trackName, artistName, uri;
    private long addDate;

    public Song() { }
    public Song(String userMail, String trackName, String artistName, String uri, long addDate) {
        this.userMail = userMail;
        this.trackName = trackName;
        this.artistName = artistName;
        this.uri = uri;
        this.addDate = addDate;
    }

    // I assume there will be no message at the same timestamp
    @Override
    public boolean equals(@Nullable Object obj) {
        Song song = (Song) obj;
        assert song != null;
        return this.addDate == song.addDate && this.userMail.equals(song.userMail);
    }

    public String getUserMail() {
        return userMail;
    }

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getAddDate() {
        return addDate;
    }

    public void setAddDate(long addDate) {
        this.addDate = addDate;
    }

}
