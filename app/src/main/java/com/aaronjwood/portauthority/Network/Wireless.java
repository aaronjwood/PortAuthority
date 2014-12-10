package com.aaronjwood.portauthority.Network;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;

public class Wireless {

    private Activity activity;
    private WifiManager wifi;
    private WifiInfo wifiInfo;
    private ConnectivityManager connection;
    private NetworkInfo networkInfo;

    private static final String EXTERNAL_IP_SERVICE = "http://whatismyip.akamai.com/";

    public Wireless(Activity activity) {
        this.activity = activity;
        this.wifi = (WifiManager) this.activity
                .getSystemService(Context.WIFI_SERVICE);
        this.wifiInfo = this.wifi.getConnectionInfo();
        this.connection = (ConnectivityManager) this.activity
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.networkInfo = this.connection
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public String getMacAddress() {
        return this.wifiInfo.getMacAddress();
    }

    public boolean isHidden() {
        return this.wifiInfo.getHiddenSSID();
    }

    public int getSignalStrength() {
        this.wifiInfo = this.wifi.getConnectionInfo();
        return this.wifiInfo.getRssi();
    }

    public String getBSSID() {
        return this.wifiInfo.getBSSID();
    }

    public String getSSID() {
        return this.wifiInfo.getSSID();
    }

    public String getInternalIpAddress() {
        int ip = this.wifiInfo.getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ip & 0xff),
                (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void getExternalIpAddress(TextView view) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(EXTERNAL_IP_SERVICE);
                HttpResponse response;

                try {
                    response = httpclient.execute(httpget);
                }
                catch (ClientProtocolException e) {
                    return null;
                }
                catch (IOException e) {
                    return null;
                }

                String ip;
                HttpEntity entity = response.getEntity();

                try {
                    ip = EntityUtils.toString(entity);
                }
                catch (ParseException e) {
                    return null;
                }
                catch (IOException e) {
                    return null;
                }

                return ip;
            }

            @Override
            protected void onPostExecute(String result) {
                TextView ipAddress = (TextView) activity
                        .findViewById(R.id.externalIpAddress);
                ipAddress.setText(result);
            }

        }.execute();
    }

    public int getLinkSpeed() {
        return this.wifiInfo.getLinkSpeed();
    }

    public boolean isConnected() {
        return this.networkInfo.isConnected();
    }

}
