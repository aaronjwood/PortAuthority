package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DownloadOuisAsyncTask extends AsyncTask<Void, String, Void> {

    private static final String OUI_SERVICE = "https://code.wireshark.org/review/gitweb?p=wireshark.git;a=blob_plain;f=manuf";
    private WeakReference<MainAsyncResponse> delegate;
    private Database db;

    public DownloadOuisAsyncTask(Database db, MainAsyncResponse activity) {
        this.db = db;
        this.delegate = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
        final MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(this);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        BufferedReader in = null;
        HttpsURLConnection connection = null;
        db.clearOuis();
        try {
            URL url = new URL(OUI_SERVICE);
            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                publishProgress(connection.getResponseCode() + " " + connection.getResponseMessage());

                return null;
            }

            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            in.readLine(); // Skip headers.
            String line;
            db.beginTransaction();
            while ((line = in.readLine()) != null) {
                if (isCancelled()) {
                    return null;
                }

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip comments and empty lines.
                }

                String[] data = line.split("\\t");
                String mac = data[0].toLowerCase();
                String vendor;
                if (data.length == 3) {
                    vendor = data[2];
                } else {
                    vendor = data[1];
                }

                if (db.insertOui(mac, vendor) == -1) {
                    publishProgress("Failed to insert MAC " + mac + " into the database. " +
                            "Please run this operation again");

                    return null;
                }
            }
            db.setTransactionSuccessful().endTransaction();
        } catch (IOException e) {
            publishProgress(e.toString());
        } finally {
            db.endTransaction();
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(new Exception(progress[0]));
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(this, result);
        }
    }

}
