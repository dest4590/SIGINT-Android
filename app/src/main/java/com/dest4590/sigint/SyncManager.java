package com.dest4590.sigint;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dest4590.sigint.sniffer.Device;
import com.dest4590.sigint.sniffer.Sniffer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final OkHttpClient httpClient;
    private String lastSyncTimestamp = null;

    public SyncManager(Context context, OkHttpClient httpClient) {
        this.context = context;
        this.httpClient = httpClient;
    }

    public void syncWithBackend(String baseUrl, String apiKey, Button syncButton, SyncCallback callback) {
        if (baseUrl.isEmpty()) {
            Toast.makeText(context, "Please enter a backend URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Device[] devices = Sniffer.scan();
        if (devices == null || devices.length == 0) {
            Toast.makeText(context, "No devices to sync", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray findings = new JSONArray();
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            Log.d(TAG, "Preparing to sync " + devices.length + " devices");

            for (Device d : devices) {
                JSONObject f = new JSONObject();
                f.put("id", d.id);
                f.put("name", d.name != null ? d.name : "Unknown");
                f.put("rssi", d.rssi);

                JSONObject manufacturerData = new JSONObject();
                if (d.manufacturerData != null) {
                    for (Map.Entry<Integer, byte[]> entry : d.manufacturerData.entrySet()) {
                        StringBuilder hex = new StringBuilder();
                        for (byte b : entry.getValue()) hex.append(String.format("%02X", b));
                        manufacturerData.put(entry.getKey().toString(), hex.toString());
                    }
                }
                f.put("manufacturer_data", manufacturerData);

                JSONArray services = new JSONArray();
                if (d.services != null) {
                    for (String s : d.services) services.put(s);
                }
                f.put("services", services);

                f.put("first_seen", d.firstSeen);
                f.put("last_seen", d.lastSeen);
                f.put("hit_count", d.hitCount);
                f.put("device_type", d.deviceType);
                f.put("distance_m", d.distanceM);
                f.put("is_connectable", d.isConnectable);

                JSONArray rssiHistory = new JSONArray();
                if (d.rssiHistoryJson != null) {
                    JSONArray history = new JSONArray(d.rssiHistoryJson);
                    for (int i = 0; i < history.length(); i++) rssiHistory.put(history.getInt(i));
                }
                f.put("rssi_history", rssiHistory);

                f.put("signal_min", d.signalMin);
                f.put("signal_max", d.signalMax);
                f.put("signal_avg", d.signalAvg);
                f.put("address_type", "random");

                JSONArray servicesResolved = new JSONArray();
                if (d.servicesResolved != null) {
                    for (String s : d.servicesResolved.split("\\|")) {
                        if (!s.isEmpty()) servicesResolved.put(s.trim());
                    }
                }
                f.put("services_resolved", servicesResolved);
                f.put("source_device_id", androidId);

                findings.put(f);
            }

            String url = baseUrl + "/sync";
            if (lastSyncTimestamp != null) {
                url += "?since=" + lastSyncTimestamp;
            }

            RequestBody body = RequestBody.create(
                    findings.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            if (!apiKey.isEmpty()) {
                requestBuilder.addHeader("X-API-Key", apiKey);
            }

            Request request = requestBuilder.build();

            Log.d(TAG, "Syncing to: " + url);
            Log.d(TAG, "Payload: " + findings);

            if (syncButton != null) {
                syncButton.setEnabled(false);
                syncButton.setText("SYNCING...");
            }

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Sync network failure", e);
                    if (callback != null)
                        callback.onSyncFinished(false, "Sync failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseData = response.body().string();
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Body: " + responseData);
                    try {
                        if (response.isSuccessful()) {
                            JSONObject res = new JSONObject(responseData);
                            int count = res.getInt("synced_count");
                            lastSyncTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
                            if (callback != null)
                                callback.onSyncFinished(true, "Synced " + count + " devices");
                        } else {
                            if (callback != null)
                                callback.onSyncFinished(false, "Server error: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Sync error", e);
                        if (callback != null)
                            callback.onSyncFinished(false, "Sync error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Sync preparation error", e);
            if (callback != null)
                callback.onSyncFinished(false, "Error preparing sync: " + e.getMessage());
        }
    }

    public interface SyncCallback {
        void onSyncFinished(boolean success, String message);
    }
}