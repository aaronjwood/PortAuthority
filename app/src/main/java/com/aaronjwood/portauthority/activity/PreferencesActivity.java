package com.aaronjwood.portauthority.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import com.aaronjwood.portauthority.R;

public class PreferencesActivity extends PreferenceActivity {

    /**
     * Activity created
     * @param savedInstanceState Data from a saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        /**
         * Fragment created
         * @param savedInstanceState Data from a saved state
         */
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

}
