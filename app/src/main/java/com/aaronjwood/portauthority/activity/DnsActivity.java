package com.aaronjwood.portauthority.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.utils.UserPreference;

public final class DnsActivity extends AppCompatActivity {

    private EditText domainName;

    /**
     * Activity created
     *
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dns);

        this.domainName = (EditText) findViewById(R.id.domainName);
        this.domainName.setText(UserPreference.getLastUsedDomainName(this));
    }

    /**
     * Clean up
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserPreference.saveLastUsedDomainName(this, this.domainName.getText().toString());
    }

    /**
     * Event handler for when a DNS lookup is requested
     */
    private void dnsLookupClick() {
        Button dnsLookupButton = (Button) findViewById(R.id.dnsLookup);
        dnsLookupButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Initiate DNS lookups
             * @param view
             */
            @Override
            public void onClick(View view) {

            }
        });
    }
}
