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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        if (fromRegister)
            updateComponentsFirstTime();
        else
            getDataAndUpdateComponents();
    }

    @Override
    protected void onStart() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ArrayList<String> genders = new ArrayList<>();
        genders.add("Select gender");
        genders.add("Male");
        genders.add("Female");
        genders.add("Other");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(arrayAdapter);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        super.onStart();
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
        CurrentUser currentUser = CurrentUser.getInstance();
        User user = currentUser.getUser();

        bio.setText(user.getBio());
        if (!user.getAvatarUrl().equals("default"))
            Picasso.get().load(user.getAvatarUrl()).into(avatar);
        switch (user.getGender()) {
            case "":
                genderSpinner.setSelection(0);
                break;
            case "Male":
                genderSpinner.setSelection(1);
                break;
            case "Female":
                genderSpinner.setSelection(2);
                break;
            default:
                genderSpinner.setSelection(3);
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
        // ToDo: Fix update problem => If avatar does not change but bio or gender changes, update does not work!
        final CurrentUser currentUser = CurrentUser.getInstance();
        final User user = currentUser.getUser();
        user.setGender(selectedGender);
        user.setBio(bio.getText().toString().trim());

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
                                            user.setAvatarUrl(uri.toString());
                                            currentUser.setUser(user);
                                            db.collection("userDetail").document(user.geteMail()).set(user)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(SettingsActivity.this, "Settings are successfully applied!",
                                                                    Toast.LENGTH_LONG).show();
                                                            Intent intentToHome = new Intent(SettingsActivity.this, LoginActivity.class);
                                                            startActivity(intentToHome);
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(SettingsActivity.this, e.getLocalizedMessage(),
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    });
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
                        }
                    });
        }
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
        CurrentUser user = CurrentUser.getInstance();
        Issue issue = new Issue(user.getUser().geteMail(), issueText);
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

}
