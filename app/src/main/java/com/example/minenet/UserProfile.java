package com.example.minenet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MineNetPrefs";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_BIO = "bio";

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView bioTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        nameTextView = findViewById(R.id.textView);
        emailTextView = findViewById(R.id.textView2);
        bioTextView = findViewById(R.id.textViewBio);
        View editProfileButton = findViewById(R.id.btnEditProfile);
        View logoutButton = findViewById(R.id.button);

        loadUserProfile();

        editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        logoutButton.setOnClickListener(v -> {
            if (mAuth != null) {
                mAuth.signOut();
            }
            sharedPreferences.edit().remove(KEY_LOGGED_IN_USER).remove(KEY_USERNAME).remove(KEY_BIO).apply();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null)
                ? currentUser.getEmail()
                : sharedPreferences.getString(KEY_LOGGED_IN_USER, "player@minenet.com");

        emailTextView.setText(userEmail);

        String docId = (currentUser != null) ? currentUser.getUid() : "profile";

        // Fetch from Firestore collection "Users", document <UID>
        db.collection("Users").document(docId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");

                        if (username != null && !username.isEmpty()) {
                            nameTextView.setText(username);
                        }
                        if (bio != null && !bio.isEmpty()) {
                            bioTextView.setText(bio);
                        } else {
                            bioTextView.setText("No bio yet");
                        }
                    } else {
                        loadLocalProfile(userEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    loadLocalProfile(userEmail);
                });
    }

    private void loadLocalProfile(String userEmail) {
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, null);
        String savedBio = sharedPreferences.getString(KEY_BIO, null);

        if (savedUsername != null && !savedUsername.isEmpty()) {
            nameTextView.setText(savedUsername);
        } else {
            nameTextView.setText("Player Profile");
        }

        if (savedBio != null && !savedBio.isEmpty()) {
            bioTextView.setText(savedBio);
        } else {
            bioTextView.setText("No bio yet");
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Player Profile");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(usernameInput);

        final EditText bioInput = new EditText(this);
        bioInput.setHint("Bio (e.g. Building castles in Minecraft)");
        bioInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(bioInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUsername = usernameInput.getText().toString().trim();
            String newBio = bioInput.getText().toString().trim();

            if (!newUsername.isEmpty()) {
                saveProfileToFirebase(newUsername, newBio);
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveProfileToFirebase(String username, String bio) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null)
                ? currentUser.getEmail()
                : sharedPreferences.getString(KEY_LOGGED_IN_USER, "player@minenet.com");

        sharedPreferences.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_BIO, bio)
                .apply();

        String uid = (currentUser != null) ? currentUser.getUid() : "local_user";

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", uid);
        userData.put("username", username);
        userData.put("bio", bio);
        userData.put("profile_image_path", "");

        // Save to Firestore collection "Users", document <UID>
        db.collection("Users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile saved to Firestore!", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Saved locally (Firestore error: " + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                    loadUserProfile();
                });
    }
}
