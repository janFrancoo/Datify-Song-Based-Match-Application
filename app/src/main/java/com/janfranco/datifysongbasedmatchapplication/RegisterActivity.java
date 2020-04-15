package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText eMailInput, usernameInput, passwordInput;
    private TextView joinUsLabel;
    private ImageView bgTop, bgLeft, bgRight;
    private ConstraintLayout layout;
    private PopupWindow loadPopUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        layout = findViewById(R.id.registerLayout);
        eMailInput = findViewById(R.id.registerEMailInput);
        usernameInput = findViewById(R.id.registerUsernameInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        joinUsLabel = findViewById(R.id.regJoinUsLabel);
        final Button registerBtn = findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerBtn.isEnabled())
                    checkUsername();
            }
        });

        bgTop = findViewById(R.id.regBgTop);
        bgLeft = findViewById(R.id.regBgLeft);
        bgRight = findViewById(R.id.regBgRight);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (filter())
                    registerBtn.setEnabled(true);
                else
                    registerBtn.setEnabled(false);
            }

            private boolean filter() {
                String password = passwordInput.getText().toString().trim();
                return Pattern.matches(Constants.EMAIL_REGEX, eMailInput.getText().toString().trim()) &&
                        password.length() > Constants.PASSWORD_MIN_LEN &&
                        password.length() < Constants.PASSWORD_MAX_LEN &&
                        Pattern.matches(Constants.USERNAME_REGEX, usernameInput.getText().toString().trim());
            }
        };

        eMailInput.addTextChangedListener(textWatcher);
        usernameInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        Typeface metropolisExtraLightItalic = Typeface.createFromAsset(getAssets(),
                "fonts/Metropolis-ExtraLightItalic.otf");
        Typeface metropolisExtraBold = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-ExtraBold.otf");

        registerBtn.bringToFront();
        registerBtn.setTypeface(metropolisLight);
        joinUsLabel.setTypeface(metropolisExtraBold);

        eMailInput.setTypeface(metropolisExtraLightItalic);
        eMailInput.setTextColor(Constants.WHITE);
        usernameInput.setTypeface(metropolisExtraLightItalic);
        usernameInput.setTextColor(Constants.WHITE);
        passwordInput.setTypeface(metropolisExtraLightItalic);
        passwordInput.setTextColor(Constants.WHITE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        eMailInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    eMailInput.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                else
                    eMailInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
            }
        });

        usernameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    usernameInput.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                else
                    usernameInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
            }
        });

        passwordInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    passwordInput.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                else
                    passwordInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
            }
        });

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.hasFocus()) {
                    eMailInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    usernameInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    passwordInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                }
            }
        });

        bgTop.animate().scaleX(1.5f).scaleY(1.5f).setDuration(Constants.ANIM_DUR).start();
        bgLeft.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DUR).start();
        bgLeft.animate().translationX(-100).setDuration(Constants.ANIM_DUR).start();
        bgRight.animate().scaleX(1.5f).scaleY(1.5f).setDuration(Constants.ANIM_DUR).start();

        joinUsLabel.animate().alpha(.5f).setDuration(Constants.ANIM_DUR).start();
        joinUsLabel.animate().translationY(50).setDuration(Constants.ANIM_DUR).start();
    }

    private void checkUsername() {
        final String username = usernameInput.getText().toString().trim();
        db.collection("userDetail").whereEqualTo("username", username).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(task.getResult()).isEmpty()) {
                                register(username);
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "This username is used, please select another username.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Error while registering, please, try again!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void register(final String username) {
        loadPopUp();
        final String eMail = eMailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        Random random = new Random();
        final int randomVal = random.nextInt(Constants.RAND_LIM);

        mAuth.createUserWithEmailAndPassword(eMail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final User user = new User(eMail, username, randomVal);
                    db.collection("userDetail").document(eMail).set(user)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        CurrentUser currentUser = CurrentUser.getInstance();
                                        currentUser.setUser(user);
                                        Toast.makeText(RegisterActivity.this, "Successfully registered!",
                                                Toast.LENGTH_LONG).show();
                                        intentToFirstTimeSettings();
                                    } else {
                                        // Remove user
                                        loadPopUp.dismiss();
                                        Objects.requireNonNull(mAuth.getCurrentUser()).delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Error while registering, please, try again!",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            });
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Error while registering: This e-mail address is used or another unknown error.",
                            Toast.LENGTH_LONG).show();
                    loadPopUp.dismiss();
                }
            }
        });
    }

    private void intentToFirstTimeSettings() {
        loadPopUp.dismiss();
        Intent intentToSettings = new Intent(this, SettingsActivity.class);
        intentToSettings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intentToSettings.putExtra("register", true);
        startActivity(intentToSettings);
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void loadPopUp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_load,
                null);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        TextView popUpLoadLabel = popupView.findViewById(R.id.popUpLoadLabel);
        popUpLoadLabel.setTypeface(metropolisLight);
        popUpLoadLabel.setText("Authentication...");
        ImageView spinner = popupView.findViewById(R.id.popUpLoadSpinner);
        spinner.setBackgroundResource(R.drawable.load_animation);
        AnimationDrawable spinnerAnim = (AnimationDrawable) spinner.getBackground();
        spinnerAnim.start();

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        loadPopUp = new PopupWindow(popupView, width, height, true);

        loadPopUp.setOutsideTouchable(false);
        loadPopUp.setFocusable(false);
        loadPopUp.setElevation(50);
        loadPopUp.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),
                Gravity.CENTER, 0, 0);
    }

}
