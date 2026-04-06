package com.dest4590.sigint.sniffer;

public class Sniffer {
    static {
        System.loadLibrary("sigint");
    }

    /**
     * Initialize the native scanner (must be called first).
     */
    public static native void init(Object context);

    /**
     * Start background BLE scanning.
     */
    public static native void start();

    /**
     * Stop background BLE scanning.
     */
    public static native void stop();

    /**
     * Snapshot of all currently tracked devices.
     */
    public static native Device[] scan();

    /**
     * Total number of tracked devices (fast path, no Device[] allocation).
     */
    public static native int getDeviceCount();

    /**
     * Clear the in-memory device map (does NOT stop scanning).
     */
    public static native void clear();
}

