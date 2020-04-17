package com.janfranco.datifysongbasedmatchapplication;

class ChatMessage {

    private String sender, message;
    private boolean transmitted;
    // ToDo: Add date!

    ChatMessage() { }
    ChatMessage(String sender, String message) {
        this.message = message;
        this.sender = sender;
        this.transmitted = false;
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

}
