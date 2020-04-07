package com.janfranco.datifysongbasedmatchapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button button = findViewById(R.id.splashNextBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToLoginActivity = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intentToLoginActivity);
            }
        });
    }

    //ToDo: Design an implement a splash screen with animations and sliding windows

}
