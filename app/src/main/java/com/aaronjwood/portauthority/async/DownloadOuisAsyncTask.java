package com.aaronjwood.portauthority.async;

import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.parser.Parser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.lang.ref.WeakReference;

public class DownloadOuisAsyncTask extends DownloadAsyncTask {

    private static final String OUI_SERVICE = "https://code.wireshark.org/review/gitweb?p=wireshark.git;a=blob_plain;f=manuf";

    public DownloadOuisAsyncTask(Database db, Parser parser, MainAsyncResponse activity) {
        this.db = db;
        this.delegate = new WeakReference<>(activity);
        this.parser = parser;
    }

    @Override
    protected Void doInBackground(Void... params) {
        doInBackground(OUI_SERVICE, parser);
        return null;
    }

}