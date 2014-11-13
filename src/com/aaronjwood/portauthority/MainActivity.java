package com.aaronjwood.portauthority;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.aaronjwood.portauthority.Network.Wireless;

public class MainActivity extends Activity {

    private Wireless wifi;
    private final static int TIMER_INTERVAL = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.wifi = new Wireless(this);

        this.wifi
                .getExternalIpAddress((TextView) findViewById(R.id.externalIpAddress));

        TextView macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        macAddress.setText(this.wifi.getMacAddress());

        TextView ipAddress = (TextView) findViewById(R.id.internalIpAddress);
        ipAddress.setText(this.wifi.getInternalIpAddress());

        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                TextView signalStrength = (TextView) findViewById(R.id.signalStrength);
                signalStrength.setText(String.valueOf(wifi.getSignalStrength())
                        + " dBm");
                mHandler.postDelayed(this, TIMER_INTERVAL);
            }
        }, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.wifi.releaseMulticastLock();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.wifi.releaseMulticastLock();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.wifi.acquireMulticastLock();
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
