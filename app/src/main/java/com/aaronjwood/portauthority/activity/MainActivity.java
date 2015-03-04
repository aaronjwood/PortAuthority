package com.aaronjwood.portauthority.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
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
    private Handler mHandler = new Handler();
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter = new IntentFilter();

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
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

        this.macAddress.setText(this.wifi.getMacAddress());

        this.receiver = new BroadcastReceiver() {

            /**
             * Detect if a network connection has been lost or established
             * @param context
             * @param intent
             */
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = intent.getParcelableExtra(wifi.getWifiManager().EXTRA_NETWORK_INFO);
                if(info != null) {
                    if(info.isConnected()) {
                        getNetworkInfo();
                    }
                    else {
                        mHandler.removeCallbacksAndMessages(null);
                        MainActivity.this.internalIp.setText("No WiFi connection");
                        externalIp.setText("No WiFi connection");
                        signalStrength.setText("No WiFi connection");
                        ssid.setText("No WiFi connection");
                        bssid.setText("No WiFi connection");
                    }
                }
            }
        };

        this.intentFilter.addAction(this.wifi.getWifiManager().NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, this.intentFilter);

        this.discoverHosts.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler to perform host discovery
             * @param v
             */
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

                discovery.scanHosts(wifi.getInternalIpAddress(), MainActivity.this);
            }
        });

        this.hostList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler to open the host activity for a specific host found on the network
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> map = (HashMap) hostList.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, HostActivity.class);
                String firstLine = map.get("First Line");
                String secondLine = map.get("Second Line");
                String macAddress = map.get("Second Line").substring(secondLine.indexOf("[") + 1, secondLine.indexOf("]"));

                intent.putExtra("HOST", firstLine);
                intent.putExtra("MAC", macAddress);
                startActivity(intent);
            }
        });

    }

    /**
     * Gets network information about the device and updates various UI elements
     */
    private void getNetworkInfo() {
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                signalStrength.setText(String.valueOf(wifi.getSignalStrength()) + " dBm");
                mHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);
        this.internalIp.setText(this.wifi.getInternalIpAddress());
        this.wifi.getExternalIpAddress(this);
        this.ssid.setText(this.wifi.getSSID());
        this.bssid.setText(this.wifi.getBSSID());
    }

    /**
     * Activity paused
     */
    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(this.receiver);

        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
        }
        this.scanProgressDialog = null;
    }

    /**
     * Activity restarted
     */
    @Override
    public void onRestart() {
        super.onRestart();

        registerReceiver(this.receiver, this.intentFilter);
    }

    /**
     * Save the state of an activity
     *
     * @param savedState Data to save
     */
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

    /**
     * Activity state restored
     *
     * @param savedState Saved data from the saved state
     */
    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        ArrayList<Map<String, String>> hosts = (ArrayList) savedState.getSerializable("hosts");
        if(hosts != null) {
            SimpleAdapter newAdapter = new SimpleAdapter(this, hosts, android.R.layout.simple_list_item_2, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
            this.hostList.setAdapter(newAdapter);
        }
    }

    /**
     * Delegate to update the host list and dismiss the progress dialog
     * Gets called when host discovery has finished
     *
     * @param output The list of hosts to bind to the list view
     */
    @Override
    public void processFinish(ArrayList<Map<String, String>> output) {
        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            SimpleAdapter adapter = new SimpleAdapter(this, output, android.R.layout.simple_list_item_2, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
            ListView hostList = (ListView) this.findViewById(R.id.hostList);
            hostList.setAdapter(adapter);
            this.scanProgressDialog.dismiss();
        }
    }

    /**
     * Delegate to update the progress of the host discovery scan
     *
     * @param output The amount of progress to increment by
     */
    @Override
    public void processFinish(int output) {
        if(this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.incrementProgressBy(output);
        }
    }

    /**
     * Delegate to handle setting the external IP in the UI
     *
     * @param output External IP
     */
    @Override
    public void processFinish(String output) {
        this.externalIp.setText(output);
    }
}
