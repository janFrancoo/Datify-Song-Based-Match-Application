package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.Nullable;

class ChatMessage {

    private String sender, message, imgUrl;
    private boolean transmitted, read;
    private long sendDate;
    private int fruitId = 0;

    ChatMessage() { }
    ChatMessage(ChatMessage cm) {
        this.sender = cm.sender;
        this.message = cm.message;
        this.imgUrl = cm.imgUrl;
        this.transmitted = cm.transmitted;
        this.read = cm.read;
        this.sendDate = cm.sendDate;
        this.fruitId = cm.fruitId;
    }
    ChatMessage(String sender, String message, long sendDate) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
        this.read = false;
        this.sendDate = sendDate;
        this.imgUrl = "";
    }
    ChatMessage(String sender, String message, long sendDate, boolean transmitted, boolean read) {
        this.message = message;
        this.sender = sender;
        this.transmitted = transmitted;
        this.read = read;
        this.sendDate = sendDate;
        this.imgUrl = "";
    }
    ChatMessage(String sender, String message, String imgUrl, long sendDate) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
        this.read = false;
        this.sendDate = sendDate;
        this.imgUrl = imgUrl;
    }
    ChatMessage(String sender, String message, String imgUrl, long sendDate, boolean transmitted, boolean read) {
        this.message = message;
        this.sender = sender;
        this.sendDate = sendDate;
        this.imgUrl = imgUrl;
        this.transmitted = transmitted;
        this.read = read;
    }
    ChatMessage(String sender, long sendDate, int fruitId) {
        this.message = "You got the meaning ;)";
        this.transmitted = false;
        this.read = false;
        this.imgUrl = "";
        this.sender = sender;
        this.sendDate = sendDate;
        this.fruitId = fruitId;
    }

    // I assume there will be no message at the same timestamp
    @Override
    public boolean equals(@Nullable Object obj) {
        ChatMessage cm = (ChatMessage) obj;
        assert cm != null;
        return this.sendDate == cm.sendDate && this.message.equals(cm.message);
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

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public boolean isTransmitted() {
        return transmitted;
    }

    public void setTransmitted(boolean transmitted) {
        this.transmitted = transmitted;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getSendDate() {
        return sendDate;
    }

    public void setSendDate(long sendDate) {
        this.sendDate = sendDate;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

}
