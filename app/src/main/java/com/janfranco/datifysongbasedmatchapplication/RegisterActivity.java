package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText eMailInput, usernameInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        eMailInput = findViewById(R.id.registerEMailInput);
        usernameInput = findViewById(R.id.registerUsernameInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        final Button registerBtn = findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerBtn.isEnabled())
                    checkUsername();
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
        final String eMail = eMailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(eMail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final User user = new User(eMail, username);
                    db.collection("userDetail").document(eMail).set(user)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        CurrentUser currentUser = CurrentUser.getInstance();
                                        currentUser.setUser(user);
                                        Toast.makeText(RegisterActivity.this, "Successfully registered!",
                                                Toast.LENGTH_LONG).show();
                                        intentToHome();
                                    } else {
                                        // Remove user
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
                }
            }
        });
    }

    private void intentToHome() {
        Intent intentToHome = new Intent(this, HomeActivity.class);
        intentToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentToHome);
        finish();
    }

}
