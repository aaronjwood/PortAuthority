package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.aaronjwood.portauthority.async.WanIpAsyncTask;
import com.aaronjwood.portauthority.response.MainAsyncResponse;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class Wireless {

    private Context context;

    public static class NoWifiManagerException extends Exception {
    }

    public static class NoConnectivityManagerException extends Exception {
    }

    /**
     * Constructor to set the activity for context
     *
     * @param context The activity to use for context
     */
    public Wireless(Context context) {
        this.context = context;
    }

    /**
     * Gets the MAC address of the device
     *
     * @return MAC address
     */
    public String getMacAddress() throws UnknownHostException, SocketException, NoWifiManagerException {
        String address = getWifiInfo().getMacAddress(); //Won't work on Android 6+ https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id
        if (!"02:00:00:00:00:00".equals(address)) {
            return address;
        }

        //This should get us the device's MAC address on Android 6+
        NetworkInterface iface = NetworkInterface.getByInetAddress(getWifiInetAddress());

        byte[] mac = iface.getHardwareAddress();

        StringBuilder buf = new StringBuilder();
        for (byte aMac : mac) {
            buf.append(String.format("%02x:", aMac));
        }

        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }

        return buf.toString();
    }

    /**
     * Gets the device's wireless address
     *
     * @return Wireless address
     */
    private InetAddress getWifiInetAddress() throws UnknownHostException, NoWifiManagerException {
        String ipAddress = getInternalWifiIpAddress(String.class);
        return InetAddress.getByName(ipAddress);
    }

    /**
     * Gets the signal strength of the wireless network that the device is connected to
     *
     * @return Signal strength
     */
    public int getSignalStrength() throws NoWifiManagerException {
        return getWifiInfo().getRssi();
    }

    /**
     * Gets the BSSID of the wireless network that the device is connected to
     *
     * @return BSSID
     */
    public String getBSSID() throws NoWifiManagerException {
        return getWifiInfo().getBSSID();
    }

    /**
     * Gets the SSID of the wireless network that the device is connected to
     *
     * @return SSID
     */
    public String getSSID() throws NoWifiManagerException {
        String ssid = getWifiInfo().getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }


    /**
     * Gets the device's internal LAN IP address associated with the WiFi network
     *
     * @param type
     * @param <T>
     * @return Local WiFi network LAN IP address
     */
    public <T> T getInternalWifiIpAddress(Class<T> type) throws UnknownHostException, NoWifiManagerException {
        int ip = getWifiInfo().getIpAddress();

        //Endianness can be a potential issue on some hardware
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ip = Integer.reverseBytes(ip);
        }

        byte[] ipByteArray = BigInteger.valueOf(ip).toByteArray();


        if (type.isInstance("")) {
            return type.cast(InetAddress.getByAddress(ipByteArray).getHostAddress());
        } else {
            return type.cast(new BigInteger(InetAddress.getByAddress(ipByteArray).getAddress()).intValue());
        }

    }

    /**
     * Gets the Wifi Manager DHCP information and returns the Netmask of the internal Wifi Network as an int
     *
     * @return Internal Wifi Subnet Netmask
     */
    public int getInternalWifiSubnet() throws NoWifiManagerException {
        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return 0;
        }

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null) {
            return 0;
        }

        int netmask = Integer.bitCount(dhcpInfo.netmask);
        /*
         * Workaround for #82477
         * https://code.google.com/p/android/issues/detail?id=82477
         * If dhcpInfo returns a subnet that cannot exist, then
         * look up the Network interface instead.
         */
        if (netmask < 4 || netmask > 32) {
            try {
                InetAddress inetAddress = getWifiInetAddress();
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                if (networkInterface == null) {
                    return 0;
                }

                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if (inetAddress != null && inetAddress.equals(address.getAddress())) {
                        return address.getNetworkPrefixLength(); // This returns a short of the CIDR notation.
                    }
                }
            } catch (SocketException | UnknownHostException ignored) {
            }
        }

        return netmask;
    }


    /**
     * Returns the number of hosts in the subnet.
     *
     * @return Number of hosts as an integer.
     */
    public int getNumberOfHostsInWifiSubnet() throws NoWifiManagerException {
        Double subnet = (double) getInternalWifiSubnet();
        double hosts;
        double bitsLeft = 32.0d - subnet;
        hosts = Math.pow(2.0d, bitsLeft) - 2.0d;

        return (int) hosts;
    }


    /**
     * Gets the device's internal LAN IP address associated with the cellular network
     *
     * @return Local cellular network LAN IP address
     */
    public static String getInternalMobileIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en != null && en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            return "Unknown";
        }

        return "Unknown";
    }

    /**
     * Gets the device's external (WAN) IP address
     *
     * @param delegate Called when the external IP address has been fetched
     */
    public void getExternalIpAddress(MainAsyncResponse delegate) {
        new WanIpAsyncTask(delegate).execute();
    }

    /**
     * Gets the current link speed of the wireless network that the device is connected to
     *
     * @return Wireless link speed
     */
    public int getLinkSpeed() throws NoWifiManagerException {
        return getWifiInfo().getLinkSpeed();
    }

    /**
     * Determines if the device is connected to a WiFi network or not
     *
     * @return True if the device is connected, false if it isn't
     */
    public boolean isConnectedWifi() throws NoConnectivityManagerException {
        NetworkInfo info = getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info != null && info.isConnectedOrConnecting();
    }

    /**
     * Determines if WiFi is enabled on the device or not
     *
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() throws NoWifiManagerException {
        return getWifiManager().isWifiEnabled();
    }

    /**
     * Gets the Android WiFi manager in the context of the current activity
     *
     * @return WifiManager
     */
    private WifiManager getWifiManager() throws NoWifiManagerException {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            throw new NoWifiManagerException();
        }

        return manager;
    }

    /**
     * Gets the Android WiFi information in the context of the current activity
     *
     * @return WiFi information
     */
    private WifiInfo getWifiInfo() throws NoWifiManagerException {
        return getWifiManager().getConnectionInfo();
    }

    /**
     * Gets the Android connectivity manager in the context of the current activity
     *
     * @return Connectivity manager
     */
    private ConnectivityManager getConnectivityManager() throws NoConnectivityManagerException {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            throw new NoConnectivityManagerException();
        }

        return manager;
    }

    /**
     * Gets the Android network information in the context of the current activity
     *
     * @return Network information
     */
    private NetworkInfo getNetworkInfo(int type) throws NoConnectivityManagerException {
        return getConnectivityManager().getNetworkInfo(type);
    }

}
