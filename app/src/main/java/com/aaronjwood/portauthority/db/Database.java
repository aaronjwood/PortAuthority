package com.aaronjwood.portauthority.db;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Database {

    private static final String TAG = "Database";

    private Activity activity;

    public Database(Activity activity) {
        this.activity = activity;
    }

    /**
     * Checks if the database exists at the application's data directory
     *
     * @param dbName Name of the database to check the existence of
     * @return True if the database exists, false if not
     */
    private boolean checkDatabase(String dbName) {
        File dbFile = new File(this.activity.getApplicationInfo().dataDir + "/" + dbName);
        return dbFile.exists();
    }

    /**
     * Copies the database from assets to the application's data directory
     *
     * @param dbName Name of the database to be copied
     */
    private void copyDatabase(String dbName) {
        try {
            InputStream input = this.activity.getAssets().open(dbName);
            OutputStream output = new FileOutputStream(this.activity.getApplicationInfo().dataDir + "/" + dbName);
            byte[] buffer = new byte[1024];
            int length;
            while((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.close();
            input.close();
        }
        catch(IOException e) {
            Log.e(TAG, e.toString());
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Opens a connection to a SQLite database
     *
     * @param dbName The database to open a connection to
     * @return Database connection
     */
    private SQLiteDatabase openDatabase(String dbName) {
        if(!this.checkDatabase(dbName)) {
            this.copyDatabase(dbName);
        }
        try {
            return SQLiteDatabase.openDatabase(this.activity.getApplicationInfo().dataDir + "/" + dbName, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch(SQLiteException e) {
            return null;
        }
    }

    /**
     * Performs a query against the database
     *
     * @param dbName The database to query
     * @param query  The query itself
     * @param args   Arguments for any bound parameters
     * @return Cursor for iterating over results
     */
    public Cursor queryDatabase(String dbName, String query, String[] args) {
        SQLiteDatabase db = this.openDatabase(dbName);
        return db.rawQuery(query, args);
    }

}
