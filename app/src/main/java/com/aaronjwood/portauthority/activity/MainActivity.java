package com.aaronjwood.portauthority.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Discovery;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements MainAsyncResponse {

    private final static int TIMER_INTERVAL = 1500;

    private Wireless wifi;
    private Discovery discovery = new Discovery();

    private Button discoverHosts;
    private ListView hostList;
    private TextView macAddress;
    private TextView internalIp;
    private TextView externalIp;
    private TextView signalStrength;
    private TextView ssid;
    private TextView bssid;
    private ProgressDialog scanProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.hostList = (ListView) findViewById(R.id.hostList);
        this.macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        this.internalIp = (TextView) findViewById(R.id.internalIpAddress);
        this.externalIp = (TextView) findViewById(R.id.externalIpAddress);
        this.signalStrength = (TextView) findViewById(R.id.signalStrength);
        this.discoverHosts = (Button) findViewById(R.id.discoverHosts);
        this.ssid = (TextView) findViewById(R.id.ssid);
        this.bssid = (TextView) findViewById(R.id.bssid);

        this.wifi = new Wireless(this);
        this.wifi.getExternalIpAddress(this);

        final String internalIp = this.wifi.getInternalIpAddress();

        this.macAddress.setText(this.wifi.getMacAddress());
        this.internalIp.setText(internalIp);
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

                scanProgressDialog = new ProgressDialog(MainActivity.this);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning For Hosts");
                scanProgressDialog.setProgressStyle(scanProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(255);
                scanProgressDialog.show();

                discovery.scanHosts(internalIp, MainActivity.this);
            }
        });

        this.hostList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> map = (HashMap) hostList.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, HostActivity.class);
                intent.putExtra("HOST", map.get("First Line"));
                intent.putExtra("MAC", map.get("Second Line"));
                startActivity(intent);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();

        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
        }
        this.scanProgressDialog = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        ListAdapter adapter = this.hostList.getAdapter();
        if(adapter != null) {
            ArrayList<Map<String, String>> adapterData = new ArrayList<>();
            for(int i = 0; i < adapter.getCount(); i++) {
                HashMap<String, String> item = (HashMap) adapter.getItem(i);
                adapterData.add(item);
            }
            savedState.putSerializable("hosts", adapterData);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        ArrayList<Map<String, String>> hosts = (ArrayList) savedState.getSerializable("hosts");
        if(hosts != null) {
            SimpleAdapter newAdapter = new SimpleAdapter(this, hosts, android.R.layout.simple_list_item_2, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
            this.hostList.setAdapter(newAdapter);
        }
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

    @Override
    public void processFinish(ArrayList<Map<String, String>> output) {
        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            SimpleAdapter adapter = new SimpleAdapter(this, output, android.R.layout.simple_list_item_2, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
            ListView hostList = (ListView) this.findViewById(R.id.hostList);
            hostList.setAdapter(adapter);
            this.scanProgressDialog.dismiss();
        }
    }

    @Override
    public void processFinish(int output) {
        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.incrementProgressBy(output);
        }
    }

    @Override
    public void processFinish(String output) {
        this.externalIp.setText(output);
    }
}
