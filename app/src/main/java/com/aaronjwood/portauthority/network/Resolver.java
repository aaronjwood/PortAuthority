package com.aaronjwood.portauthority.network;

import java.io.IOException;
import java.net.InetAddress;

public interface Resolver {

    String[] resolve(InetAddress ip) throws IOException;

    void close();

}
