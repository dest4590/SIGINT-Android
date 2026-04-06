package com.dest4590.sigint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;
import android.widget.Toast;

public class ServiceManager {
    public static boolean areServicesEnabled(Context context, LogCallback logCallback) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_LONG).show();
            if (logCallback != null) logCallback.appendLog("Bluetooth is disabled.");
            return false;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(context, "Location services are disabled. Please enable them.", Toast.LENGTH_LONG).show();
            if (logCallback != null) logCallback.appendLog("Location services are disabled.");
            return false;
        }
        return true;
    }

    public interface LogCallback {
        void appendLog(String message);
    }
}