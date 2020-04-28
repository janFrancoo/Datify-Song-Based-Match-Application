package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.Nullable;

class ChatMessage {

    private String sender, message, imgUrl;
    private boolean transmitted, read, hasImage;
    private long sendDate;

    ChatMessage() { }
    ChatMessage(String sender, String message, long sendDate) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
        this.read = false;
        this.sendDate = sendDate;
        this.hasImage = false;
        this.imgUrl = "";
    }
    ChatMessage(String sender, String message, long sendDate, boolean transmitted, boolean read) {
        this.message = message;
        this.sender = sender;
        this.transmitted = transmitted;
        this.read = read;
        this.sendDate = sendDate;
        this.hasImage = false;
        this.imgUrl = "";
    }
    ChatMessage(String sender, String message, String imgUrl, long sendDate) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
        this.read = false;
        this.sendDate = sendDate;
        this.hasImage = true;
        this.imgUrl = imgUrl;
    }

    // I assume there will be no message at the same timestamp
    @Override
    public boolean equals(@Nullable Object obj) {
        ChatMessage cm = (ChatMessage) obj;
        assert cm != null;
        return this.sendDate == cm.sendDate;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isTransmitted() {
        return transmitted;
    }

    public void setTransmitted(boolean transmitted) {
        this.transmitted = transmitted;
    }

    public long getSendDate() {
        return sendDate;
    }

    public void setSendDate(long sendDate) {
        this.sendDate = sendDate;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

}
