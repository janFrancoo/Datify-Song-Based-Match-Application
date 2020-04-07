package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

//ToDo: Always clean the CurrentUser Singleton Class
//ToDo: When there is a sign out or onDestory method is called!

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    @Override
    protected void onStart() {
        super.onStart();

        User currentUser = getCurrentUser();
        Log.d("WELCOME", currentUser.toString());
    }

    private User getCurrentUser() {
        CurrentUser currentUser = CurrentUser.getInstance();
        return currentUser.getUser();
    }
}
