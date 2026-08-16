package com.example.ezmanage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ezmanage.database.AppDatabase;
import com.example.ezmanage.model.Password;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    // Current encrypted database (with old passphrase)
    private AppDatabase db;
    // Stores the HASH of master password
    private SharedPreferences prefs;
    // Background thread for heavy operations
    private ExecutorService executor;

    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("EzManagePrefs", MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();

        // Back button: close this activity and return to MainActivity
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        MaterialButton btnChangeMasterPassword = findViewById(R.id.btnChangeMasterPassword);

        String currentPassphrase = getIntent().getStringExtra("CURRENT_PASSPHRASE");

        // SECURITY CHECK: If no passphrase received, redirect to MasterPasswordActivity
        if (currentPassphrase == null) {
            startActivity(new Intent(this, MasterPasswordActivity.class));
            finish();
            return;
        }

        db = AppDatabase.getInstance(this, currentPassphrase);
        final String oldPassphrase = currentPassphrase;

        btnChangeMasterPassword.setOnClickListener(v -> {
            String current = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            // Validate: check if master password is set
            String storedHash = prefs.getString("master_hash", null);

            if (storedHash == null) {
                Toast.makeText(this, "Master password is not set", Toast.LENGTH_SHORT).show();
                return;
            }

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!current.equals(oldPassphrase) || !hashPassword(current).equals(storedHash)) {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 4) {
                Toast.makeText(this, "Master password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            changeMasterPassword(newPass, btnChangeMasterPassword);
        });
    }

    private void changeMasterPassword(String newPassphrase, MaterialButton btnChangeMasterPassword) {
        // Disable button to prevent double-clicks during long operation
        btnChangeMasterPassword.setEnabled(false);

        final Context appContext = getApplicationContext();

        executor.execute(() -> {
            try {
                // Read all passwords from old database
                // Using sync version because we're in background thread
                List<Password> data = db.passwordDao().getAllPasswordsSync();

                AppDatabase.deleteDatabase(appContext);

                AppDatabase newDb = AppDatabase.getInstance(appContext, newPassphrase);

                for (Password p : data) {
                    newDb.passwordDao().insert(
                            new Password(p.getTitle(), p.getLogin(), p.getPassword())
                    );
                }

                prefs.edit().putString("master_hash", hashPassword(newPassphrase)).apply();

                runOnUiThread(() ->
                        Toast.makeText(this, "Master password changed", Toast.LENGTH_SHORT).show()
                );
                // Restart MainActivity with new passphrase
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("DB_PASSPHRASE", newPassphrase);
                // FLAG_ACTIVITY_CLEAR_TASK: Clear all previous activities from back stack
                // This ensures security: user cannot go back to old state
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            } catch (Exception e) {
                // Error handling: if anything fails, show error and re-enable button
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to change master password", Toast.LENGTH_SHORT).show();
                    btnChangeMasterPassword.setEnabled(true);
                });
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}