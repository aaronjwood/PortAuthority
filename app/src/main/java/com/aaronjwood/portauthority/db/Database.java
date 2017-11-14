package com.aaronjwood.portauthority.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Database {

    private static final String OUI_TABLE = "ouis";
    private static final String PORT_TABLE = "ports";

    private static final String MAC_FIELD = "mac";
    private static final String VENDOR_FIELD = "vendor";
    private static final String PORT_NAME_FIELD = "name";
    private static final String PORT_FIELD = "port";
    private static final String PROTOCOL_FIELD = "protocol";
    private static final String DESCRIPTION_FIELD = "description";

    private SQLiteDatabase db;

    public Database(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    public long insertOui(String mac, String vendor) {
        ContentValues values = new ContentValues();
        values.put(MAC_FIELD, mac);
        values.put(VENDOR_FIELD, vendor);

        return db.insert(OUI_TABLE, null, values);
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
