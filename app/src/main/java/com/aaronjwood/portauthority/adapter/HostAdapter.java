package com.aaronjwood.portauthority.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.network.Host;

import java.util.List;

public final class HostAdapter extends ArrayAdapter<Host> {
    private final List<Host> data;

    public HostAdapter(Context context, List<Host> data) {
        super(context, 0, data);
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;
        Context context = getContext();

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.host_list_item, parent, false);

            view = new ViewHolder();
            view.hostname = rowView.findViewById(R.id.hostname);
            view.hostIp = rowView.findViewById(R.id.hostIp);
            view.hostMac = rowView.findViewById(R.id.hostMac);
            view.hostMacVendor = rowView.findViewById(R.id.hostMacVendor);

            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        Host item = data.get(position);
        view.hostname.setText(item.getHostname());
        view.hostIp.setText(item.getIp());
        view.hostMac.setText(item.getMac());
        view.hostMacVendor.setText(item.getVendor());

        return rowView;
    }

    private static class ViewHolder {
        private TextView hostname;
        private TextView hostIp;
        private TextView hostMac;
        private TextView hostMacVendor;
    }
}
