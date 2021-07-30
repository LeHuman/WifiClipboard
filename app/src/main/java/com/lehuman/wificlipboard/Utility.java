package com.lehuman.wificlipboard;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Utility {

    public static String inputStreamString(InputStream inputStream) {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            for (int result = bis.read(); result != -1; result = bis.read()) {
                buf.write((byte) result);
            }
        } catch (IOException ignored) {
        }

        try {
            return buf.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface inter : interfaces) {
                List<InetAddress> addresses = Collections.list(inter.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (hostAddress.indexOf(':') < 0)
                            return hostAddress;
                    }
                }
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }

}
