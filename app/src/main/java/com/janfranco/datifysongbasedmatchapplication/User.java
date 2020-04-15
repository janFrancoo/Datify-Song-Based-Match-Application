package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class User {

    private String eMail, username, avatarUrl, bio, gender;
    private ArrayList<String> matches;
    private int random;

    // ToDo: Add create date!

    public User() { }
    public User(String eMail, String username, int random) {
        this.eMail = eMail;
        this.username = username;
        this.avatarUrl = "default";
        this.bio = "";
        this.gender = "";
        this.matches = new ArrayList<>();
        this.random = random;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public ArrayList<String> getMatches() {
        return matches;
    }

    public void setMatches(ArrayList<String> matches) {
        this.matches = matches;
    }

    public int getRandom() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }

}
