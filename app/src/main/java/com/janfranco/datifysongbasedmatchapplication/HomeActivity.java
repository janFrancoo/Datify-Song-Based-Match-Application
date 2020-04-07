package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

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

        new CountDownTimer(8000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                FirebaseAuth.getInstance().signOut();
                Intent intentToLoginPage = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intentToLoginPage);
                HomeActivity.this.finish();
            }
        }.start();
    }

    private User getCurrentUser() {
        CurrentUser currentUser = CurrentUser.getInstance();
        return currentUser.getUser();
    }
}
