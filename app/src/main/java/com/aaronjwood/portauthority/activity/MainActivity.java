package com.aaronjwood.portauthority.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aaronjwood.portauthority.BuildConfig;
import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.adapters.HostAdapter;
import com.aaronjwood.portauthority.network.Discovery;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.utils.UserPreference;
import com.squareup.leakcanary.LeakCanary;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MainActivity extends AppCompatActivity implements MainAsyncResponse {

    private final static int TIMER_INTERVAL = 1500;

    private Wireless wifi;
    private ListView hostList;
    private TextView internalIp;
    private TextView externalIp;
    private String cachedWanIp;
    private TextView signalStrength;
    private TextView ssid;
    private TextView bssid;
    private Button discoverHostsBtn;
    private String discoverHostsStr; // Cache this so it's not looked up every time a host is found.
    private ProgressDialog scanProgressDialog;
    private Handler signalHandler = new Handler();
    private Handler scanHandler;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter = new IntentFilter();
    private HostAdapter hostAdapter;
    private List<Host> hosts = Collections.synchronizedList(new ArrayList<Host>());
    private boolean sortAscending;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            LeakCanary.install(getApplication());
        }

        setContentView(R.layout.activity_main);

        internalIp = (TextView) findViewById(R.id.internalIpAddress);
        externalIp = (TextView) findViewById(R.id.externalIpAddress);
        signalStrength = (TextView) findViewById(R.id.signalStrength);
        ssid = (TextView) findViewById(R.id.ssid);
        bssid = (TextView) findViewById(R.id.bssid);
        hostList = (ListView) findViewById(R.id.hostList);
        discoverHostsBtn = (Button) findViewById(R.id.discoverHosts);
        discoverHostsStr = getResources().getString(R.string.hostDiscovery);

        wifi = new Wireless(getApplicationContext());
        scanHandler = new Handler(Looper.getMainLooper());

        setupHostsAdapter();
        setupDrawer();
        setupReceivers();
        setupMac();
        setupHostDiscovery();
    }

    /**
     * Sets up animations for the activity
     */
    private void setAnimations() {
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(MainActivity.this, R.anim.layout_slide_in_bottom);
        hostList.setLayoutAnimation(animation);
    }

    /**
     * Sets up the adapter to handle discovered hosts
     */
    private void setupHostsAdapter() {
        setAnimations();
        hostAdapter = new HostAdapter(this, hosts);

        hostList.setAdapter(hostAdapter);
        if (!hosts.isEmpty()) {
            discoverHostsBtn.setText(discoverHostsStr + " (" + hosts.size() + ")");
        }
    }

    /**
     * Sets up the device's MAC address and vendor
     */
    private void setupMac() {
        //Set MAC address
        TextView macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        String mac = wifi.getMacAddress();
        macAddress.setText(mac);

        //Set the device's vendor
        if (mac != null) {
            TextView macVendor = (TextView) findViewById(R.id.deviceMacVendor);
            macVendor.setText(Host.getMacVendor(mac.replace(":", "").substring(0, 6), this));
        }
    }

    /**
     * Sets up event handlers and functionality for host discovery
     */
    private void setupHostDiscovery() {
        discoverHostsBtn.setOnClickListener(new View.OnClickListener() {

            /**
             * Click handler to perform host discovery
             * @param v
             */
            @Override
            public void onClick(View v) {
                if (!wifi.isConnectedWifi()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notConnectedWifi), Toast.LENGTH_SHORT).show();
                    return;
                }

                setAnimations();

                hosts.clear();
                discoverHostsBtn.setText(discoverHostsStr);
                hostAdapter.notifyDataSetChanged();

                scanProgressDialog = new ProgressDialog(MainActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle(getResources().getString(R.string.hostScan));
                scanProgressDialog.setMessage(String.format(getResources().getString(R.string.subnetHosts), wifi.getNumberOfHostsInWifiSubnet()));
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(wifi.getNumberOfHostsInWifiSubnet());
                scanProgressDialog.show();

                Integer ip = wifi.getInternalWifiIpAddress(Integer.class);
                if (ip != null) {
                    Discovery.scanHosts(ip, wifi.getInternalWifiSubnet(), UserPreference.getHostSocketTimeout(getApplicationContext()), MainActivity.this);
                }
            }
        });

        hostList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler to open the host activity for a specific host found on the network
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Host host = (Host) hostList.getItemAtPosition(position);
                if (host == null) {
                    return;
                }
                Intent intent = new Intent(MainActivity.this, LanHostActivity.class);
                intent.putExtra("HOST", host);
                startActivity(intent);
            }
        });

        registerForContextMenu(hostList);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.hostList) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.host_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sortHostname:
                if (sortAscending) {
                    hostAdapter.sort(new Comparator<Host>() {
                        @Override
                        public int compare(Host lhs, Host rhs) {
                            return rhs.getHostname().toLowerCase().compareTo(lhs.getHostname().toLowerCase());
                        }
                    });
                } else {
                    hostAdapter.sort(new Comparator<Host>() {
                        @Override
                        public int compare(Host lhs, Host rhs) {
                            return lhs.getHostname().toLowerCase().compareTo(rhs.getHostname().toLowerCase());
                        }
                    });
                }

                sortAscending = !sortAscending;
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Sets up and registers receivers
     */
    private void setupReceivers() {
        receiver = new BroadcastReceiver() {

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
                        signalHandler.removeCallbacksAndMessages(null);
                        internalIp.setText(Wireless.getInternalMobileIpAddress());
                        getExternalIp();
                        signalStrength.setText(R.string.noWifi);
                        ssid.setText(R.string.noWifi);
                        bssid.setText(R.string.noWifi);
                    }
                }
            }
        };

        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * Sets up event handlers and items for the left drawer
     */
    private void setupDrawer() {
        final DrawerLayout leftDrawer = (DrawerLayout) findViewById(R.id.leftDrawer);
        final RelativeLayout leftDrawerLayout = (RelativeLayout) findViewById(R.id.leftDrawerLayout);

        ImageView drawerIcon = (ImageView) findViewById(R.id.leftDrawerIcon);
        drawerIcon.setOnClickListener(new View.OnClickListener() {

            /**
             * Open the left drawer when the users taps on the icon
             * @param v
             */
            @Override
            public void onClick(View v) {
                leftDrawer.openDrawer(GravityCompat.START);
            }
        });

        ListView upperList = (ListView) findViewById(R.id.upperLeftDrawerList);
        ListView lowerList = (ListView) findViewById(R.id.lowerLeftDrawerList);

        upperList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler for the left side navigation drawer items
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(MainActivity.this, WanHostActivity.class));
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this, DnsActivity.class));
                        break;
                }
                leftDrawer.closeDrawer(leftDrawerLayout);
            }
        });

        lowerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Click handler for the left side navigation drawer items
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                        break;
                }
                leftDrawer.closeDrawer(leftDrawerLayout);
            }
        });
    }

    /**
     * Gets network information about the device and updates various UI elements
     */
    private void getNetworkInfo() {
        final int linkSpeed = wifi.getLinkSpeed();
        signalHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                signalStrength.setText(String.format(getResources().getString(R.string.signalLink), wifi.getSignalStrength(), linkSpeed));
                signalHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);
        getInternalIp();
        getExternalIp();
        ssid.setText(wifi.getSSID());
        bssid.setText(wifi.getBSSID());
    }

    /**
     * Wrapper method for getting the internal wireless IP address.
     * This gets the netmask, counts the bits set (subnet size),
     * then prints it along side the IP.
     */
    private void getInternalIp() {
        int netmask = wifi.getInternalWifiSubnet();
        String InternalIpWithSubnet = wifi.getInternalWifiIpAddress(String.class) + "/" + Integer.toString(netmask);
        internalIp.setText(InternalIpWithSubnet);
    }

    /**
     * Wrapper for getting the external IP address
     * We can control whether or not to do this based on the user's preference
     * If the user doesn't want this then hide the appropriate views
     */
    private void getExternalIp() {
        TextView label = (TextView) findViewById(R.id.externalIpAddressLabel);
        TextView ip = (TextView) findViewById(R.id.externalIpAddress);

        if (UserPreference.getFetchExternalIp(this)) {
            label.setVisibility(View.VISIBLE);
            ip.setVisibility(View.VISIBLE);

            if (cachedWanIp == null) {
                wifi.getExternalIpAddress(this);
            }
        } else {
            label.setVisibility(View.GONE);
            ip.setVisibility(View.GONE);
        }
    }

    /**
     * Activity paused
     */
    @Override
    public void onPause() {
        super.onPause();

        if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.dismiss();
        }
        scanProgressDialog = null;
    }

    /**
     * Activity destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        signalHandler.removeCallbacksAndMessages(null);

        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    /**
     * Activity restarted
     */
    @Override
    public void onRestart() {
        super.onRestart();

        registerReceiver(receiver, intentFilter);
    }

    /**
     * Save the state of an activity
     *
     * @param savedState Data to save
     */
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        ListAdapter adapter = hostList.getAdapter();
        if (adapter != null) {
            ArrayList<Host> adapterData = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
                Host item = (Host) adapter.getItem(i);
                adapterData.add(item);
            }
            savedState.putSerializable("hosts", adapterData);
            savedState.putString("wanIp", cachedWanIp);
        }
    }

    /**
     * Activity state restored
     *
     * @param savedState Saved data from the saved state
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        cachedWanIp = savedState.getString("wanIp");
        externalIp.setText(cachedWanIp);
        hosts = (ArrayList<Host>) savedState.getSerializable("hosts");
        if (hosts != null) {
            setupHostsAdapter();
        }
    }

    /**
     * Delegate to update the host list and dismiss the progress dialog
     * Gets called when host discovery has finished
     *
     * @param output The list of hosts to bind to the list view
     */
    @Override
    public void processFinish(final Host output) {
        scanHandler.post(new Runnable() {

            @Override
            public void run() {
                if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
                    scanProgressDialog.dismiss();
                }

                hosts.add(output);
                hostAdapter.sort(new Comparator<Host>() {

                    @Override
                    public int compare(Host lhs, Host rhs) {
                        try {
                            int leftIp = new BigInteger(InetAddress.getByName(lhs.getIp()).getAddress()).intValue();
                            int rightIp = new BigInteger(InetAddress.getByName(rhs.getIp()).getAddress()).intValue();

                            return leftIp - rightIp;
                        } catch (UnknownHostException ignored) {
                            return 0;
                        }
                    }
                });
                discoverHostsBtn.setText(discoverHostsStr + " (" + hosts.size() + ")");
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
        if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.incrementProgressBy(output);
        }
    }

    /**
     * Delegate to handle setting the external IP in the UI
     *
     * @param output External IP
     */
    @Override
    public void processFinish(String output) {
        cachedWanIp = output;
        externalIp.setText(output);
    }
}
