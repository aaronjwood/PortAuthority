package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WanIpAsyncTask extends AsyncTask<Void, Void, String> {

    // IP service is 100% open source https://github.com/aaronjwood/public-ip-api
    private static final String EXTERNAL_IP_SERVICE = "https://public-ip-api.appspot.com/";
    private final WeakReference<MainAsyncResponse> delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when the external IP has been fetched
     */
    public WanIpAsyncTask(MainAsyncResponse delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    /**
     * Fetch the external IP address
     *
     * @param params
     * @return External IP address
     */
    @Override
    protected String doInBackground(Void... params) {
        String error = "Couldn't get your external IP";
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url(EXTERNAL_IP_SERVICE).build();

        try {
            Response response = httpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                body.close();
                return error;
            }

            return response.body().string().trim(); // string() closes the resource automatically.
        } catch (IOException e) {
            return error;
        }
    }

    /**
     * Calls the delegate when the external IP has been fetched
     *
     * @param result External IP address
     */
    @Override
    protected void onPostExecute(String result) {
        MainAsyncResponse activity = delegate.get();
        if (activity != null) {
            activity.processFinish(result);
        }
    }
}
