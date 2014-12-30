package com.aaronjwood.portauthority.async;

import android.os.AsyncTask;
import android.util.Log;

import com.aaronjwood.portauthority.response.HostAsyncResponse;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GetHostnameAsyncTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "GetHostnameAsyncTask";
    private HostAsyncResponse delegate;

    public GetHostnameAsyncTask(HostAsyncResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(String... params) {
        String ip = params[0];
        try {
            InetAddress add = InetAddress.getByName(ip);
            return add.getHostName();
        }
        catch(UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        delegate.processFinish(result);
    }
}
