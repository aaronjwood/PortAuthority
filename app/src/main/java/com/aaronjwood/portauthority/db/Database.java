package com.aaronjwood.portauthority.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "PortAuthority";
    private static final int DATABASE_VERSION = 2;
    private static final String OUI_TABLE = "ouis";
    private static final String PORT_TABLE = "ports";
    private static final String MAC_FIELD = "mac";
    private static final String VENDOR_FIELD = "vendor";
    private static final String PORT_FIELD = "port";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String CREATE_OUI_TABLE = "CREATE TABLE " + OUI_TABLE + " (" + MAC_FIELD + " TEXT NOT NULL, " + VENDOR_FIELD + " TEXT NOT NULL);";
    private static final String CREATE_PORT_TABLE = "CREATE TABLE " + PORT_TABLE + " (" + PORT_FIELD + " INTEGER NOT NULL, " + DESCRIPTION_FIELD + " TEXT);";
    private static final String CREATE_PORT_INDEX = "CREATE INDEX IF NOT EXISTS idx_ports_port ON " + PORT_TABLE + " (" + PORT_FIELD + ");";
    private static final String CREATE_MAC_INDEX = "CREATE INDEX IF NOT EXISTS idx_ouis_mac ON " + OUI_TABLE + " (" + MAC_FIELD + ");";

    private static Database singleton;
    private SQLiteDatabase db;

    /**
     * Returns the single instance of this class or creates one if it doesn't already exist.
     *
     * @param context
     * @return
     */
    public static Database getInstance(Context context) {
        if (singleton == null) {
            singleton = new Database(context);
        }

        return singleton;
    }

    /**
     * Sets up the database and returns the writable handle to it.
     *
     * @param context
     */
    private Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
    }

    /**
     * Starts a transaction that allows for multiple readers and one writer.
     *
     * @return
     */
    public Database beginTransaction() {
        db.beginTransactionNonExclusive();
        return this;
    }

    /**
     * Finishes the transaction.
     *
     * @return
     */
    public Database endTransaction() {
        db.endTransaction();
        return this;
    }

    /**
     * Marks the transaction as successful and commits the transaction.
     *
     * @return
     */
    public Database setTransactionSuccessful() {
        db.setTransactionSuccessful();
        return this;
    }

    /**
     * Called when the database doesn't exist and needs its schema created.
     *
     * @param db
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(CREATE_OUI_TABLE);
        db.execSQL(CREATE_PORT_TABLE);
        db.execSQL(CREATE_PORT_INDEX);
        db.execSQL(CREATE_MAC_INDEX);
    }

    /**
     * Handles upgrades between database versions.
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {

            // Indexes weren't initially created on the first iteration of the schema.
            case 1:
                db.execSQL(CREATE_PORT_INDEX);
                db.execSQL(CREATE_MAC_INDEX);
        }
    }

    /**
     * Inserts a new OUI entry containing a MAC address and its associated vendor.
     *
     * @param mac
     * @param vendor
     * @return
     */
    public long insertOui(String mac, String vendor) {
        ContentValues values = new ContentValues();
        values.put(MAC_FIELD, mac);
        values.put(VENDOR_FIELD, vendor);

        return db.insert(OUI_TABLE, null, values);
    }

    /**
     * Inserts a new port containing the port number and its associated description.
     *
     * @param port
     * @param description
     * @return
     */
    public long insertPort(String port, String description) {
        ContentValues values = new ContentValues();
        values.put(PORT_FIELD, port);
        values.put(DESCRIPTION_FIELD, description);

        return db.insert(PORT_TABLE, null, values);
    }

    /**
     * Wipes out all of the OUIs that are currently in the database.
     *
     * @return
     */
    public Database clearOuis() {
        db.execSQL("DELETE FROM " + OUI_TABLE);
        db.execSQL("VACUUM");
        return this;
    }

    /**
     * Wipes out all of the ports that are currently in the database.
     *
     * @return
     */
    public Database clearPorts() {
        db.execSQL("DELETE FROM " + PORT_TABLE);
        db.execSQL("VACUUM");
        return this;
    }

    /**
     * Searches for a vendor based on the provided MAC address.
     *
     * @param mac
     * @return
     */
    public String selectVendor(String mac) {
        Cursor cursor = db.rawQuery("SELECT " + VENDOR_FIELD + " FROM " + OUI_TABLE + " WHERE " + MAC_FIELD + " = ?", new String[]{mac});
        String vendor;
        if (cursor.moveToFirst()) {
            vendor = cursor.getString(cursor.getColumnIndex("vendor"));
        } else {
            vendor = "Vendor not in database";
        }

        cursor.close();

        return vendor;
    }

    /**
     * Searches for a port description based on the provided port.
     *
     * @param port
     * @return
     */
    public String selectPortDescription(String port) {
        Cursor cursor = db.rawQuery("SELECT " + DESCRIPTION_FIELD + " FROM " + PORT_TABLE + " WHERE " + PORT_FIELD + " = ?", new String[]{port});
        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(DESCRIPTION_FIELD));
        }

        cursor.close();

        return name;
    }

}
