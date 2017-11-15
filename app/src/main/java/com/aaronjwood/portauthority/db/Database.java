package com.aaronjwood.portauthority.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "PortAuthority";
    private static final int DATABASE_VERSION = 1;
    private static final String OUI_TABLE = "ouis";
    private static final String PORT_TABLE = "ports";
    private static final String MAC_FIELD = "mac";
    private static final String VENDOR_FIELD = "vendor";
    private static final String PORT_NAME_FIELD = "name";
    private static final String PORT_FIELD = "port";
    private static final String PROTOCOL_FIELD = "protocol";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String DATABASE_CREATE = "CREATE TABLE " + OUI_TABLE + " (" + MAC_FIELD + " TEXT NOT NULL, " + VENDOR_FIELD + " TEXT NOT NULL);" +
            "CREATE TABLE " + PORT_TABLE + " (" + PORT_NAME_FIELD + " TEXT, " + PORT_FIELD + " INTEGER, " + PROTOCOL_FIELD + " TEXT, " + DESCRIPTION_FIELD + " TEXT);";

    private static Database singleton;
    private SQLiteDatabase db;

    public static Database getInstance(Context context) {
        if (singleton == null) {
            singleton = new Database(context);
        }

        return singleton;
    }

    private Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO implement when upgrades are needed.
    }

    public Database beginTransaction() {
        if (!db.inTransaction()) {
            db.beginTransaction();
        }

        return this;
    }

    public Database endTransaction() {
        if (db.inTransaction()) {
            db.endTransaction();
        }

        return this;
    }

    public Database setTransactionSuccessful() {
        if (db.inTransaction()) {
            db.setTransactionSuccessful();
        }

        return this;
    }

    public long insertOui(String mac, String vendor) {
        ContentValues values = new ContentValues();
        values.put(MAC_FIELD, mac);
        values.put(VENDOR_FIELD, vendor);

        return db.insert(OUI_TABLE, null, values);
    }

    public Database clearOuis() {
        db.execSQL("DELETE FROM " + OUI_TABLE);
        db.execSQL("VACUUM");
        return this;
    }

    public String selectVendor(String mac) {
        Cursor cursor = db.rawQuery("SELECT " + VENDOR_FIELD + " FROM " + OUI_TABLE + " WHERE " + MAC_FIELD + " LIKE ?", new String[]{mac});
        String vendor;
        if (cursor.moveToFirst()) {
            vendor = cursor.getString(cursor.getColumnIndex("vendor"));
        } else {
            vendor = "Vendor not in database";
        }

        cursor.close();

        return vendor;
    }

    public String selectPortName(int port) {
        Cursor cursor = db.rawQuery("SELECT name FROM ports WHERE port = ?", new String[]{Integer.toString(port)});
        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex("name"));
        }

        cursor.close();

        return name;
    }

}
