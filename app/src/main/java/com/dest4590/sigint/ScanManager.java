package com.dest4590.sigint;

import android.os.Handler;
import android.os.Looper;

import com.dest4590.sigint.sniffer.Device;
import com.dest4590.sigint.sniffer.Sniffer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScanManager {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScanCallback callback;
    private volatile ScanState scanState = ScanState.IDLE;
    private volatile Thread scanThread;
    private long lastAutosyncTime = 0;

    public ScanManager(ScanCallback callback) {
        this.callback = callback;
    }

    public ScanState getScanState() {
        return scanState;
    }

    public void startScan() {
        scanState = ScanState.SCANNING;
        Sniffer.start();

        scanThread = new Thread(() -> {
            while (scanState != ScanState.IDLE) {
                if (scanState == ScanState.PAUSED) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                Device[] devices = Sniffer.scan();
                mainHandler.post(() -> {
                    if (devices != null) {
                        callback.onDevicesFound(devices);
                    }
                    if (scanState == ScanState.SCANNING) {
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        callback.onScanStatusUpdate("STATUS: SCANNING [" + time + "]");
                    }
                });

                if (callback.isAutosyncEnabled()) {
                    long now = System.currentTimeMillis();
                    int intervalMin = callback.getAutosyncIntervalMin();
                    if (now - lastAutosyncTime > intervalMin * 60 * 1000L) {
                        lastAutosyncTime = now;
                        mainHandler.post(callback::onAutosyncTriggered);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        scanThread.start();
    }

    public void pauseScan() {
        scanState = ScanState.PAUSED;
    }

    public void resumeScan() {
        scanState = ScanState.SCANNING;
    }

    public void stopScan() {
        if (scanState == ScanState.IDLE) return;
        scanState = ScanState.IDLE;
        Sniffer.stop();
        if (scanThread != null) {
            scanThread.interrupt();
            scanThread = null;
        }
    }

    public void setLastAutosyncTime(long time) {
        this.lastAutosyncTime = time;
    }

    public interface ScanCallback {
        void onDevicesFound(Device[] devices);

        void onScanStatusUpdate(String status);

        boolean isAutosyncEnabled();

        int getAutosyncIntervalMin();

        void onAutosyncTriggered();
    }
}