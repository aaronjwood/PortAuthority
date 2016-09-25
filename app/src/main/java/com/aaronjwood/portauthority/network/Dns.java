package com.aaronjwood.portauthority.network;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public final class Dns {

    public static String lookupA(String domain) {
        Record[] records;

        try {
            records = new Lookup(domain, Type.A).run();
            String data = "";

            for(Record record : records) {
                data += record.rdataToString();
            }

            return data;
        } catch (TextParseException e) {
            return "Could not lookup record!";
        }
    }

}
