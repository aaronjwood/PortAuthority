package com.aaronjwood.portauthority.Network;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;

public class Discovery extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "Discovery";

    private Activity activity;
    private String ip;

    public Discovery(Activity activity, String ip) {
        this.activity = activity;
        this.ip = ip;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String parts[] = this.ip.split("\\.");
        for(int i = 1; i <= 255; i++) {
            String newIp = parts[0] + "." + parts[1] + "." + parts[2] + "." + i;
            InetAddress address;
            try {
                address = InetAddress.getByName(newIp);
                address.isReachable(1);
            }
            catch(UnknownHostException e) {
                Log.e(TAG, e.getMessage());
            }
            catch(IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return null;
    }

    protected void onPostExecute(Void result) {
        Toast.makeText(this.activity.getApplicationContext(), "Host discovery finished!", Toast.LENGTH_SHORT).show();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            ListView hostList = (ListView) this.activity.findViewById(R.id.hostList);
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) hostList.getAdapter();

            while((line = reader.readLine()) != null) {
                String[] l = line.split("\\s+");
                String flag = l[2];
                String macAddress = l[3];
                if(!flag.equals("0x0") && !macAddress.equals("00:00:00:00:00:00")) {
                    adapter.add(l[0]);
                }
            }
            adapter.sort(new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            });
            adapter.notifyDataSetChanged();
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
