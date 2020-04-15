package com.janfranco.datifysongbasedmatchapplication;

public class Chat {

    private int basedOn, status;
    private String chatName, username1, username2, avatar1, avatar2, lastMessage;

    // ToDo: Add date!!!

    Chat() { }
    Chat(String chatName, int basedOn, String username1, String username2,
         String avatar1, String avatar2, String lastMessage) {

        this.chatName = chatName;
        this.basedOn = basedOn;
        this.status = Constants.STATUS_NEW;

        this.username1 = username1;
        this.username2 = username2;
        this.avatar1 = avatar1;
        this.avatar2 = avatar2;
        this.lastMessage = lastMessage;

    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public int getBasedOn() {
        return basedOn;
    }

    public void setBasedOn(int basedOn) {
        this.basedOn = basedOn;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getUsername1() {
        return username1;
    }

    public void setUsername1(String username1) {
        this.username1 = username1;
    }

    public String getUsername2() {
        return username2;
    }

    public void setUsername2(String username2) {
        this.username2 = username2;
    }

    public String getAvatar1() {
        return avatar1;
    }

    public void setAvatar1(String avatar1) {
        this.avatar1 = avatar1;
    }

    public String getAvatar2() {
        return avatar2;
    }

    public void setAvatar2(String avatar2) {
        this.avatar2 = avatar2;
    }

}
