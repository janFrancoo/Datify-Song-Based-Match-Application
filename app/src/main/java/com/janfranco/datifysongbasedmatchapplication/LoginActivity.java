package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.Map;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private final int PASSWORD_MIN_LEN = 5;
    private final String EMAIL_REGEX = "^(.+)@([a-zA-Z\\d-]+)\\.([a-zA-Z]+)(\\.[a-zA-Z]+)?$";

    private FirebaseAuth mAuth;
    private Button loginBtn;
    private EditText eMailInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.loginLoginBtn);
        eMailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        Button showPasswordBtn = findViewById(R.id.loginShowPasswordBtn);
        TextView forgotPasswordText = findViewById(R.id.loginForgotPasswordText);
        TextView registerText = findViewById(R.id.loginRegisterText);
        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginBtn.isEnabled())
                    login();
            }
        });

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
                return Pattern.matches(EMAIL_REGEX, eMailInput.getText().toString().trim()) &&
                        passwordInput.getText().toString().length() >= PASSWORD_MIN_LEN;
            }
        };

        eMailInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!checkInternet()) {
            loginBtn.setEnabled(false);
            eMailInput.setEnabled(false);
            passwordInput.setEnabled(false);
            Toast.makeText(this, "Check your internet connection and try again!",
                    Toast.LENGTH_LONG).show();
        } else if (mAuth.getCurrentUser() != null) {
            updateCurrentUserSingleton(mAuth.getCurrentUser().getEmail());
        }
    }

    private void login() {
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

        Button sendMail = popupView.findViewById(R.id.popUpSendMail);
        final EditText email = popupView.findViewById(R.id.popUpEmailInput);
        sendMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Pattern.matches(EMAIL_REGEX, email.getText().toString().trim()))
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("userDetail").document(eMail).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            User user = new User((String) data.get("username"),
                                    (String) data.get("eMail"),
                                    (String) data.get("bio"));
                            updateCurrentUser(user);
                            intentToHomeActivity();
                        }
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

    private void updateCurrentUser(User user) {
        CurrentUser currentUser = CurrentUser.getInstance();
        currentUser.setUser(user);
    }

    private void intentToHomeActivity() {
        Intent intentToHomeActivity = new Intent(LoginActivity.this, HomeActivity.class);
        intentToHomeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentToHomeActivity);
        finish();
    }

    private boolean checkInternet() {
        NetworkUtil internet = new NetworkUtil();
        Thread thread = new Thread(internet);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return internet.getValue();
    }

}
