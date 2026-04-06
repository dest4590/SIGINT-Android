package com.dest4590.sigint.sniffer;

public class Database {
    static {
        System.loadLibrary("sigint");
    }

    /**
     * Aggregate statistics JSON string:
     * {"total":N,"beacons":N,"connectable":N,"avgRssi":N,"types":{...},"vendors":{...}}
     */
    public static native String getStatsJson();

    public static native String getSavedStatsJson();

    /**
     * Persist device list to the app's internal files directory.
     */
    public static native String saveStats();

    /**
     * Clear the database (does NOT stop scanning).
     */
    public static native void clear();
}
