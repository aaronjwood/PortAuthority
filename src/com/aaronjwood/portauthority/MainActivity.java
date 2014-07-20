package com.aaronjwood.portauthority;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.aaronjwood.portauthority.Network.Wireless;

public class MainActivity extends Activity {

    private Wireless wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.wifi = new Wireless(this);

        TextView macAddress = (TextView) findViewById(R.id.deviceMacAddress);
        macAddress.setText(this.wifi.getMacAddress());

        TextView ipAddress = (TextView) findViewById(R.id.internalIpAddress);
        ipAddress.setText(this.wifi.getInternalIpAddress());

        this.wifi
                .getExternalIpAddress((TextView) findViewById(R.id.externalIpAddress));
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
