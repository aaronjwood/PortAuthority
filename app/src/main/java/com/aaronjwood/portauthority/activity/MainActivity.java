package com.aaronjwood.portauthority.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.adapter.HostAdapter;
import com.aaronjwood.portauthority.async.DownloadAsyncTask;
import com.aaronjwood.portauthority.async.DownloadOuisAsyncTask;
import com.aaronjwood.portauthority.async.DownloadPortDataAsyncTask;
import com.aaronjwood.portauthority.async.ScanHostsAsyncTask;
import com.aaronjwood.portauthority.async.WolAsyncTask;
import com.aaronjwood.portauthority.db.Database;
import com.aaronjwood.portauthority.network.Host;
import com.aaronjwood.portauthority.network.Wireless;
import com.aaronjwood.portauthority.parser.OuiParser;
import com.aaronjwood.portauthority.parser.PortParser;
import com.aaronjwood.portauthority.response.MainAsyncResponse;
import com.aaronjwood.portauthority.utils.Errors;
import com.aaronjwood.portauthority.utils.UserPreference;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends AppCompatActivity implements MainAsyncResponse {

    private final static int TIMER_INTERVAL = 1500;
    private final static int COARSE_LOCATION_REQUEST = 1;
    private final static int FINE_LOCATION_REQUEST = 2;

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
    private final Handler signalHandler = new Handler();
    private Handler scanHandler;
    private final IntentFilter intentFilter = new IntentFilter();
    private HostAdapter hostAdapter;
    private List<Host> hosts = Collections.synchronizedList(new ArrayList<>());
    private Database db;
    private DownloadAsyncTask ouiTask;
    private DownloadAsyncTask portTask;
    private boolean sortAscending;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

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

        Context context = getApplicationContext();
        wifi = new Wireless(context);
        scanHandler = new Handler(Looper.getMainLooper());

        checkDatabase();
        db = Database.getInstance(context);

        setupHostsAdapter();
        setupDrawer();
        setupHostDiscovery();

        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        ssidAccess(context);
    }

    /**
     * Android 8+ now requires extra location permissions to read the SSID.
     * Determine what permissions to prompt the user for based on saved state.
     *
     * @param context
     */
    private void ssidAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (UserPreference.getCoarseLocationPermDiag(context) || UserPreference.getFineLocationPermDiag(context)) {
                return;
            }

            Activity activity = this;
            String version = "8-9";
            String message = getResources().getString(R.string.ssidCoarseMsg, version);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                version = "10+";
                message = getResources().getString(R.string.ssidFineMsg, version);
            }

            String title = getResources().getString(R.string.ssidAccessTitle, version);
            new AlertDialog.Builder(activity, R.style.DialogTheme).setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            UserPreference.saveFineLocationPermDiag(context);
                        } else {
                            UserPreference.saveCoarseLocationPermDiag(context);
                        }

                        String perm = Manifest.permission.ACCESS_COARSE_LOCATION;
                        int request = COARSE_LOCATION_REQUEST;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            perm = Manifest.permission.ACCESS_FINE_LOCATION;
                            request = FINE_LOCATION_REQUEST;
                        }

                        if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity, new String[]{perm}, request);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert).show().setCanceledOnTouchOutside(false);
        }
    }

    /**
     * Determines if the initial download of OUI and port data needs to be done.
     */
    public void checkDatabase() {
        if (getDatabasePath(Database.DATABASE_NAME).exists()) {
            return;
        }

        final MainActivity activity = this;
        new AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(R.string.ouiDbTitle)
                .setMessage(R.string.ouiDbMsg)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    ouiTask = new DownloadOuisAsyncTask(db, new OuiParser(), activity);
                    ouiTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                })
                .setNegativeButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(android.R.drawable.ic_dialog_alert).show()
                .setCanceledOnTouchOutside(false);

        new AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(R.string.portDbTitle)
                .setMessage(R.string.portDbMsg)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    portTask = new DownloadPortDataAsyncTask(db, new PortParser(), activity);
                    portTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                })
                .setNegativeButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(android.R.drawable.ic_dialog_alert).show()
                .setCanceledOnTouchOutside(false);
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
        } catch (SQLiteException | UnsupportedOperationException e) {
            macVendor.setText(R.string.getMacVendorFailed);
        } catch (Wireless.NoWifiInterface e) {
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
                    if (!wifi.isEnabled()) {
                        Errors.showError(context, resources.getString(R.string.wifiDisabled));
                        return;
                    }

                    if (!wifi.isConnectedWifi()) {
                        Errors.showError(context, resources.getString(R.string.notConnectedWifi));
                        return;
                    }
                } catch (Wireless.NoWifiManagerException | Wireless.NoConnectivityManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedWifiManager));
                    return;
                }

                int numSubnetHosts;
                try {
                    numSubnetHosts = wifi.getNumberOfHostsInWifiSubnet();
                } catch (Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.failedSubnetHosts));
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
                    new ScanHostsAsyncTask(MainActivity.this, db).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ip, wifi.getInternalWifiSubnet(), UserPreference.getHostSocketTimeout(context));
                    discoverHostsBtn.setAlpha(.3f);
                    discoverHostsBtn.setEnabled(false);
                } catch (UnknownHostException | Wireless.NoWifiManagerException e) {
                    Errors.showError(context, resources.getString(R.string.notConnectedWifi));
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
            case R.id.sortIp:
                sortAscending = !sortAscending;
                if (sortAscending) {
                    hostAdapter.sort((lhs, rhs) -> {
                        int leftIp = ByteBuffer.wrap(lhs.getAddress()).getInt();
                        int rightIp = ByteBuffer.wrap(rhs.getAddress()).getInt();
                        return rightIp - leftIp;
                    });
                } else {
                    hostAdapter.sort((lhs, rhs) -> {
                        int leftIp = ByteBuffer.wrap(lhs.getAddress()).getInt();
                        int rightIp = ByteBuffer.wrap(rhs.getAddress()).getInt();
                        return leftIp - rightIp;
                    });
                }

                return true;
            case R.id.sortHostname:
                if (sortAscending) {
                    hostAdapter.sort((lhs, rhs) -> rhs.getHostname().toLowerCase().compareTo(lhs.getHostname().toLowerCase()));
                } else {
                    hostAdapter.sort((lhs, rhs) -> lhs.getHostname().toLowerCase().compareTo(rhs.getHostname().toLowerCase()));
                }

                sortAscending = !sortAscending;
                return true;
            case R.id.sortVendor:
                if (sortAscending) {
                    hostAdapter.sort((lhs, rhs) -> rhs.getVendor().toLowerCase().compareTo(lhs.getVendor().toLowerCase()));
                } else {
                    hostAdapter.sort((lhs, rhs) -> lhs.getVendor().toLowerCase().compareTo(rhs.getVendor().toLowerCase()));
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
                        Dialog wolDialog = new Dialog(MainActivity.this, R.style.DialogTheme);
                        wolDialog.setContentView(R.layout.wake_on_lan);
                        wolDialog.show();
                        Button wakeUp = wolDialog.findViewById(R.id.wolWake);
                        wakeUp.setOnClickListener(v -> {
                            try {
                                if (!wifi.isConnectedWifi()) {
                                    Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
                                    return;
                                }
                            } catch (Wireless.NoConnectivityManagerException e) {
                                Errors.showError(getApplicationContext(), getResources().getString(R.string.failedWifiManager));
                                return;
                            }

                            EditText ip = wolDialog.findViewById(R.id.wolIp);
                            EditText mac = wolDialog.findViewById(R.id.wolMac);
                            String ipVal = ip.getText().toString();
                            String macVal = mac.getText().toString();
                            if (ipVal.isEmpty() || macVal.isEmpty()) {
                                return;
                            }

                            new WolAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, macVal, ipVal);
                            Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.waking), ipVal), Toast.LENGTH_SHORT).show();
                        });
                        break;
                    case 2:
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
                        ouiTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        break;
                    case 1:
                        portTask = new DownloadPortDataAsyncTask(db, new PortParser(), MainActivity.this);
                        portTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            int netmask = wifi.getInternalWifiSubnet();
            String internalIpWithSubnet = wifi.getInternalWifiIpAddress(String.class) + "/" + netmask;
            internalIp.setText(internalIpWithSubnet);
        } catch (UnknownHostException | Wireless.NoWifiManagerException e) {
            Errors.showError(getApplicationContext(), getResources().getString(R.string.notConnectedLan));
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
    public void onSaveInstanceState(@NonNull Bundle savedState) {
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
        scanHandler.post(() -> {
            if (h != null) {
                hosts.add(h);
            }
            hostAdapter.sort((lhs, rhs) -> {
                int leftIp = ByteBuffer.wrap(lhs.getAddress()).getInt();
                int rightIp = ByteBuffer.wrap(rhs.getAddress()).getInt();
                return leftIp - rightIp;
            });

            discoverHostsBtn.setText(discoverHostsStr + " (" + hosts.size() + ")");
            if (i.decrementAndGet() == 0) {
                discoverHostsBtn.setAlpha(1);
                discoverHostsBtn.setEnabled(true);
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
        scanHandler.post(() -> {
            if (output && scanProgressDialog != null && scanProgressDialog.isShowing()) {
                scanProgressDialog.dismiss();
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
        scanHandler.post(() -> Errors.showError(getApplicationContext(), output.getLocalizedMessage()));
    }
}
