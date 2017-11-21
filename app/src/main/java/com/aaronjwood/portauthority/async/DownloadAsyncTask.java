package com.aaronjwood.portauthority.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.aaronjwood.portauthority.R;
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

    private ProgressDialog dialog;

    protected Database db;
    protected WeakReference<MainAsyncResponse> delegate;

    Parser parser;

    /**
     * Creates and displays the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        MainAsyncResponse activity = delegate.get();
        if (activity == null) {
            return;
        }

        Context ctx = (Context) activity;
        dialog = new ProgressDialog(ctx, R.style.DialogTheme);
        dialog.setMessage(ctx.getResources().getString(R.string.downloadingData));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.cancel();
                cancel(true);
            }
        });
        dialog.show();
    }

    /**
     * Downloads and parses data based on the service URL and parser.
     *
     * @param service
     * @param parser
     */
    final void doInBackground(String service, Parser parser) {
        BufferedReader in = null;
        HttpsURLConnection connection = null;
        db.beginTransaction();
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

            while ((line = in.readLine()) != null) {
                if (isCancelled()) {
                    return;
                }

                String[] data = parser.parseLine(line);
                if (data == null) {
                    continue;
                }

                if (parser.saveLine(db, data) == -1) {
                    publishProgress("Failed to insert data into the database. Please run this operation again");

                    return;
                }
            }
            db.setTransactionSuccessful();
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

    /**
     * Handles errors.
     *
     * @param progress
     */
    @Override
    protected void onProgressUpdate(String... progress) {
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(new Exception(progress[0]));
        }
    }

    /**
     * Dismisses the dialog.
     *
     * @param result
     */
    @Override
    protected void onPostExecute(Void result) {
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            dialog.dismiss();
        }
    }

}
