package com.aaronjwood.portauthority.async;

import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.parser.Parser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.lang.ref.WeakReference;

public class DownloadPortDataAsyncTask extends DownloadAsyncTask {

    private static final String SERVICE = "https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.csv";

    public DownloadPortDataAsyncTask(Database database, Parser parser, MainAsyncResponse activity) {
        db = database;
        delegate = new WeakReference<>(activity);
        this.parser = parser;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        db.clearPorts();
        doInBackground(SERVICE, parser);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }

}
