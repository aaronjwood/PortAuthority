package com.aaronjwood.portauthority.network;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Wired extends Network {

    public Wired(Context context) {
        super(context);
    }

    /**
     * Reads the ethernet MAC address from the system.
     *
     * @return MAC address.
     * @throws IOException
     */
    @Override
    String getMacAddress() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader("/sys/class/net/eth0/address"))) {
            String address = br.readLine();
            return address.substring(0, 17);
        }
    }

}
