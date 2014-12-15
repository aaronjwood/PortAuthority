package com.aaronjwood.portauthority.Network;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    public void getMacAddress() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            while((line = reader.readLine()) != null) {
                String[] l = line.split("\\s+");

                String ip = l[0];
                String macAddress = l[3];

                if(ip.equals(this.ip)) {
                    TextView hostMac = (TextView) activity.findViewById(R.id.hostMac);
                    hostMac.setText(macAddress);
                    return;
                }
            }
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
