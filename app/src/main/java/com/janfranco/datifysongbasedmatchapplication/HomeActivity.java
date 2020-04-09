package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

//ToDo: Don't forget the release the sources onDestroy like CurrentUser Singleton Class

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button tempSettings = findViewById(R.id.tempSettingsBtn);
        tempSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToSettings = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intentToSettings);
            }
        });

        Button tempSignout = findViewById(R.id.tempSignout);
        tempSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        CurrentUser currentUser = CurrentUser.getInstance();
        User user = currentUser.getUser();
        Log.d("WELCOME", user.toString());
    }

}
