package com.aaronjwood.portauthority.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.utils.UserPreference;

public final class DnsActivity extends AppCompatActivity {

    private EditText domainName;

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
}
