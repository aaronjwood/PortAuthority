package com.aaronjwood.portauthority.network;

import com.aaronjwood.portauthority.async.DnsLookupAsyncTask;
import com.aaronjwood.portauthority.response.DnsAsyncResponse;

import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;

public final class Dns {

    public static void lookup(String domain, String recordType, DnsAsyncResponse delegate) {
        String type;

        switch (recordType) {
            case "A":
                type = Integer.toString(Type.A);
                break;
            default:
                type = Integer.toString(-1);
                break;
        }

        List<String> data = new ArrayList<>();
        data.add(domain);
        data.add(type);

        new DnsLookupAsyncTask(delegate).execute(data);
    }

}
