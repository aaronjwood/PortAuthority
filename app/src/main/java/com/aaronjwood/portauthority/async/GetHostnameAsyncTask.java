package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GetHostnameAsyncTask extends AsyncTask<String, Void, String> {
    private HostAsyncResponse delegate;

    /**
     * Constructor to set the delegate
     *
     * @param delegate Called when the hostname has been fetched
     */
    public GetHostnameAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    /**
     * Fetches the hostname
     *
     * @param params IP address
     * @return Hostname
     */
    @Override
    protected String doInBackground(String... params) {
        String ip = params[0];
        try {
            InetAddress add = InetAddress.getByName(ip);
            return add.getCanonicalHostName();
        } catch (UnknownHostException ignored) {
        }
        return null;
    }

    /**
     * Calls the delegate when the hostname has been fetched
     *
     * @param result Hostname
     */
    @Override
    protected void onPostExecute(String result) {
        delegate.processFinish(result);
    }
}
