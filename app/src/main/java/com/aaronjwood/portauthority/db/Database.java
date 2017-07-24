package com.aaronjwood.portauthority.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Database {
    private Context context;
    private SQLiteDatabase db;

    public Database(Context context) {
        this.context = context;
    }

    /**
     * Checks if the database exists at the application's data directory
     *
     * @param dbName Name of the database to check the existence of
     * @return True if the database exists, false if not
     */
    private boolean checkDatabase(String dbName) {
        File dbFile = new File(context.getApplicationInfo().dataDir + "/" + dbName);

        return dbFile.exists();
    }

    /**
     * Copies the database from assets to the application's data directory
     *
     * @param dbName Name of the database to be copied
     */
    private void copyDatabase(String dbName) throws IOException {
        InputStream input = context.getAssets().open(dbName);
        OutputStream output = new FileOutputStream(context.getApplicationInfo().dataDir + "/" + dbName);

        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            output.close();
            input.close();
        }
    }

    /**
     * Opens a connection to a SQLite database
     *
     * @param dbName The database to open a connection to
     */
    public void openDatabase(String dbName) throws IOException, SQLiteException {
        if (!checkDatabase(dbName)) {
            copyDatabase(dbName);
        }

        db = SQLiteDatabase.openDatabase(context.getApplicationInfo().dataDir + "/" + dbName, null, SQLiteDatabase.OPEN_READONLY);
    }

    /**
     * Performs a query against the database
     *
     * @param query The query itself
     * @param args  Arguments for any bound parameters
     * @return Cursor for iterating over results
     */
    public Cursor queryDatabase(String query, String[] args) {
        return db.rawQuery(query, args);
    }

    /**
     * Closes the database handle
     */
    public void close() {
        db.close();
    }

}
