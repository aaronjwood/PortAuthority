package com.aaronjwood.portauthority;

import android.app.Application;
import android.os.Process;

import com.squareup.leakcanary.LeakCanary;

public class PortAuthority extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        LeakCanary.install(this);
        checkReplacingState(); // Workaround for https://issuetracker.google.com/issues/36972466
    }

    /**
     * Kills the process if we can't get our own resources.
     */
    private void checkReplacingState() {
        if (getResources() == null) {
            Process.killProcess(Process.myPid());
        }
    }

}
