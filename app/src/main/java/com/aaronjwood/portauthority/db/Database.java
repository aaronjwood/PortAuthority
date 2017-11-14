package com.aaronjwood.portauthority.db;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.async.DownloadOuisAsyncTask;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PortAuthority";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "CREATE TABLE ouis (mac TEXT NOT NULL, vendor TEXT NOT NULL);" +
            "CREATE TABLE ports (name TEXT, port INTEGER, protocol TEXT, description TEXT);";

    private static final String OUI_TABLE = "ouis";
    private static final String PORT_TABLE = "ports";

    private static final String MAC_FIELD = "mac";
    private static final String VENDOR_FIELD = "vendor";
    private static final String PORT_NAME_FIELD = "name";
    private static final String PORT_FIELD = "port";
    private static final String PROTOCOL_FIELD = "protocol";
    private static final String DESCRIPTION_FIELD = "description";

    private SQLiteDatabase db;
    private Context context;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        final Database instance = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        builder.setTitle("Generate OUI Database")
                .setMessage("Do you want to create the OUI database? " +
                        "This will download the official OUI lists from the IEEE. " +
                        "Note that you won't be able to resolve any MAC vendors without this data. " +
                        "You can always perform this later in the settings.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        db.execSQL(DATABASE_CREATE);
                        new DownloadOuisAsyncTask(instance, context).execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // We don't want to do anything.
            }
        }).setIcon(android.R.drawable.ic_dialog_alert).show().setCanceledOnTouchOutside(false);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO implement when upgrades are needed.
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
