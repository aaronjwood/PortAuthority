package com.aaronjwood.portauthority.listener;

import android.view.View;
import android.widget.ArrayAdapter;

import java.util.List;

public class ScanPortsListener implements View.OnClickListener {

    private final List<String> ports;
    private final ArrayAdapter<String> adapter;

    /**
     * New click listener for scanning ports
     *
     * @param ports   Port list
     * @param adapter Bridge for the view and underlying data
     */
    protected ScanPortsListener(List<String> ports, ArrayAdapter<String> adapter) {
        this.ports = ports;
        this.adapter = adapter;
    }

    /**
     * Common functionality to be run before each scan
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        ports.clear();
        adapter.notifyDataSetChanged();
    }
}
