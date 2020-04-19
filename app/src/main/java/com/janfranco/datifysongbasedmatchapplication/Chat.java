package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Chat {

    private int basedOn, status;
    private String chatName, username1, username2, avatar1, avatar2;
    private ArrayList<ChatMessage> messages;
    private long createDate;

    Chat() { }
    Chat(String chatName, int basedOn, String username1, String username2,
         String avatar1, String avatar2, String lastMessage, long createDate) {

        this.chatName = chatName;
        this.basedOn = basedOn;
        this.status = Constants.STATUS_NEW;

        this.username1 = username1;
        this.username2 = username2;
        this.avatar1 = avatar1;
        this.avatar2 = avatar2;

        this.createDate = createDate;

        this.messages = new ArrayList<>();
        this.messages.add(new ChatMessage(Constants.SENDER_BROADCAST, lastMessage, createDate));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Chat chat = (Chat) obj;
        return this.chatName.equals(chat.getChatName()) &&
                this.getLastMessage().equals(chat.getLastMessage());
    }

    public String getLastMessage() {
        return this.messages.get(this.messages.size() - 1).getMessage();
    }

    public long getLastMessageDate() {
        return this.messages.get(this.messages.size() - 1).getSendDate();
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

    public ArrayList<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }
}
