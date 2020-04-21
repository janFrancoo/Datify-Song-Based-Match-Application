package com.janfranco.datifysongbasedmatchapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ImageView avatar;
    private Spinner genderSpinner;
    private TextView bottomLabel;
    private Button nextSkipBtn;
    private boolean fromRegister;
    private String selectedGender = "";
    private EditText bio;
    private Uri imgData;
    private PopupWindow loadPopUp;
    private String avatarUrlOld;
    private CurrentUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentUser = CurrentUser.getInstance();
        fromRegister = getIntent().getBooleanExtra("register", false);

        avatar = findViewById(R.id.settingsAvatar);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        bottomLabel = findViewById(R.id.settingsBottomLabel);
        bottomLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fromRegister)
                    skip();
                else
                    createPopUp();
            }
        });

        nextSkipBtn = findViewById(R.id.settingsSaveBtn);
        nextSkipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
            }
        });

        genderSpinner = findViewById(R.id.settingsSpinner);
        bio = findViewById(R.id.settingsBio);

        Typeface metropolisLight = Typeface.createFromAsset(getAssets(), "fonts/Metropolis-Light.otf");
        Typeface metropolisExtraLightItalic = Typeface.createFromAsset(getAssets(),
                "fonts/Metropolis-ExtraLightItalic.otf");

        bio.setTypeface(metropolisExtraLightItalic);
        bottomLabel.setTypeface(metropolisLight);

        ArrayList<String> genders = new ArrayList<>();
        genders.add("Select gender");
        genders.add("Male");
        genders.add("Female");
        genders.add("Neutral");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_text, genders);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_text);
        genderSpinner.setAdapter(arrayAdapter);

        if (fromRegister)
            updateComponentsFirstTime();
        else
            getDataAndUpdateComponents();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        bio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    bio.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).start();
                else
                    bio.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!fromRegister) {
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.settings_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings_activity_user_signout) {
            mAuth.signOut();
            Intent intentToLogin = new Intent(this, LoginActivity.class);
            intentToLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentToLogin);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentMedia = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentMedia, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intentMedia = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentMedia, 2);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            imgData = data.getData();
            try {
                Bitmap chosenAvatar;
                if (Build.VERSION.SDK_INT >= 28) {
                    assert imgData != null;
                    ImageDecoder.Source source = ImageDecoder.createSource(
                            this.getContentResolver(), imgData);
                    chosenAvatar = ImageDecoder.decodeBitmap(source);
                    avatar.setImageBitmap(chosenAvatar);
                } else {
                    chosenAvatar = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgData);
                    avatar.setImageBitmap(chosenAvatar);
                }
            } catch (IOException e) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetTextI18n")
    private void updateComponentsFirstTime() {
        bottomLabel.setText("Click here if you want to skip, you can change these settings later.");
        nextSkipBtn.setText("Next");
    }

    private void getDataAndUpdateComponents() {
        avatarUrlOld = currentUser.getUser().getAvatarUrl();

        bio.setText(currentUser.getUser().getBio());
        if (!currentUser.getUser().getAvatarUrl().equals("default"))
            Picasso.get().load(currentUser.getUser().getAvatarUrl()).into(avatar);

        switch (currentUser.getUser().getGender()) {
            case "unknown":
                genderSpinner.setSelection(0, true);
                break;
            case "Male":
                genderSpinner.setSelection(1, true);
                break;
            case "Female":
                genderSpinner.setSelection(2, true);
                break;
            default:
                genderSpinner.setSelection(3, true);
                break;
        }
    }

    private void skip() {
        Intent intentToHome = new Intent(SettingsActivity.this, LoginActivity.class);
        intentToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentToHome);
        finish();
    }

    private void update() {
        loadPopUp();
        if (selectedGender.equals("Select gender"))
            currentUser.getUser().setGender("unknown");
        else
            currentUser.getUser().setGender(selectedGender);
        currentUser.getUser().setBio(bio.getText().toString().trim());

        if (imgData != null) {
            UUID uuid = UUID.randomUUID();
            final String imgName = "avatars/" + uuid + ".jpg";
            storage.getReference().child(imgName).putFile(imgData)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            storage.getReference(imgName).getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            currentUser.getUser().setAvatarUrl(uri.toString());
                                            updateDb();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        // Image has been uploaded but could not get download uri
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(SettingsActivity.this, e.getLocalizedMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Image could not get uploaded
                            Toast.makeText(SettingsActivity.this, e.getLocalizedMessage(),
                                    Toast.LENGTH_LONG).show();
                            if (loadPopUp != null)
                                loadPopUp.dismiss();
                        }
                    });
        } else {
            updateDb();
        }
    }

    private void updateDb() {
        db.collection("userDetail").document(currentUser.getUser().geteMail()).set(currentUser.getUser())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(SettingsActivity.this, "Settings are successfully applied!",
                                Toast.LENGTH_LONG).show();
                        updateChats();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this, e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                        if (loadPopUp != null)
                            loadPopUp.dismiss();
                    }
                });
    }

    private void createPopUp() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_contact,
                null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        popupWindow.setElevation(50);
        popupWindow.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content),
                Gravity.CENTER, 0, 0);

        Button sendIssue = popupView.findViewById(R.id.popUpSendIssue);
        final EditText issue = popupView.findViewById(R.id.popUpIssueInput);
        sendIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (issue.getText().toString().trim().length() > 5) {
                    saveIssue(issue.getText().toString());
                    popupWindow.dismiss();
                }
            }
        });

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private void saveIssue(String issueText) {
        long createDate = Timestamp.now().getSeconds();
        Issue issue = new Issue(currentUser.getUser().geteMail(), issueText, createDate);
        db.collection("report").add(issue)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Toast.makeText(SettingsActivity.this, "Your report has sent.",
                        Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SettingsActivity.this, e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
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
        popUpLoadLabel.setText("Updating...");
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

    private void updateChats() {
        String avatarUrlNew = currentUser.getUser().getAvatarUrl();
        if (avatarUrlOld != null && !avatarUrlOld.equals(avatarUrlNew)) {
            ArrayList<String> chatNames = generateChatNames();
            WriteBatch updateBatch = db.batch();
            for (int i=0; i<chatNames.size(); i++) {
                int offset = getOffsetOfCharSequence(
                        chatNames.get(i),
                        currentUser.getUser().geteMail()
                );
                if (offset == 0)
                    updateBatch.update(db.collection("chat").document(chatNames.get(i)),
                            "avatar1", currentUser.getUser().getAvatarUrl());
                else
                    updateBatch.update(db.collection("chat").document(chatNames.get(i)),
                            "avatar2", currentUser.getUser().getAvatarUrl());
            }
            updateBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (loadPopUp != null)
                        loadPopUp.dismiss();
                    Intent intentToHome = new Intent(SettingsActivity.this, HomeActivity.class);
                    startActivity(intentToHome);
                }
            });
        } else {
            if (loadPopUp != null)
                loadPopUp.dismiss();
            Intent intentToHome = new Intent(SettingsActivity.this, HomeActivity.class);
            startActivity(intentToHome);
        }
    }

    private ArrayList<String> generateChatNames() {
        ArrayList<String> chatNames = new ArrayList<>();
        for (int i=0; i<currentUser.getUser().getMatches().size(); i++) {
            String matchMail = currentUser.getUser().getMatches().get(i);
            if (currentUser.getUser().geteMail().compareTo(matchMail) < 0)
                chatNames.add(currentUser.getUser().geteMail() + "_" + matchMail);
            else
                chatNames.add(matchMail + "_" + currentUser.getUser().geteMail());
        }
        return chatNames;
    }

    private int getOffsetOfCharSequence(String chatName, String mail) {
        int match = 0;
        int mailLen = mail.length();

        for (int i=0; i<chatName.length(); i++) {
            if (chatName.charAt(i) == mail.charAt(match)) {
                match += 1;
                if (match == mailLen)
                    return i - mailLen + 1;
            }
            else
                match = 0;
        }

        return -1;
    }

}
