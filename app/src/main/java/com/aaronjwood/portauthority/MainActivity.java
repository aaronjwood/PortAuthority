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
    private ArrayList<String> hosts = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView hostList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState != null) {
            this.hosts = savedInstanceState.getStringArrayList("hosts");
            this.adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, this.hosts);
            this.hostList = (ListView) findViewById(R.id.hostList);
            this.hostList.setAdapter(this.adapter);
            this.adapter.notifyDataSetChanged();
        }

        this.wifi = new Wireless(this);
        this.wifi.getExternalIpAddress();

        final String internalIp = this.wifi.getInternalIpAddress();

        TextView macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        macAddress.setText(this.wifi.getMacAddress());

        TextView ipAddress = (TextView) findViewById(R.id.internalIpAddress);
        ipAddress.setText(internalIp);

        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                TextView signalStrength = (TextView) findViewById(R.id.signalStrength);
                signalStrength.setText(String.valueOf(wifi.getSignalStrength()) + " dBm");
                mHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);

        this.discoverHosts = (Button) findViewById(R.id.discoverHosts);

        discoverHosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Finding hosts on your network...", Toast.LENGTH_SHORT).show();

                hosts.clear();

                adapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_list_item_1, hosts);
                hostList = (ListView) findViewById(R.id.hostList);
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
