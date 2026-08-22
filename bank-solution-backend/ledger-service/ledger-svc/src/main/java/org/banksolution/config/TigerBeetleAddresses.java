package org.banksolution.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public final class TigerBeetleAddresses {

    private TigerBeetleAddresses() {
    }

    public static String[] resolve(List<String> addresses) {
        return addresses.stream().map(TigerBeetleAddresses::resolveOne).toArray(String[]::new);
    }

    private static String resolveOne(String address) {
        int separator = address.lastIndexOf(':');
        if (separator < 0 || address.startsWith("[")) {
            return address;
        }

        String host = address.substring(0, separator);
        String port = address.substring(separator + 1);

        try {
            return InetAddress.getByName(host).getHostAddress() + ":" + port;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot resolve TigerBeetle host: " + host, e);
        }
    }
}
