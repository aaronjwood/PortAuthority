package com.aaronjwood.portauthority.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;

import java.util.List;

public final class HostAdapter extends ArrayAdapter<Host> {
    private final Activity activity;
    private final List<Host> data;

    public HostAdapter(Activity activity, List<Host> data) {
        super(activity, 0, data);
        this.activity = activity;
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;

        if (rowView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.host_list_item, null);

            view = new ViewHolder();
            view.hostname = (TextView) rowView.findViewById(R.id.hostname);
            view.hostIp = (TextView) rowView.findViewById(R.id.hostIp);
            view.hostMac = (TextView) rowView.findViewById(R.id.hostMac);
            view.hostMacVendor = (TextView) rowView.findViewById(R.id.hostMacVendor);

            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        Host item = data.get(position);
        String mac = item.getMac().replace(":", "").substring(0, 6);
        view.hostname.setText(item.getHostname());
        view.hostIp.setText(item.getIp());
        view.hostMac.setText(item.getMac());
        view.hostMacVendor.setText(Host.getMacVendor(mac, activity));

        return rowView;
    }

    private static class ViewHolder {
        TextView hostname;
        TextView hostIp;
        TextView hostMac;
        TextView hostMacVendor;
    }
}
