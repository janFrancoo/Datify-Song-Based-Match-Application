package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class User {

    private String eMail, username, avatarUrl, bio, gender, currTrack, currTrackUri;
    private ArrayList<String> matches;
    private ArrayList<Block> blockedMails;
    private int random;
    private long createDate;

    public User() {
        this.blockedMails = new ArrayList<>();
    }
    public User(String eMail, String username, int random, long createDate) {
        this.eMail = eMail;
        this.username = username;
        this.avatarUrl = "default";
        this.bio = "";
        this.gender = "";
        this.matches = new ArrayList<>();
        this.blockedMails = new ArrayList<>();
        this.random = random;
        this.createDate = createDate;
        this.currTrack = "";
        this.currTrackUri = "";
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

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public String getCurrTrack() {
        return currTrack;
    }

    public void setCurrTrack(String currTrack) {
        this.currTrack = currTrack;
    }

    public ArrayList<Block> getBlockedMails() {
        return blockedMails;
    }

    public void setBlockedMails(ArrayList<Block> blockedMails) {
        this.blockedMails = blockedMails;
    }

    public String getCurrTrackUri() {
        return currTrackUri;
    }

    public void setCurrTrackUri(String currTrackUri) {
        this.currTrackUri = currTrackUri;
    }

}
