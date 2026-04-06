package com.dest4590.sigint.sniffer;

import java.util.List;
import java.util.Map;

public class Device {
    public String id;
    public String name;
    public short rssi;
    public Map<Integer, byte[]> manufacturerData;
    public List<String> services;
    public String firstSeen;
    public String lastSeen;
    public int hitCount;
    public String vendor;

    public String deviceType;
    public float distanceM;
    public boolean isConnectable;
    public boolean isBeacon;
    public short signalMin;
    public short signalMax;
    public float signalAvg;
    public String rssiHistoryJson;
    public String servicesResolved;

    public Device(
            String id, String name, short rssi,
            Map<Integer, byte[]> manufacturerData, List<String> services,
            String firstSeen, String lastSeen, int hitCount, String vendor,
            String deviceType, float distanceM, boolean isConnectable, boolean isBeacon,
            short signalMin, short signalMax, float signalAvg,
            String rssiHistoryJson, String servicesResolved) {
        this.id = id;
        this.name = name;
        this.rssi = rssi;
        this.manufacturerData = manufacturerData;
        this.services = services;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.hitCount = hitCount;
        this.vendor = vendor;
        this.deviceType = deviceType;
        this.distanceM = distanceM;
        this.isConnectable = isConnectable;
        this.isBeacon = isBeacon;
        this.signalMin = signalMin;
        this.signalMax = signalMax;
        this.signalAvg = signalAvg;
        this.rssiHistoryJson = rssiHistoryJson;
        this.servicesResolved = servicesResolved;
    }

    public int signalQuality() {
        int clamped = Math.max(-100, Math.min(-40, rssi));
        return (clamped + 100) * 100 / 60;
    }

    public String distanceLabel() {
        if (distanceM < 0) return "?";
        if (distanceM < 1.0f) return String.format("%.1f m", distanceM);
        if (distanceM < 10.0f) return String.format("%.1f m", distanceM);
        return String.format("%.0f m", distanceM);
    }

    public int[] parsedRssiHistory() {
        if (rssiHistoryJson == null || rssiHistoryJson.length() < 2) return new int[0];
        try {
            String inner = rssiHistoryJson.substring(1, rssiHistoryJson.length() - 1).trim();
            if (inner.isEmpty()) return new int[0];
            String[] parts = inner.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            return new int[0];
        }
    }
}
