package com.janfranco.datifysongbasedmatchapplication;

import java.net.InetAddress;

public class NetworkUtil implements Runnable {
    private volatile boolean isInternet;

    @Override
    public void run() {
        try {
            InetAddress ip = InetAddress.getByName("google.com");
            isInternet = !ip.equals("");
        } catch (Exception e) {
            isInternet = false;
        }
    }

    public boolean getValue() {
        return isInternet;
    }
}
