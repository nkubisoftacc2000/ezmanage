package com.example.ezmanage.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
// This class serves TWO purposes:
//   1. Data model for the app (used in Adapter, Activities)
//   2. Database table definition for Room
// @Entity tells Room: "Create a database table based on this class"
// tableName sets the actual table name in SQLite
@Entity(tableName = "passwords")
public class Password {

    @PrimaryKey(autoGenerate = true)
    private int id;
    // @NonNull means:
    //   1. Compiler warns if null is assigned
    //   2. Room creates the column with NOT NULL constraint
    @NonNull
    private String title;

    @NonNull
    private String login;

    @NonNull
    private String password;
    // Constructor used by ROOM when reading data from the database.
    // Room needs the id parameter because it reads all columns including id.
    public Password(int id, @NonNull String title, @NonNull String login, @NonNull String password) {
        this.id = id;
        this.title = title;
        this.login = login;
        this.password = password;
    }
    // Constructor used by OUR APP CODE when creating a NEW password.
    // We don't have an id yet (database will generate it), so we pass 0.
    // @Ignore tells Room to skip this constructor and use the other one.
    @Ignore
    public Password(@NonNull String title, @NonNull String login, @NonNull String password) {
        this(0, title, login, password);
    }
    // Getters: Allow other classes to read the private fields.
    // Used in PasswordAdapter (to display data)
    // and in MainActivity/SettingsActivity (to read values).
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}