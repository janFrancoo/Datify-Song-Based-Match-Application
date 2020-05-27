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
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button loginBtn, showPasswordBtn;
    private EditText eMailInput, passwordInput;
    private ConstraintLayout layout;
    private TextView welcomeLabel, secondLabel, forgotPasswordText;
    private PopupWindow loadPopUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            loadPopUp();
            updateCurrentUserSingleton(mAuth.getCurrentUser().getEmail());
        }

        init();
    }

    private void init() {
        layout = findViewById(R.id.loginLayout);
        loginBtn = findViewById(R.id.loginLoginBtn);
        eMailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        showPasswordBtn = findViewById(R.id.loginShowPasswordBtn);
        forgotPasswordText = findViewById(R.id.loginForgotPasswordText);
        TextView registerText = findViewById(R.id.loginRegisterText);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        Typeface metropolisExtraLightItalic = Typeface.createFromAsset(getAssets(),
                "fonts/Metropolis-ExtraLightItalic.otf");
        Typeface metropolisExtraBold = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-ExtraBold.otf");

        welcomeLabel = findViewById(R.id.welcomeLabel);
        welcomeLabel.setTypeface(metropolisExtraBold);
        welcomeLabel.setTextColor(Constants.DARK_PURPLE);
        welcomeLabel.setTypeface(welcomeLabel.getTypeface(), Typeface.BOLD_ITALIC);
        secondLabel = findViewById(R.id.secondWelcomeLabel);
        secondLabel.setTypeface(metropolisExtraLightItalic);
        secondLabel.setTextColor(Constants.BLACK);
        forgotPasswordText.setTypeface(metropolisLight);
        forgotPasswordText.setTextColor(Constants.BLACK);
        registerText.setTypeface(metropolisLight);
        registerText.setTextColor(Constants.BLACK);
        passwordInput.setTypeface(metropolisLight);
        passwordInput.setTextColor(Constants.WHITE);
        eMailInput.setTypeface(metropolisLight);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginBtn.isEnabled())
                    login();
                else
                    Toast.makeText(LoginActivity.this,
                            "The length of your password must be between 5 and 20. " +
                                    "Also, check your e-mail address.", Toast.LENGTH_LONG).show();
            }
        });
        loginBtn.setTypeface(metropolisLight);

        showPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod)
                    passwordInput.setTransformationMethod(null);
                else
                    passwordInput.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        registerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToSplashActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intentToSplashActivity);
            }
        });

        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopUp();
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (filter())
                    loginBtn.setEnabled(true);
                else
                    loginBtn.setEnabled(false);
            }

            private boolean filter() {
                return Pattern.matches(Constants.EMAIL_REGEX, eMailInput.getText().toString().trim()) &&
                        passwordInput.getText().toString().length() >= Constants.PASSWORD_MIN_LEN;
            }
        };

        eMailInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);

        eMailInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    eMailInput.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                else
                    eMailInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
            }
        });

        passwordInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    passwordInput.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                    showPasswordBtn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                } else {
                    passwordInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    showPasswordBtn.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                }
            }
        });

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.hasFocus()) {
                    eMailInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    eMailInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    passwordInput.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                    showPasswordBtn.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        layout.startAnimation(headerAlpha());
        Constants.translation(welcomeLabel, Constants.DIR_X, -100);
        Constants.translation(secondLabel, Constants.DIR_X, -80);
        Constants.translation(loginBtn, Constants.DIR_Y, -75);
        Constants.translation(eMailInput, Constants.DIR_Y, -75);
        Constants.translation(passwordInput, Constants.DIR_Y, -75);
        Constants.translation(forgotPasswordText, Constants.DIR_Y, -75);
        Constants.translation(showPasswordBtn, Constants.DIR_Y, -75);
        ImageView headerTop = findViewById(R.id.backgroundTop);
        Constants.translation(headerTop, Constants.DIR_X, -30);
    }

    private void login() {
        loadPopUp();
        final String eMail = eMailInput.getText().toString().trim();
        mAuth.signInWithEmailAndPassword(
                eMail,
                passwordInput.getText().toString().trim()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    updateCurrentUserSingleton(eMail);
                } else {
                    Toast.makeText(LoginActivity.this, "Check your e-mail or password!",
                            Toast.LENGTH_LONG).show();
                    if (loadPopUp != null)
                        loadPopUp.dismiss();
                }
            }
        });
    }

    private void createPopUp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_window,
                null);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        TextView popUpInfoMailLabel = popupView.findViewById(R.id.popUpInfoMailLabel);
        popUpInfoMailLabel.setTypeface(metropolisLight);
        popUpInfoMailLabel.setTextColor(Constants.DARK_PURPLE);

        Button sendMail = popupView.findViewById(R.id.popUpSendMail);
        final EditText email = popupView.findViewById(R.id.popUpEmailInput);
        email.setTypeface(metropolisLight);
        sendMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Pattern.matches(Constants.EMAIL_REGEX, email.getText().toString().trim()))
                    sendPasswordResetMail(email.getText().toString().trim());
                else
                    Toast.makeText(LoginActivity.this, "Please enter an valid e-mail!",
                            Toast.LENGTH_LONG).show();
            }
        });

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        popupWindow.setElevation(50);
        popupWindow.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),
                Gravity.CENTER, 0, 0);

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private void sendPasswordResetMail(String eMail) {
        mAuth.sendPasswordResetEmail(eMail)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            Toast.makeText(LoginActivity.this, "An e-mail sent.",
                                    Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(LoginActivity.this, "Error while sending an email!",
                                    Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateCurrentUserSingleton(String eMail) {
        if (loadPopUp != null)
            loadPopUp.dismiss();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("userDetail").document(eMail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    assert user != null;
                    CurrentUser currentUser = CurrentUser.getInstance();
                    currentUser.setUser(user);
                    Intent intentToHomeActivity = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intentToHomeActivity);
                    Toast.makeText(LoginActivity.this, "Welcome, " +
                            CurrentUser.getInstance().getUser().getUsername(), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }

    private Animation headerAlpha() {
        AlphaAnimation animation = new AlphaAnimation(0.2f, Constants.ANIM_ALPHA_TO);
        animation.setDuration(1000);
        animation.setFillAfter(true);
        return animation;
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
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

        findViewById(R.id.loginLayout).post(() -> loadPopUp.showAtLocation(findViewById(R.id.loginLayout),
                Gravity.CENTER, 0, 0));
    }

}
