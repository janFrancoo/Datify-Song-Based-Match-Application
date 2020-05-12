package com.janfranco.datifysongbasedmatchapplication;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.View;

class Constants {

    // Login & Register
    static final int PASSWORD_MIN_LEN = 5;
    static final int PASSWORD_MAX_LEN = 20;
    static final String USERNAME_REGEX = "^[a-zA-Z\\d]{5,15}$";
    static final String EMAIL_REGEX = "^(.+)@([a-zA-Z\\d-]+)\\.([a-zA-Z]+)(\\.[a-zA-Z]+)?$";

    // Colors
    static int BLACK = Color.parseColor("#000000");
    static int WHITE = Color.parseColor("#FFFFFF");
    static int DARK_PURPLE = Color.parseColor("#752893");

    // Translation animation
    static void translation(View comp, boolean dir, float amount) {
        ObjectAnimator animation;
        if (dir == Constants.DIR_X)
            animation = ObjectAnimator.ofFloat(comp, "translationX", amount);
        else
            animation = ObjectAnimator.ofFloat(comp, "translationY", amount);
        animation.setDuration(1000);
        animation.start();
    }

    // Animation values
    static float ANIM_ALPHA_TO = 1.0f;
    static boolean DIR_X = true;
    static boolean DIR_Y = false;
    static int ANIM_DUR = 1200;

    /*
    // Fonts
    enum FONT {
        CALIBRI_LIGHT,
        CALIBRI_LIGHT_ITALIC,
        CALIBRI_BOLD,
        CALIBRI_BOLD_ITALIC
    }

    // Get font
    static Typeface getFont(Context context, FONT font) {
        if (font == FONT.CALIBRI_LIGHT)
            return Typeface.createFromAsset(context.getAssets(), "fonts/calibril.ttf");
        else if (font == FONT.CALIBRI_LIGHT_ITALIC)
            return Typeface.createFromAsset(context.getAssets(), "fonts/calibrili.ttf");
        else if (font == FONT.CALIBRI_BOLD)
            return Typeface.createFromAsset(context.getAssets(), "fonts/calibrib.ttf");
        else
            return Typeface.createFromAsset(context.getAssets(), "fonts/calibribi.ttf");
    }
    */

    // Chat
    // Status
    static int STATUS_NEW = 0;
    static int STATUS_CLOSED = 1; // (archived)
    // Based on
    static int BASED_RANDOM = 0;
    static int BASED_SONG = 1;
    // Consider a new based => premium membership randomize
    // Consider a new based => premium membership randomize
    // Consider a new based => premium membership randomize

    // Random
    static int RAND_LIM = 10000;
    static int RAND_UP = 1;
    static int RAND_DOWN = 0;
    static int RAND_TRY_LIM = 5;

    // Date
    static String DATE_MESSAGE = "HH:mm"; // Hours and minutes for chat messages

    // Local Database
    static String DB_NAME = "Chat";
    static String TABLE_CHAT = "chats";
    static String TABLE_MESSAGES = "messages";

    static String DB_NAME_2 = "Track";
    static String TABLE_TRACKS = "tracks";

    // Fruit
    static int FRUIT_LEMON = 1;
    static int FRUIT_WATERMELON = 2;

}
