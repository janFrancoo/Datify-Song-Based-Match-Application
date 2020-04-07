package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;

public class User {

    private String username, eMail, bio;

    //ToDo: Add time value, avatar vs.

    public User() { }
    public User(String username, String eMail, String bio) {
        this.username = username;
        this.eMail = eMail;
        this.bio = bio;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMail() {
        return eMail;
    }

    public void setMail(String eMail) {
        this.eMail = eMail;
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
        return this.username + ", " + this.eMail + ", " + this.bio;
    }
}
