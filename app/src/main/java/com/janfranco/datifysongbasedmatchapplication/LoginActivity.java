package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button loginBtn, showPasswordBtn;
    private EditText eMailInput, passwordInput;
    private ConstraintLayout layout;
    private TextView welcomeLabel, secondLabel, forgotPasswordText;
    private PopupWindow loadPopUp;
    private SQLiteDatabase localDb;

    // ToDo: Write currUser to localDb every time when app is closed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        localDb = openOrCreateDatabase(Constants.DB_USER, Context.MODE_PRIVATE, null);
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_USER +
                "(eMail VARCHAR PRIMARY KEY, username VARCHAR, avatarUrl VARCHAR, bio VARCHAR, " +
                "gender VARCHAR, currTrack VARCHAR, currTrackUri VARCHAR, random INT, " +
                "createDate LONG, currTrackIntervention BOOLEAN)");
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_BLOCKED +
                "(eMail VARCHAR PRIMARY KEY, mail VARCHAR, username VARCHAR, avatarUrl VARCHAR, " +
                "reason VARCHAR, createDate LONG)");
        localDb.execSQL("CREATE TABLE IF NOT EXISTS " +
                Constants.TABLE_MATCH +
                "(eMail VARCHAR PRIMARY KEY, mail VARCHAR)");

        layout = findViewById(R.id.loginLayout);
        layout.setVisibility(View.INVISIBLE);

        loginBtn = findViewById(R.id.loginLoginBtn);
        eMailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        showPasswordBtn = findViewById(R.id.loginShowPasswordBtn);
        forgotPasswordText = findViewById(R.id.loginForgotPasswordText);
        TextView registerText = findViewById(R.id.loginRegisterText);
        mAuth = FirebaseAuth.getInstance();

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
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null)
            updateCurrentUserSingleton(mAuth.getCurrentUser().getEmail());
        else {
            layout.setVisibility(View.VISIBLE);
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

        String qUser = "SELECT * FROM " + Constants.TABLE_USER + " WHERE eMail = '" + eMail +"'";
        String qMatch = "SELECT * FROM " + Constants.TABLE_MATCH + " WHERE eMail = '" + eMail + "'";
        String qBlock = "SELECT * FROM " + Constants.TABLE_BLOCKED + " WHERE eMail = '" + eMail + "'";

        Cursor cursor = localDb.rawQuery(qUser, null);
        Cursor cursorMatch = localDb.rawQuery(qMatch, null);
        Cursor cursorBlock = localDb.rawQuery(qBlock, null);

        int cUsername = cursor.getColumnIndex("username");
        int cAvatarUrl = cursor.getColumnIndex("avatarUrl");
        int cBio = cursor.getColumnIndex("bio");
        int cGender = cursor.getColumnIndex("gender");
        int cCurrTrack = cursor.getColumnIndex("currTrack");
        int cCurrTrackUri = cursor.getColumnIndex("currTrackUri");
        int cRandom = cursor.getColumnIndex("random");
        int cCreateDate = cursor.getColumnIndex("createDate");
        int cCurrTrackIntervention = cursor.getColumnIndex("currTrackIntervention");

        int cMatchMail = cursorMatch.getColumnIndex("mail");

        int cBlockedMail = cursorBlock.getColumnIndex("mail");
        int cBlockedUsername = cursorBlock.getColumnIndex("username");
        int cBlockedAvatarUrl = cursorBlock.getColumnIndex("avatarUrl");
        int cBlockedReason = cursorBlock.getColumnIndex("reason");
        int cBlockedCreateDate = cursorBlock.getColumnIndex("createDate");

        ArrayList<Block> blockedMails = new ArrayList<>();
        ArrayList<String> matches = new ArrayList<>();

        while (cursorMatch.moveToNext()) {
            matches.add(cursorMatch.getString(cMatchMail));
        }

        while (cursorBlock.moveToNext()) {
            blockedMails.add(new Block(
                    cursorBlock.getString(cBlockedMail),
                    cursorBlock.getString(cBlockedUsername),
                    cursorBlock.getString(cBlockedAvatarUrl),
                    cursorBlock.getString(cBlockedReason),
                    cursorBlock.getLong(cBlockedCreateDate)
            ));
        }

        while (cursor.moveToNext()) {
            User user = new User(
                    eMail,
                    cursor.getString(cUsername),
                    cursor.getString(cAvatarUrl),
                    cursor.getString(cBio),
                    cursor.getString(cGender),
                    cursor.getString(cCurrTrack),
                    cursor.getString(cCurrTrackUri),
                    matches,
                    blockedMails,
                    cursor.getInt(cRandom),
                    cursor.getLong(cCreateDate),
                    (cursor.getInt(cCurrTrackIntervention) == 1)
            );
            CurrentUser.getInstance().setUser(user);
        }
        cursor.close();
        cursorMatch.close();
        cursorBlock.close();

        boolean canIntent = true;
        if (CurrentUser.getInstance().getUser() != null) {
            Intent intentToHomeActivity = new Intent(LoginActivity.this, HomeActivity.class);
            intentToHomeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentToHomeActivity);
            Toast.makeText(LoginActivity.this, "Welcome, " +
                    CurrentUser.getInstance().getUser().getUsername(), Toast.LENGTH_LONG).show();
            canIntent = false;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean finalCanIntent = canIntent;
        db.collection("userDetail").document(eMail).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        assert user != null;
                        CurrentUser currentUser = CurrentUser.getInstance();
                        currentUser.setUser(user);
                        writeToLocalDb();
                        if (finalCanIntent) {
                            Intent intentToHomeActivity = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intentToHomeActivity);
                        }
                        LoginActivity.this.finish();
                        Toast.makeText(LoginActivity.this, "Welcome, " +
                                CurrentUser.getInstance().getUser().getUsername(), Toast.LENGTH_LONG).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this, e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
                mAuth.signOut();
            }
        });
    }

    private void writeToLocalDb() {
        CurrentUser currentUser = CurrentUser.getInstance();
        User currUser = currentUser.getUser();

        String matchQuery = "REPLACE INTO " + Constants.TABLE_MATCH +
                " (eMail, mail) VALUES (?, ?)";
        for (int i=0; i<currUser.getMatches().size(); i++) {
            String matchMail = currUser.getMatches().get(i);
            SQLiteStatement sqLiteStatement = localDb.compileStatement(matchQuery);
            sqLiteStatement.bindString(1, currUser.geteMail());
            sqLiteStatement.bindString(2, matchMail);

            try {
                sqLiteStatement.execute();
            } catch (Exception e) {
                Log.d("LocalDBError", Objects.requireNonNull(e.getLocalizedMessage()));
            }
        }

        String blockedQuery = "REPLACE INTO " + Constants.TABLE_BLOCKED +
                " (eMail, mail, username, avatarUrl, reason, createDate) VALUES (?, ?, ?, ?, ?, ?)";
        for (int i=0; i<currUser.getBlockedMails().size(); i++) {
            Block blockedUser = currUser.getBlockedMails().get(i);
            SQLiteStatement sqLiteStatement = localDb.compileStatement(blockedQuery);
            sqLiteStatement.bindString(1, currUser.geteMail());
            sqLiteStatement.bindString(2, blockedUser.getMail());
            sqLiteStatement.bindString(3, blockedUser.getAvatarUrl());
            sqLiteStatement.bindString(4, blockedUser.getReason());
            sqLiteStatement.bindLong(5, blockedUser.getCreateDate());

            try {
                sqLiteStatement.execute();
                Log.d("LocalDB", "Wrote successfully!");
            } catch (Exception e) {
                Log.d("LocalDBError", Objects.requireNonNull(e.getLocalizedMessage()));
            }
        }

        String userQuery = "REPLACE INTO " + Constants.TABLE_USER + " (eMail, username, avatarUrl, " +
                "bio, gender, currTrack, currTrackUri, random, createDate, currTrackIntervention) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteStatement sqLiteStatement = localDb.compileStatement(userQuery);
        sqLiteStatement.bindString(1, currUser.geteMail());
        sqLiteStatement.bindString(2, currUser.getUsername());
        sqLiteStatement.bindString(3, currUser.getAvatarUrl());
        sqLiteStatement.bindString(4, currUser.getBio());
        sqLiteStatement.bindString(5, currUser.getGender());
        sqLiteStatement.bindString(6, currUser.getCurrTrack());
        sqLiteStatement.bindString(7, currUser.getCurrTrackUri());
        sqLiteStatement.bindLong(8, currUser.getRandom());
        sqLiteStatement.bindLong(9, currUser.getCreateDate());
        sqLiteStatement.bindLong(10, currUser.isCurrTrackIntervention() ? 1 : 0);

        try {
            sqLiteStatement.execute();
        } catch (Exception e) {
            Log.d("LocalDBError", Objects.requireNonNull(e.getLocalizedMessage()));
        }
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
        loadPopUp.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),
                Gravity.CENTER, 0, 0);
    }

}
