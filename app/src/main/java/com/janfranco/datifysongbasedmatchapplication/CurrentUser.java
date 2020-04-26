package com.janfranco.datifysongbasedmatchapplication;

class CurrentUser {

    private User user;
    private String currentChat = "";
    private static CurrentUser singleInstance = null;

    private CurrentUser() { }
    static CurrentUser getInstance() {
        if (singleInstance == null)
            singleInstance = new CurrentUser();
        return singleInstance;
    }

    User getUser() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
    }

    public String getCurrentChat() {
        return currentChat;
    }

    public void setCurrentChat(String currentChat) {
        this.currentChat = currentChat;
    }
}
