package com.example.ezmanage.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ezmanage.model.Password;

import java.util.List;
// ============================================================
// PasswordDao: Data Access Object for the Password entity.
//
// This interface defines ALL database operations for passwords.
// Room automatically generates the implementation at compile time.
// We only define WHAT we want, Room writes the HOW (SQL code).
//
// Why interface? Because Room generates the actual implementation
// class (PasswordDao_Impl) during compilation. We never write it.
// ============================================================


// @Dao tells Room: "This is a Data Access Object, generate its implementation"
@Dao
public interface PasswordDao {

    @Insert
    void insert(Password password);

    @Delete
    void delete(Password password);

    @Update
    void update(Password password);

    // GET ALL (Live): Returns all passwords sorted alphabetically by title.
    // LiveData means the UI automatically updates when data changes.
    // Used in MainActivity to display the list.
    @Query("SELECT * FROM passwords ORDER BY title ASC")
    LiveData<List<Password>> getAllPasswords();
    // GET ALL (Sync): Same query, but returns a plain List instead of LiveData.
    // Used in SettingsActivity on a background thread where we need
    // to read all data at once (no live updates needed).
    @Query("SELECT * FROM passwords ORDER BY title ASC")
    List<Password> getAllPasswordsSync();
}