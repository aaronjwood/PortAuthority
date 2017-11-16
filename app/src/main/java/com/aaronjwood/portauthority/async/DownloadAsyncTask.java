package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.parser.Parser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public abstract class DownloadAsyncTask extends AsyncTask<Void, String, Void> {

    protected WeakReference<MainAsyncResponse> delegate;
    protected Database db;
    Parser parser;

    @Override
    protected void onPreExecute() {
        final MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(this);
        }
    }

    void doInBackground(String service, Parser parser) {
        BufferedReader in = null;
        HttpsURLConnection connection = null;
        db.clearOuis();
        try {
            URL url = new URL(service);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                publishProgress(connection.getResponseCode() + " " + connection.getResponseMessage());

                return;
            }

            in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
            String line;
            db.beginTransaction();
            while ((line = in.readLine()) != null) {
                if (isCancelled()) {
                    return;
                }

                String[] data = parser.parseLine(line);
                if (data == null) {
                    continue;
                }

                if (parser.saveLine(db, data) == -1) {
                    publishProgress("Failed to insert MAC " + data[0] + " into the database. " +
                            "Please run this operation again");

                    return;
                }
            }
            db.setTransactionSuccessful().endTransaction();
        } catch (Exception e) {
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
