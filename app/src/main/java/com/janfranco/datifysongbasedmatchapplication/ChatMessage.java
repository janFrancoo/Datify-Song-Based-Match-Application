package com.janfranco.datifysongbasedmatchapplication;

class ChatMessage {

    private String sender, message;
    private boolean transmitted;
    private long sendDate;

    ChatMessage() { }
    ChatMessage(String sender, String message, long sendDate) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
        this.sendDate = sendDate;
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
}
