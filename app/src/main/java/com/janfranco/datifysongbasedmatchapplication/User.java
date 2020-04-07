package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;

public class User {

    private String eMail, username, avatarUrl, bio;

    public User() { }
    public User(String eMail, String username) {
        this.eMail = eMail;
        this.username = username;
        this.avatarUrl = "default";
        this.bio = "";
    }
    public User(String eMail, String username, String avatarUrl, String bio) {
        this.eMail = eMail;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public String geteMail() {
        return eMail;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @NonNull
    @Override
    public String toString() {
        return this.eMail + ", " + this.username + ", " + this.avatarUrl + ", " + this.bio;
    }
}
