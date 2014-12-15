package com.aaronjwood.portauthority.Network;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host {

    private static final String TAG = "Host";

    private Activity activity;
    private String ip;

    public Host(Activity activity, String ip) {
        this.activity = activity;
        this.ip = ip;
    }

    public String getHostName() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    InetAddress add = InetAddress.getByName(ip);
                    return add.getHostName();
                }
                catch(UnknownHostException e) {
                    Log.e(TAG, e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                TextView hostName = (TextView) activity.findViewById(R.id.hostName);
                hostName.setText(result);
            }

        }.execute();
        return null;
    }

}
