package com.example.ezmanage;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ezmanage.adapter.PasswordAdapter;
import com.example.ezmanage.database.AppDatabase;
import com.example.ezmanage.model.Password;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    // Adapter connects our data to the RecyclerView
    private PasswordAdapter adapter;
    // Encrypted Room database instance
    private AppDatabase db;
    // Background thread for database operations (prevents UI freezing)
    private ExecutorService executor;

    // Database passphrase received from MasterPasswordActivity
    private String dbPassphrase;

    // Shown when the list is empty
    private TextView tvEmpty;
    // Search field in the header
    private EditText etSearch;

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

        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);

        dbPassphrase = getIntent().getStringExtra("DB_PASSPHRASE");

        // SECURITY CHECK: If no passphrase, redirect to MasterPasswordActivity
        if (dbPassphrase == null) {
            startActivity(new Intent(this, MasterPasswordActivity.class));
            finish();
            return;
        }

        executor = Executors.newSingleThreadExecutor();
        db = AppDatabase.getInstance(this, dbPassphrase);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PasswordAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // OBSERVE: Automatically update UI whenever database changes
        db.passwordDao().getAllPasswords().observe(this, passwords -> {
            adapter.setAllPasswords(passwords);
            adapter.filter(etSearch.getText().toString().trim());
            updateEmptyState();
        });

        findViewById(R.id.fabAddPassword).setOnClickListener(v -> showAddDialog());

        setupSwipeActions(recyclerView);
        setupHeader();
    }

    private void setupHeader() {
        ImageView ivSettings = findViewById(R.id.ivSettings);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString().trim());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("CURRENT_PASSPHRASE", dbPassphrase);
            startActivity(intent);
        });
    }

    private void updateEmptyState() {
        if (adapter == null || tvEmpty == null) return;

        if (adapter.getAllCount() == 0) {
            tvEmpty.setText("No passwords added yet");
            tvEmpty.setVisibility(View.VISIBLE);
        } else if (adapter.getItemCount() == 0) {
            tvEmpty.setText("No passwords found");
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null);

        TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        TextInputEditText etLogin = view.findViewById(R.id.etLogin);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        // We set PositiveButton listener to null here
        // so the dialog does NOT auto-dismiss.
        // We want to validate first, then dismiss manually.
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Add Password")
                .setView(view)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String login = etLogin.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (title.isEmpty() || login.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Insert on background thread (Room forbids main-thread DB access)
                executor.execute(() ->
                        db.passwordDao().insert(new Password(title, login, password))
                );

                dialog.dismiss();
            });
        });

        dialog.show();
    }
    // Reuse the same layout as Add dialog, but pre-fill with
    private void showEditDialog(Password password, int position) {
        if (password == null) {
            adapter.notifyItemChanged(position);
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null);

        TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        TextInputEditText etLogin = view.findViewById(R.id.etLogin);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);

        etTitle.setText(password.getTitle());
        etLogin.setText(password.getLogin());
        etPassword.setText(password.getPassword());

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Edit Password")
                .setView(view)
                .setPositiveButton("Save", null)
                // If user cancels, restore the swiped item to its original state
                .setNegativeButton("Cancel", (dialog, which) ->
                        adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog ->
                        adapter.notifyItemChanged(position));

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String login = etLogin.getText().toString().trim();
                String newPassword = etPassword.getText().toString().trim();

                // Validate: all fields must be filled
                if (title.isEmpty() || login.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                Password updated = new Password(password.getId(), title, login, newPassword);

                executor.execute(() ->
                        db.passwordDao().update(updated)
                );

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void setupSwipeActions(RecyclerView recyclerView) {
        // 0 = no drag, LEFT|RIGHT = enable both swipe directions
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Password password = adapter.getPasswordAt(position);

                if (password == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }
                // Swipe LEFT = Delete (with confirmation dialog)
                // Swipe RIGHT = Edit
                if (direction == ItemTouchHelper.LEFT) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete password?")
                            .setMessage("This action cannot be undone.")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                executor.execute(() -> db.passwordDao().delete(password));
                            })
                            .setNegativeButton("Cancel", (dialog, which) ->
                                    adapter.notifyItemChanged(position))
                            .setOnCancelListener(dialog ->
                                    adapter.notifyItemChanged(position))
                            .show();
                } else {
                    showEditDialog(password, position);
                }
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            // Shutdown executor to prevent memory leaks
            executor.shutdown();
        }
    }
}