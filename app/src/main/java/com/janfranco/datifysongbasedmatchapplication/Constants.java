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

    // User Detail
    enum GENDER {
        MALE,
        FEMALE,
        OTHER
    }

    // ToDo: Create error messages here, don't use 'ERROR' 'ERROR' every time!

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

}
