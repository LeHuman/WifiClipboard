package com.lehuman.wificlipboard;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    public static final String TCP_DEFAULT_PORT_NAME = "TCP_DEFAULT_PORT";
    public static final String TCP_DEFAULT_TIMEOUT_NAME = "TCP_DEFAULT_TIMEOUT";
    public static final String TCP_DEFAULT_TOAST_NAME = "TCP_DEFAULT_TOAST";

    public static final int TCP_DEFAULT_PORT = 55051;
    public static final int TCP_DEFAULT_TIMEOUT = 5000;
    public static final boolean TCP_DEFAULT_TOAST = false;

    public static int getPORT(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TCP_DEFAULT_PORT_NAME, Context.MODE_PRIVATE);
        return sp.getInt(TCP_DEFAULT_PORT_NAME, TCP_DEFAULT_PORT);
    }

    public static int getTIMEOUT(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TCP_DEFAULT_TIMEOUT_NAME, Context.MODE_PRIVATE);
        return sp.getInt(TCP_DEFAULT_TIMEOUT_NAME, TCP_DEFAULT_TIMEOUT);
    }

    public static boolean getTOAST(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TCP_DEFAULT_TOAST_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(TCP_DEFAULT_TOAST_NAME, TCP_DEFAULT_TOAST);
    }

    public static void setPORT(Context context, int port) {
        SharedPreferences.Editor editor = context.getSharedPreferences(TCP_DEFAULT_PORT_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(TCP_DEFAULT_PORT_NAME, port);
        editor.apply();
    }

    public static void setTIMEOUT(Context context, int timeout) {
        SharedPreferences.Editor editor = context.getSharedPreferences(TCP_DEFAULT_TIMEOUT_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(TCP_DEFAULT_TIMEOUT_NAME, timeout);
        editor.apply();
    }

    public static void setTOAST(Context context, boolean enableToasts) {
        SharedPreferences.Editor editor = context.getSharedPreferences(TCP_DEFAULT_TOAST_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(TCP_DEFAULT_TOAST_NAME, enableToasts);
        editor.apply();
    }

    public static void reset(Context context) {
        setPORT(context, TCP_DEFAULT_PORT);
        setTIMEOUT(context, TCP_DEFAULT_TIMEOUT);
        setTOAST(context, TCP_DEFAULT_TOAST);
    }
}
