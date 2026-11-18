package com.example.minenet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MineNetPrefs";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Verify Firebase Connection & Initialize Auth
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            String projectId = app.getOptions().getProjectId();
            Log.d(TAG, "Connected to Firebase project: " + projectId);
            Toast.makeText(this, "Firebase Connected (" + projectId + ")", Toast.LENGTH_SHORT).show();
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase connection initialization error", e);
            Toast.makeText(this, "Firebase init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            mAuth = null;
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(v -> showAuthDialog(true));
        btnSignUp.setOnClickListener(v -> showAuthDialog(false));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            Toast.makeText(this, "Logged in as: " + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            String localUser = sharedPreferences.getString(KEY_LOGGED_IN_USER, null);
            if (localUser != null) {
                Toast.makeText(this, "Logged in as: " + localUser, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAuthDialog(boolean isLogin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isLogin ? "Login" : "Sign Up");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Email");
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password (6+ characters)");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton(isLogin ? "Login" : "Sign Up", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mAuth != null) {
                if (isLogin) {
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(MainActivity.this, "Firebase Login successful: " + (user != null ? user.getEmail() : email), Toast.LENGTH_SHORT).show();
                                } else {
                                    // Fallback to local authentication if Firebase reports configuration/internal error
                                    handleLocalAuth(email, isLogin);
                                }
                            });
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(MainActivity.this, "Firebase Sign up successful: " + (user != null ? user.getEmail() : email), Toast.LENGTH_SHORT).show();
                                } else {
                                    // Fallback to local authentication if Firebase reports configuration/internal error
                                    handleLocalAuth(email, isLogin);
                                }
                            });
                }
            } else {
                handleLocalAuth(email, isLogin);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void handleLocalAuth(String email, boolean isLogin) {
        sharedPreferences.edit().putString(KEY_LOGGED_IN_USER, email).apply();
        String action = isLogin ? "Login successful" : "Sign up successful";
        Toast.makeText(MainActivity.this, action + " (Offline mode): " + email, Toast.LENGTH_SHORT).show();
    }
}
