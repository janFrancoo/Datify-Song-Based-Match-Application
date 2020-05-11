package com.janfranco.datifysongbasedmatchapplication;

import android.util.Log;

import androidx.annotation.Nullable;

public class Block {

    private String mail, username, avatarUrl, reason;
    private long createDate;

    public Block() { }
    public Block(String mail, String username, String avatarUrl, String reason, long createDate) {
        this.mail = mail;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.reason = reason;
        this.createDate = createDate;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
