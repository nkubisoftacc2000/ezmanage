package com.example.ezmanage.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ezmanage.R;
import com.example.ezmanage.model.Password;

import java.util.ArrayList;
import java.util.List;
// PasswordAdapter: Bridge between data and RecyclerView.
// Responsible for creating and binding views for each password.
public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    // Currently displayed list (may be filtered)
    private List<Password> passwords;
    // Complete list of all passwords (used as source for filtering)
    private List<Password> allPasswords;
    // Last search query, so we can re-apply filter when data changes
    private String currentQuery = "";

    // ID of the currently expanded item (-1 = none expanded)
    // We use ID instead of position because position changes with filtering
    private int expandedId = -1;

    // Constructor: make a defensive copy of the list
    // so external changes don't affect our internal state
    public PasswordAdapter(List<Password> passwords) {
        this.allPasswords = new ArrayList<>(passwords);
        this.passwords = new ArrayList<>(passwords);
    }
    // Called when RecyclerView needs a NEW ViewHolder.
    // This is called only a few times because RecyclerView RECYCLES ViewHolders.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Convert item_password.xml into an actual View object
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    // Called to bind data to an EXISTING ViewHolder.
    // This is called many times as user scrolls.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Password p = passwords.get(position);

        holder.title.setText(p.getTitle());
        holder.login.setText(p.getLogin());
        holder.password.setText(p.getPassword());

        // SINGLE-EXPAND LOGIC:
        // Only show password if this item is the currently expanded one
        if (p.getId() == expandedId) {
            holder.password.setVisibility(View.VISIBLE);
        } else {
            holder.password.setVisibility(View.GONE);
        }

        // Click to toggle password visibility.
        // If this item is expanded, collapse it.
        // Otherwise, expand it (and collapse any previously expanded item).
        holder.itemView.setOnClickListener(v -> {
            if (expandedId == p.getId()) {
                expandedId = -1;// Collapse
            } else {
                expandedId = p.getId();// Expand this one
            }
            notifyDataSetChanged();// Refresh the list
        });

        holder.itemView.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("password", p.getPassword());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(v.getContext(), "Password copied", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return passwords.size();
    }
    // ViewHolder holds references to the views of one item.
    // This avoids calling findViewById repeatedly (performance optimization).
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, login, password;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            login = itemView.findViewById(R.id.tv_login);
            password = itemView.findViewById(R.id.tv_password);
        }
    }

    // Called when new data arrives from the database (via LiveData).
    // We replace the full list and re-apply the current search filter.
    public void setAllPasswords(List<Password> newPasswords) {
        this.allPasswords = new ArrayList<>(newPasswords);
        filter(currentQuery);// Re-apply search so user's query isn't lost
    }
    // SEARCH FILTER:
    // Filters the displayed list based on title or login.
    // The original list (allPasswords) is never modified.
    public void filter(String query) {
        currentQuery = query == null ? "" : query.trim();

        passwords.clear();

        if (currentQuery.isEmpty()) {
            // No search query: show everything
            passwords.addAll(allPasswords);
        } else {
            // Search: match title or login (case-insensitive)
            String lowerCaseQuery = currentQuery.toLowerCase();

            for (Password p : allPasswords) {
                boolean titleMatches = p.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean loginMatches = p.getLogin().toLowerCase().contains(lowerCaseQuery);

                if (titleMatches || loginMatches) {
                    passwords.add(p);
                }
            }
        }

        notifyDataSetChanged();
    }
    // Get password at position, with bounds checking to prevent crashes.
    // Used by MainActivity for swipe actions.
    public Password getPasswordAt(int position) {
        if (position < 0 || position >= passwords.size()) {
            return null;
        }
        return passwords.get(position);
    }
    // Returns total count of ALL passwords (ignoring filter).
    // Used by MainActivity to show the correct empty state message.
    public int getAllCount() {
        return allPasswords.size();
    }
}