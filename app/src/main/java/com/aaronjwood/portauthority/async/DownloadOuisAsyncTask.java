package com.aaronjwood.portauthority.async;

import com.aaronjwood.portauthority.activity.MainActivity;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.parser.Parser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.lang.ref.WeakReference;

public class DownloadOuisAsyncTask extends DownloadAsyncTask {

    // The official source on gitlab.com doesn't provide a content length header which ruins our ability to report progress!
    // Use the mirror from github.com instead.
    private static final String SERVICE = "https://raw.githubusercontent.com/wireshark/wireshark/master/manuf";

    /**
     * Creates a new asynchronous task to handle downloading OUI data.
     *
     * @param database
     * @param parser
     * @param activity
     */
    public DownloadOuisAsyncTask(Database database, Parser parser, MainAsyncResponse activity) {
        db = database;
        delegate = new WeakReference<>(activity);
        this.parser = parser;
    }

    /**
     * Sets up and displays the dialog.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * Downloads new OUI data.
     *
     * @param params
     * @return
     */
    @Override
    protected Void doInBackground(Void... params) {
        db.clearOuis();
        doInBackground(SERVICE, parser);
        return null;
    }

    /**
     * Updates the UI with the MAC address from the newly fetched data.
     *
     * @param result
     */
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            ((MainActivity) activity).setupMac();
        }
    }

}