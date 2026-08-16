package com.example.ezmanage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ezmanage.database.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MasterPasswordActivity extends AppCompatActivity {

    private TextInputEditText etMasterPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout tilConfirmPassword;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private MaterialButton btnSubmit;

    // Stores the HASH of master password (never the plain password)
    private SharedPreferences prefs;
    // True if user has never set a master password before
    private boolean isFirstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_password);

        prefs = getSharedPreferences("EzManagePrefs", MODE_PRIVATE);

        etMasterPassword = findViewById(R.id.etMasterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Check if this is the first launch by looking for a stored hash
        String storedHash = prefs.getString("master_hash", null);
        isFirstTime = (storedHash == null);

        // Adjust UI based on mode (Create vs Unlock)
        setupUiForMode();

        btnSubmit.setOnClickListener(v -> {
            String input = etMasterPassword.getText().toString().trim();

            if (TextUtils.isEmpty(input)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFirstTime) {
                String confirm = etConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(confirm)) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!input.equals(confirm)) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (input.length() < 4) {
                    Toast.makeText(this, "Master password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Delete any old database that might exist from previous installs.
                // This prevents "file is not a database" errors when the
                // passphrase changes.
                AppDatabase.deleteDatabase(this);
                // Store only the HASH, never the plain password
                prefs.edit().putString("master_hash", hashPassword(input)).apply();
                proceedToMain(input);
            } else {
                // Compare entered password's hash with stored hash
                if (storedHash.equals(hashPassword(input))) {
                    proceedToMain(input);
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    etMasterPassword.setText("");
                }
            }
        });
    }

    private void setupUiForMode() {
        if (isFirstTime) {
            tvTitle.setText("Create Master Password");
            tvSubtitle.setText("Set a master password to secure your passwords");
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("Create");
        } else {
            tvTitle.setText("Enter Master Password");
            tvSubtitle.setText("Enter your master password to continue");
            tilConfirmPassword.setVisibility(View.GONE);
            btnSubmit.setText("Unlock");
        }
    }

    private void proceedToMain(String passphrase) {
        Intent intent = new Intent(this, MainActivity.class);
        // Pass the passphrase to MainActivity so it can decrypt the database
        intent.putExtra("DB_PASSPHRASE", passphrase);
        startActivity(intent);
        // finish() removes this activity from back stack.
        // User cannot go back to password screen with back button.
        finish();
    }

    // SHA-256 hashing:
    // - One-way: cannot reverse hash to get original password
    // - Deterministic: same input always gives same output
    // - Fixed length: always 64 hex characters
    private String hashPassword(String password) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            // Convert each byte to 2-digit hexadecimal
            // 0xff & b ensures the byte is treated as unsigned
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');// pad with leading zero
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}