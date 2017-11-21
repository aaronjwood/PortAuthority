package com.aaronjwood.portauthority.async;

import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.parser.Parser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.lang.ref.WeakReference;

public class DownloadPortDataAsyncTask extends DownloadAsyncTask {

    private static final String SERVICE = "https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.csv";

    /**
     * Creates a new asynchronous task that takes care of downloading port data.
     *
     * @param database
     * @param parser
     * @param activity
     */
    public DownloadPortDataAsyncTask(Database database, Parser parser, MainAsyncResponse activity) {
        db = database;
        delegate = new WeakReference<>(activity);
        this.parser = parser;
    }

    /**
     * Displays the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * Downloads new port data.
     *
     * @param params
     * @return
     */
    @Override
    protected Void doInBackground(Void... params) {
        db.clearPorts();
        doInBackground(SERVICE, parser);
        return null;
    }

    /**
     * Dismisses the dialog.
     *
     * @param result
     */
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }

}
