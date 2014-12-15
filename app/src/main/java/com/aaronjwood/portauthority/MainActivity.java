package com.aaronjwood.portauthority;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.Network.Discovery;
import com.aaronjwood.portauthority.Network.Wireless;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private final static int TIMER_INTERVAL = 1500;

    private Wireless wifi;

    private Button discoverHosts;
    private ListView hostList;
    private TextView macAddress;
    private TextView ipAddress;
    private TextView signalStrength;
    private TextView ssid;
    private TextView bssid;

    private ArrayList<String> hosts = new ArrayList<>();
    private ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.hostList = (ListView) findViewById(R.id.hostList);
        this.macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        this.ipAddress = (TextView) findViewById(R.id.internalIpAddress);
        this.signalStrength = (TextView) findViewById(R.id.signalStrength);
        this.discoverHosts = (Button) findViewById(R.id.discoverHosts);
        this.ssid = (TextView) findViewById(R.id.ssid);
        this.bssid = (TextView) findViewById(R.id.bssid);

        if(savedInstanceState != null) {
            this.hosts = savedInstanceState.getStringArrayList("hosts");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.hosts);
            this.hostList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }

        this.wifi = new Wireless(this);
        this.wifi.getExternalIpAddress();

        final String internalIp = this.wifi.getInternalIpAddress();

        this.macAddress.setText(this.wifi.getMacAddress());
        this.ipAddress.setText(internalIp);
        this.ssid.setText(this.wifi.getSSID());
        this.bssid.setText(this.wifi.getBSSID());

        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                signalStrength.setText(String.valueOf(wifi.getSignalStrength()) + " dBm");
                mHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);

        this.discoverHosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!wifi.isConnected()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(getApplicationContext(), "Finding hosts on your network...", Toast.LENGTH_SHORT).show();

                hosts.clear();

                adapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_list_item_1, hosts);
                hostList.setAdapter(adapter);

                Discovery discovery = new Discovery((Activity) v.getContext(), internalIp);
                discovery.execute();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putStringArrayList("hosts", this.hosts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
