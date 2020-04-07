package com.janfranco.datifysongbasedmatchapplication;

class Constants {
    // Login & Register
    static final int PASSWORD_MIN_LEN = 5;
    static final int PASSWORD_MAX_LEN = 20;
    static final String USERNAME_REGEX = "^[a-zA-Z\\d]{5,15}$";
    static final String EMAIL_REGEX = "^(.+)@([a-zA-Z\\d-]+)\\.([a-zA-Z]+)(\\.[a-zA-Z]+)?$";

    // User Detail
    static final int MALE = 0;
    static final int FEMALE = 1;
    static final int OTHER = 2;

}
