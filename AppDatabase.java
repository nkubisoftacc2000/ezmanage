package com.example.ezmanage.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.ezmanage.dao.PasswordDao;
import com.example.ezmanage.model.Password;

import net.sqlcipher.database.SupportFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
// ============================================================
// AppDatabase: Singleton database manager with SQLCipher encryption.
//
// This class is the ONLY entry point to the database.
// It ensures:
//   1. Only ONE database instance exists (Singleton)
//   2. Database is encrypted with SQLCipher (AES-256)
//   3. Thread-safe access (Double-Checked Locking + volatile)
//   4. Proper cleanup (deleteDatabase removes all related files)
// ============================================================
@Database(entities = {Password.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Database file name on device storage
    private static final String DB_NAME = "ezmanage_db";
    // The SINGLE instance of the database (Singleton pattern).
    // volatile ensures visibility
    private static volatile AppDatabase INSTANCE;
    // Room generates the implementation of this method at compile time.
    // Returns the DAO for password CRUD operations.

    public abstract PasswordDao passwordDao();
    // ============================================================
    // getInstance: The ONLY way to access the database.
    // Uses Double-Checked Locking for thread safety:
    //   1st check: Fast path (no lock) if instance already exists
    //   synchronized: Only one thread can enter this block
    //   2nd check: Verify instance is still null after acquiring lock
    // ============================================================
    public static AppDatabase getInstance(Context context, String passphrase) {
        // SECURITY: Database cannot be opened without a passphrase
        if (passphrase == null) {
            throw new IllegalArgumentException("Database passphrase cannot be null");
        }

        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Convert passphrase to bytes for SQLCipher
                    byte[] key = passphrase.getBytes(StandardCharsets.UTF_8);
                    // SupportFactory connects Room to SQLCipher encryption
                    SupportFactory factory = new SupportFactory(key);

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .openHelperFactory(factory)// ← THIS LINE ENABLES ENCRYPTION
                            .build();
                }
            }
        }

        return INSTANCE;
    }
    // ============================================================
    // clearInstance: Close the database and reset the singleton.
    // Used before deleting or recreating the database.
    // ============================================================
    public static void clearInstance() {
        if (INSTANCE != null) {
            if (INSTANCE.isOpen()) {
                INSTANCE.close();// Close only if open (avoids errors)
            }
            INSTANCE = null;// Reset singleton so next getInstance creates fresh
        }
    }

    public static void deleteDatabase(Context context) {
        clearInstance();// Close database first

        File dbFile = context.getDatabasePath(DB_NAME);
        if (dbFile == null) return;
        // Delete main database file
        context.deleteDatabase(DB_NAME);
        // Delete all SQLite helper files
        deleteFileIfExists(dbFile);
        deleteFileIfExists(new File(dbFile.getAbsolutePath() + "-wal"));
        deleteFileIfExists(new File(dbFile.getAbsolutePath() + "-shm"));
        deleteFileIfExists(new File(dbFile.getAbsolutePath() + "-journal"));
    }

    private static void deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}