package com.aaronjwood.portauthority.db;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aaronjwood.portauthority.async.DownloadOuisAsyncTask;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PortAuthority";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "CREATE TABLE ouis (mac TEXT NOT NULL, vendor TEXT NOT NULL);" +
            "CREATE TABLE ports (name TEXT, port INTEGER, protocol TEXT, description TEXT);";

    private Context context;

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Generate OUI Database")
                .setMessage("Do you want to create the OUI database? " +
                        "This will download the official OUI lists from the IEEE. " +
                        "Note that you won't be able to resolve any MAC vendors without this data. " +
                        "You can always perform this later in the settings.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        db.execSQL(DATABASE_CREATE);
                        new DownloadOuisAsyncTask(context).execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).setIcon(android.R.drawable.ic_dialog_alert).show().setCanceledOnTouchOutside(false);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO implement when upgrades are needed.
    }

}
