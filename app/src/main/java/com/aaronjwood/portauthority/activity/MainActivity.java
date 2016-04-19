package com.aaronjwood.portauthority.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements MainAsyncResponse {

    private final static int TIMER_INTERVAL = 1500;

    private Wireless wifi;
    private Discovery discovery = new Discovery();
    private ListView hostList;
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

        this.setupDrawer();

        this.hostList = (ListView) findViewById(R.id.hostList);
        this.internalIp = (TextView) findViewById(R.id.internalIpAddress);
        this.externalIp = (TextView) findViewById(R.id.externalIpAddress);
        this.signalStrength = (TextView) findViewById(R.id.signalStrength);

        this.ssid = (TextView) findViewById(R.id.ssid);
        this.bssid = (TextView) findViewById(R.id.bssid);

        this.wifi = new Wireless(this);

        TextView macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        macAddress.setText(this.wifi.getMacAddress());

        this.setupReceivers();
        this.setupHostDiscovery();
    }

    /**
     * Sets up event handlers and functionality for host discovery
     */
    private void setupHostDiscovery() {
        Button discoverHosts = (Button) findViewById(R.id.discoverHosts);

        discoverHosts.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler to perform host discovery
             * @param v
             */
            @Override
            public void onClick(View v) {
                if (!wifi.isConnectedWifi()) {
                    Toast.makeText(getApplicationContext(), "You're not connected to a WiFi network!", Toast.LENGTH_SHORT).show();
                    return;
                }

                scanProgressDialog = new ProgressDialog(MainActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle("Scanning For Hosts");
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
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
                Intent intent = new Intent(MainActivity.this, LanHostActivity.class);
                String firstLine = map.get("First Line");
                String secondLine = map.get("Second Line");
                String ip = secondLine.substring(0, secondLine.indexOf("[") - 1);
                String macAddress = secondLine.substring(secondLine.indexOf("[") + 1, secondLine.indexOf("]"));

                intent.putExtra("HOSTNAME", firstLine);
                intent.putExtra("IP", ip);
                intent.putExtra("MAC", macAddress);
                startActivity(intent);
            }
        });
    }

    /**
     * Sets up and registers receivers
     */
    private void setupReceivers() {
        this.receiver = new BroadcastReceiver() {

            /**
             * Detect if a network connection has been lost or established
             * @param context
             * @param intent
             */
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    if (info.isConnected()) {
                        getNetworkInfo();
                    } else {
                        mHandler.removeCallbacksAndMessages(null);
                        MainActivity.this.internalIp.setText(R.string.noWifi);
                        wifi.getExternalIpAddress(MainActivity.this);
                        signalStrength.setText(R.string.noWifi);
                        ssid.setText(R.string.noWifi);
                        bssid.setText(R.string.noWifi);
                    }
                }
            }
        };

        this.intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, this.intentFilter);
    }

    /**
     * Sets up event handlers and items for the left drawer
     */
    private void setupDrawer() {
        final DrawerLayout leftDrawer = (DrawerLayout) findViewById(R.id.mainLeftDrawer);
        findViewById(R.id.mainLeftDrawerIcon).setOnClickListener(new View.OnClickListener() {

            /**
             * Open the left drawer when the users taps on the icon
             * @param v
             */
            @Override
            public void onClick(View v) {
                leftDrawer.openDrawer(Gravity.LEFT);
            }
        });

        //Fill the left drawer
        final ListView leftDrawerList = (ListView) findViewById(R.id.mainLeftDrawerList);
        ArrayList<String> items = new ArrayList<>();
        items.add("Scan External Host");
        leftDrawerList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));

        leftDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler for the left side navigation drawer items
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    Intent intent = new Intent(MainActivity.this, WanHostActivity.class);
                    startActivity(intent);
                    leftDrawer.closeDrawer(parent);
                }
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

        if (this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
            this.scanProgressDialog.dismiss();
        }
        this.scanProgressDialog = null;
    }

    /**
     * Activity destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (this.receiver != null) {
            unregisterReceiver(this.receiver);
        }
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
        if (adapter != null) {
            ArrayList<Map<String, String>> adapterData = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
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
        if (hosts != null) {
            SimpleAdapter newAdapter = new SimpleAdapter(this, hosts, R.layout.host_list_item, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
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
    public void processFinish(final List<Map<String, String>> output) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                synchronized (output) {
                    Collections.sort(output, new Comparator<Map<String, String>>() {

                        @Override
                        public int compare(Map<String, String> lhs, Map<String, String> rhs) {
                            int left = Integer.parseInt(lhs.get("Second Line").substring(lhs.get("Second Line").lastIndexOf(".") + 1, lhs.get("Second Line").indexOf("[") - 1));
                            int right = Integer.parseInt(rhs.get("Second Line").substring(rhs.get("Second Line").lastIndexOf(".") + 1, rhs.get("Second Line").indexOf("[") - 1));

                            return left - right;
                        }
                    });

                    SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), output, R.layout.host_list_item, new String[]{"First Line", "Second Line"}, new int[]{android.R.id.text1, android.R.id.text2});
                    ListView hostList = (ListView) findViewById(R.id.hostList);
                    hostList.setAdapter(adapter);
                }

                if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
                    scanProgressDialog.dismiss();
                }
            }
        });
    }

    /**
     * Delegate to update the progress of the host discovery scan
     *
     * @param output The amount of progress to increment by
     */
    @Override
    public void processFinish(int output) {
        if (this.scanProgressDialog != null && this.scanProgressDialog.isShowing()) {
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
