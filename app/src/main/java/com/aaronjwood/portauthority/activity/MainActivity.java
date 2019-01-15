package com.aaronjwood.portauthority.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
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

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.adapter.HostAdapter;
import com.aaronjwood.portauthority.async.DownloadAsyncTask;
import com.aaronjwood.portauthority.async.DownloadOuisAsyncTask;
import com.aaronjwood.portauthority.async.DownloadPortDataAsyncTask;
import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Network;
import com.aaronjwood.portauthority.network.Wired;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.parser.OuiParser;
import com.aaronjwood.portauthority.parser.PortParser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.utils.Errors;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends AppCompatActivity implements MainAsyncResponse {

    private final static int TIMER_INTERVAL = 1500;

    private Wireless wifi;
    private Wired ethernet;
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
    private IntentFilter intentFilter = new IntentFilter();
    private HostAdapter hostAdapter;
    private List<Host> hosts = Collections.synchronizedList(new ArrayList<Host>());
    private Database db;
    private DownloadAsyncTask ouiTask;
    private DownloadAsyncTask portTask;
    private boolean sortAscending;

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        /**
         * Detect if a network connection has been lost or established
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info == null) {
                return;
            }

            getNetworkInfo(info);
        }

    };

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        internalIp = findViewById(R.id.internalIpAddress);
        externalIp = findViewById(R.id.externalIpAddress);
        signalStrength = findViewById(R.id.signalStrength);
        ssid = findViewById(R.id.ssid);
        bssid = findViewById(R.id.bssid);
        hostList = findViewById(R.id.hostList);
        discoverHostsBtn = findViewById(R.id.discoverHosts);
        discoverHostsStr = getResources().getString(R.string.hostDiscovery);

        Context ctx = getApplicationContext();
        wifi = new Wireless(ctx);
        ethernet = new Wired(ctx);
        scanHandler = new Handler(Looper.getMainLooper());

        checkDatabase();
        db = Database.getInstance(ctx);

        setupHostsAdapter();
        setupDrawer();
        setupHostDiscovery();

        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    /**
     * Determines if the initial download of OUI and port data needs to be done.
     */
    public void checkDatabase() {
        if (getDatabasePath(Database.DATABASE_NAME).exists()) {
            return;
        }

        final MainActivity activity = this;
        new AlertDialog.Builder(activity, R.style.DialogTheme).setTitle("Generate Database")
                .setMessage("Do you want to create the OUI and port databases? " +
                        "This will download the official OUI list from Wireshark and port list from IANA. " +
                        "Note that you won't be able to resolve any MAC vendors or identify services without this data. " +
                        "You can always perform this from the menu later.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        ouiTask = new DownloadOuisAsyncTask(db, new OuiParser(), activity);
                        portTask = new DownloadPortDataAsyncTask(db, new PortParser(), activity);
                        ouiTask.execute();
                        portTask.execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).setIcon(android.R.drawable.ic_dialog_alert).show().setCanceledOnTouchOutside(false);
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
    public void setupMac() {
        TextView macAddress = findViewById(R.id.deviceMacAddress);
        TextView macVendor = findViewById(R.id.deviceMacVendor);

        try {
            if (!wifi.isEnabled()) {
                macAddress.setText(R.string.wifiDisabled);
                macVendor.setText(R.string.wifiDisabled);

                return;
            }

            String mac = wifi.getMacAddress();
            macAddress.setText(mac);

            String vendor = Host.findMacVendor(mac, db);
            macVendor.setText(vendor);
        } catch (UnknownHostException | SocketException | Wireless.NoWifiManagerException e) {
            macAddress.setText(R.string.noWifiConnection);
            macVendor.setText(R.string.noWifiConnection);
        } catch (IOException | SQLiteException | UnsupportedOperationException e) {
            macVendor.setText(R.string.getMacVendorFailed);
        } catch (Wireless.NoWifiInterfaceException e) {
            macAddress.setText(R.string.noWifiInterface);
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
                Resources resources = getResources();
                Context context = getApplicationContext();
                try {
                    if (!wifi.isConnected() && !ethernet.isConnected()) {
                        Errors.showError(context, resources.getString(R.string.notConnectedLan));
                        return;
                    }
                } catch (Network.NoConnectivityManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedWifiManager));
                    return;
                }

                int numSubnetHosts;
                try {
                    numSubnetHosts = wifi.getNumberOfHostsInWifiSubnet();
                } catch (Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedSubnetHosts));
                    return;
                } catch (Network.SubnetNotFoundException e) {
                    Errors.showError(context, resources.getString(R.string.subnetNotFound));
                    return;
                }

                setAnimations();

                hosts.clear();
                discoverHostsBtn.setText(discoverHostsStr);
                hostAdapter.notifyDataSetChanged();

                scanProgressDialog = new ProgressDialog(MainActivity.this, R.style.DialogTheme);
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.setTitle(resources.getString(R.string.hostScan));
                scanProgressDialog.setMessage(String.format(resources.getString(R.string.subnetHosts), numSubnetHosts));
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                scanProgressDialog.setProgress(0);
                scanProgressDialog.setMax(numSubnetHosts);
                scanProgressDialog.show();

                try {
                    Integer ip = wifi.getInternalWifiIpAddress(Integer.class);
                    new ScanHostsAsyncTask(MainActivity.this, db).execute(ip, wifi.getSubnet(), UserPreference.getHostSocketTimeout(context));
                    discoverHostsBtn.setAlpha(.3f);
                    discoverHostsBtn.setEnabled(false);
                } catch (UnknownHostException | Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.notConnectedLan));
                } catch (Network.SubnetNotFoundException e) {
                    Errors.showError(context, resources.getString(R.string.subnetNotFound));
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

    /**
     * Inflate our context menu to be used on the host list
     *
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.hostList) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.host_menu, menu);
        }
    }

    /**
     * Handles actions selected from the context menu for a host
     *
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
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
            case R.id.sortVendor:
                if (sortAscending) {
                    hostAdapter.sort(new Comparator<Host>() {
                        @Override
                        public int compare(Host lhs, Host rhs) {
                            return rhs.getVendor().toLowerCase().compareTo(lhs.getVendor().toLowerCase());
                        }
                    });
                } else {
                    hostAdapter.sort(new Comparator<Host>() {
                        @Override
                        public int compare(Host lhs, Host rhs) {
                            return lhs.getVendor().toLowerCase().compareTo(rhs.getVendor().toLowerCase());
                        }
                    });
                }

                sortAscending = !sortAscending;
                return true;
            case R.id.copyHostname:
                setClip("hostname", hosts.get(info.position).getHostname());

                return true;
            case R.id.copyIp:
                setClip("ip", hosts.get(info.position).getIp());

                return true;
            case R.id.copyMac:
                setClip("mac", hosts.get(info.position).getMac());

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Sets some text to the system's clipboard
     *
     * @param label Label for the text being set
     * @param text  The text to save to the system's clipboard
     */
    private void setClip(CharSequence label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Gets network information about the device and updates various UI elements
     */
    private void getNetworkInfo(NetworkInfo info) {
        setupMac();
        getExternalIp();

        final Resources resources = getResources();
        final Context context = getApplicationContext();
        try {
            boolean enabled = wifi.isEnabled();
            if (!info.isConnected() || !enabled) {
                signalHandler.removeCallbacksAndMessages(null);
                internalIp.setText(Wireless.getInternalMobileIpAddress());
            }

            if (!enabled) {
                signalStrength.setText(R.string.wifiDisabled);
                ssid.setText(R.string.wifiDisabled);
                bssid.setText(R.string.wifiDisabled);

                return;
            }
        } catch (Wireless.NoWifiManagerException e) {
            Errors.showError(context, resources.getString(R.string.failedWifiManager));
        }

        if (!info.isConnected()) {
            signalStrength.setText(R.string.noWifiConnection);
            ssid.setText(R.string.noWifiConnection);
            bssid.setText(R.string.noWifiConnection);

            return;
        }

        signalHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int signal;
                int speed;
                try {
                    speed = wifi.getLinkSpeed();
                } catch (Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedLinkSpeed));
                    return;
                }
                try {
                    signal = wifi.getSignalStrength();
                } catch (Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedSignal));
                    return;
                }
                
                signalStrength.setText(String.format(resources.getString(R.string.signalLink), signal, speed));
                signalHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);

        getInternalIp();

        String wifiSsid;
        String wifiBssid;
        try {
            wifiSsid = wifi.getSSID();
        } catch (Wireless.NoWifiManagerException e) {
            Errors.showError(context, resources.getString(R.string.failedSsid));
            return;
        }
        try {
            wifiBssid = wifi.getBSSID();
        } catch (Wireless.NoWifiManagerException e) {
            Errors.showError(context, resources.getString(R.string.failedBssid));
            return;
        }

        ssid.setText(wifiSsid);
        bssid.setText(wifiBssid);
    }

    /**
     * Sets up event handlers and items for the left drawer
     */
    private void setupDrawer() {
        final DrawerLayout leftDrawer = findViewById(R.id.leftDrawer);
        final RelativeLayout leftDrawerLayout = findViewById(R.id.leftDrawerLayout);

        ImageView drawerIcon = findViewById(R.id.leftDrawerIcon);
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

        ListView upperList = findViewById(R.id.upperLeftDrawerList);
        ListView lowerList = findViewById(R.id.lowerLeftDrawerList);

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
                        ouiTask = new DownloadOuisAsyncTask(db, new OuiParser(), MainActivity.this);
                        ouiTask.execute();
                        break;
                    case 1:
                        portTask = new DownloadPortDataAsyncTask(db, new PortParser(), MainActivity.this);
                        portTask.execute();
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                        break;
                }
                leftDrawer.closeDrawer(leftDrawerLayout);
            }
        });
    }

    /**
     * Wrapper method for getting the internal wireless IP address.
     * This gets the netmask, counts the bits set (subnet size),
     * then prints it along side the IP.
     */
    private void getInternalIp() {
        try {
            int netmask = wifi.getSubnet();
            String internalIpWithSubnet = wifi.getInternalWifiIpAddress(String.class) + "/" + Integer.toString(netmask);
            internalIp.setText(internalIpWithSubnet);
        } catch (UnknownHostException | Wireless.NoWifiManagerException e) {
            Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
        } catch (Network.SubnetNotFoundException e) {
            Errors.showError(getApplicationContext(), getResources().getString(R.string.subnetNotFound));
        }
    }

    /**
     * Wrapper for getting the external IP address
     * We can control whether or not to do this based on the user's preference
     * If the user doesn't want this then hide the appropriate views
     */
    private void getExternalIp() {
        TextView label = findViewById(R.id.externalIpAddressLabel);
        TextView ip = findViewById(R.id.externalIpAddress);

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

        unregisterReceiver(receiver);
        signalHandler.removeCallbacksAndMessages(null);

        if (scanProgressDialog != null) {
            scanProgressDialog.dismiss();
        }

        if (ouiTask != null) {
            ouiTask.cancel(true);
        }

        if (portTask != null) {
            portTask.cancel(true);
        }

        scanProgressDialog = null;
        ouiTask = null;
        portTask = null;
    }

    /**
     * Activity resumed.
     */
    @Override
    public void onResume() {
        super.onResume();

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
     * @param h The host to add to the list of discovered hosts
     * @param i Number of hosts
     */
    @Override
    public void processFinish(final Host h, final AtomicInteger i) {
        scanHandler.post(new Runnable() {

            @Override
            public void run() {
                hosts.add(h);
                hostAdapter.sort(new Comparator<Host>() {

                    @Override
                    public int compare(Host lhs, Host rhs) {
                        try {
                            int leftIp = ByteBuffer.wrap(InetAddress.getByName(lhs.getIp()).getAddress()).getInt();
                            int rightIp = ByteBuffer.wrap(InetAddress.getByName(rhs.getIp()).getAddress()).getInt();

                            return leftIp - rightIp;
                        } catch (UnknownHostException ignored) {
                            return 0;
                        }
                    }
                });

                discoverHostsBtn.setText(discoverHostsStr + " (" + hosts.size() + ")");
                if (i.decrementAndGet() == 0) {
                    discoverHostsBtn.setAlpha(1);
                    discoverHostsBtn.setEnabled(true);
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

    /**
     * Delegate to dismiss the progress dialog
     *
     * @param output
     */
    @Override
    public void processFinish(final boolean output) {
        scanHandler.post(new Runnable() {

            @Override
            public void run() {
                if (output && scanProgressDialog != null && scanProgressDialog.isShowing()) {
                    scanProgressDialog.dismiss();
                }
            }
        });
    }

    /**
     * Delegate to handle bubbled up errors
     *
     * @param output The exception we want to handle
     * @param <T>    Exception
     */
    @Override
    public <T extends Throwable> void processFinish(final T output) {
        scanHandler.post(new Runnable() {

            @Override
            public void run() {
                Errors.showError(getApplicationContext(), output.getLocalizedMessage());
            }
        });
    }
}
