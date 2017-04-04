package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.MainAsyncResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class GetExternalIpAsyncTask extends AsyncTask<Void, Void, String> {

    // IP service is 100% open source https://github.com/aaronjwood/public-ip-api
    private static final String EXTERNAL_IP_SERVICE = "https://public-ip-api.appspot.com/";
    private final WeakReference<MainAsyncResponse> delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when the external IP has been fetched
     */
    public GetExternalIpAsyncTask(MainAsyncResponse delegate) {
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
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(EXTERNAL_IP_SERVICE);

        try {
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return error;
            }

            return EntityUtils.toString(response.getEntity()).trim();
        } catch (ClientProtocolException e) {
            return error;
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
