package com.dest4590.sigint;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dest4590.sigint.sniffer.Database;
import com.dest4590.sigint.sniffer.Device;
import com.dest4590.sigint.sniffer.Sniffer;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements ScanManager.ScanCallback, SyncManager.SyncCallback, ServiceManager.LogCallback {
    private static final String[] SORT_OPTIONS = {
            "Sort: RSSI ↓", "Sort: RSSI ↑", "Sort: Name A-Z",
            "Sort: Hits ↓", "Sort: First Seen", "Sort: Distance ↑"
    };
    private static final String[] INTERVAL_OPTIONS = {"1", "5", "10", "30", "60"};
    private static final String[] RSSI_FILTER_OPTIONS = {"All", "-90", "-80", "-70", "-60"};
    private final Map<String, Device> deviceMap = new LinkedHashMap<>();
    private final List<Device> displayList = new ArrayList<>();
    private final List<String> logLines = new LinkedList<>();
    private final OkHttpClient httpClient = new OkHttpClient();
    private TextView emptyText, statsView, statusView, statsDetailText;
    private RecyclerView deviceRecyclerView, logRecyclerView;
    private LinearLayout devicePage, logPage;
    private View statsPage;
    private Button scanButton, pauseButton, syncButton;
    private EditText searchInput, backendUrlInput, apiKeyInput;
    private android.widget.CheckBox syncOnStartCheckbox, autosyncCheckbox, keepScreenOnCheckbox;
    private Spinner sortSpinner, autosyncIntervalSpinner, rssiFilterSpinner;
    private DeviceAdapter deviceAdapter;
    private LogAdapter logAdapter;
    private SyncManager syncManager;
    private ScanManager scanManager;

    private BroadcastReceiver notificationActionReceiver;
    private int currentSort = 0;
    private String currentSearch = "";
    private int currentRssiFilter = -100;

    private static String truncate(String s) {
        return s.length() <= 20 ? s : s.substring(0, 20 - 1) + "…";
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_main);

        emptyText = findViewById(R.id.empty_text);
        statsView = findViewById(R.id.stats_text);
        statusView = findViewById(R.id.status_text);
        deviceRecyclerView = findViewById(R.id.device_list);
        logRecyclerView = findViewById(R.id.log_list);
        devicePage = findViewById(R.id.device_page);
        logPage = findViewById(R.id.log_page);
        statsPage = findViewById(R.id.stats_page);
        statsDetailText = findViewById(R.id.stats_detail_text);
        scanButton = findViewById(R.id.scan_button);
        pauseButton = findViewById(R.id.pause_button);
        syncButton = findViewById(R.id.sync_button);
        searchInput = findViewById(R.id.search_input);
        sortSpinner = findViewById(R.id.sort_spinner);
        backendUrlInput = findViewById(R.id.backend_url_input);
        apiKeyInput = findViewById(R.id.api_key_input);
        syncOnStartCheckbox = findViewById(R.id.sync_on_start_checkbox);
        autosyncCheckbox = findViewById(R.id.autosync_checkbox);
        keepScreenOnCheckbox = findViewById(R.id.keep_screen_on_checkbox);
        autosyncIntervalSpinner = findViewById(R.id.autosync_interval_spinner);
        rssiFilterSpinner = findViewById(R.id.rssi_filter_spinner);

        backendUrlInput.setText(getPreferences(MODE_PRIVATE).getString("backend_url", ""));
        apiKeyInput.setText(getPreferences(MODE_PRIVATE).getString("api_key", ""));
        syncOnStartCheckbox.setChecked(getPreferences(MODE_PRIVATE).getBoolean("sync_on_start", false));
        autosyncCheckbox.setChecked(getPreferences(MODE_PRIVATE).getBoolean("autosync", false));
        keepScreenOnCheckbox.setChecked(getPreferences(MODE_PRIVATE).getBoolean("keep_screen_on", false));

        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, INTERVAL_OPTIONS);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autosyncIntervalSpinner.setAdapter(intervalAdapter);
        int savedIntervalPos = getPreferences(MODE_PRIVATE).getInt("autosync_interval_pos", 0);
        autosyncIntervalSpinner.setSelection(savedIntervalPos);

        syncManager = new SyncManager(this, httpClient);
        scanManager = new ScanManager(this);

        ArrayAdapter<String> rssiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, RSSI_FILTER_OPTIONS);
        rssiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rssiFilterSpinner.setAdapter(rssiAdapter);
        int savedRssiPos = getPreferences(MODE_PRIVATE).getInt("rssi_filter_pos", 0);
        rssiFilterSpinner.setSelection(savedRssiPos);
        updateRssiFilter(savedRssiPos);

        rssiFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateRssiFilter(pos);
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });

        keepScreenOnCheckbox.setOnCheckedChangeListener((v, checked) -> {
            if (checked)
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        if (keepScreenOnCheckbox.isChecked())
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Devices"));
        tabLayout.addTab(tabLayout.newTab().setText("Settings"));
        tabLayout.addTab(tabLayout.newTab().setText("Logs"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                devicePage.setVisibility(View.GONE);
                statsPage.setVisibility(View.GONE);
                logPage.setVisibility(View.GONE);
                switch (tab.getPosition()) {
                    case 0:
                        devicePage.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        statsPage.setVisibility(View.VISIBLE);
                        refreshStatsPage();
                        break;
                    case 2:
                        logPage.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(displayList);
        deviceRecyclerView.setAdapter(deviceAdapter);

        logRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(logLines);
        logRecyclerView.setAdapter(logAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SORT_OPTIONS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentSort = pos;
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().toLowerCase(Locale.getDefault()).trim();
                applyFilterAndSort();
            }
        });

        updateButtonState();

        try {
            Sniffer.init(getApplicationContext());
            appendLog("Sniffer initialized.");
        } catch (UnsatisfiedLinkError e) {
            emptyText.setText("Error: libsigint.so not found");
            scanButton.setEnabled(false);
            appendLog("Init failed: " + e.getMessage());
        }

        scanButton.setOnClickListener(v -> {
            if (scanManager.getScanState() == ScanState.IDLE) {
                if (PermissionManager.checkPermissions(this)) {
                    if (ServiceManager.areServicesEnabled(this, this)) startScan();
                } else {
                    PermissionManager.requestPermissions(this);
                }
            } else {
                stopScan();
            }
        });

        pauseButton.setOnClickListener(v -> {
            if (scanManager.getScanState() == ScanState.SCANNING) pauseScan();
            else if (scanManager.getScanState() == ScanState.PAUSED) resumeScan();
        });

        syncButton.setOnClickListener(v -> syncWithBackend());

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(v -> clearAll());

        Button exportDbButton = findViewById(R.id.export_db_button);
        exportDbButton.setOnClickListener(v -> ExportHelper.exportDb(this));

        NotificationHelper.createNotificationChannel(this);
        setupNotificationReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        NotificationHelper.cancelScannerNotification(this);
        if (notificationActionReceiver != null) unregisterReceiver(notificationActionReceiver);
    }


    @SuppressLint("SetTextI18n")
    private void updateButtonState() {
        runOnUiThread(() -> {
            switch (scanManager.getScanState()) {
                case IDLE:
                    scanButton.setText("START SCAN");
                    pauseButton.setVisibility(View.GONE);
                    statusView.setText("STATUS: IDLE");
                    break;
                case SCANNING:
                    scanButton.setText("STOP SCAN");
                    pauseButton.setVisibility(View.VISIBLE);
                    pauseButton.setText("PAUSE");
                    break;
                case PAUSED:
                    scanButton.setText("STOP SCAN");
                    pauseButton.setVisibility(View.VISIBLE);
                    pauseButton.setText("RESUME");
                    statusView.setText("STATUS: PAUSED");
                    break;
            }
        });
    }

    private void updateRssiFilter(int pos) {
        switch (pos) {
            case 1:
                currentRssiFilter = -90;
                break;
            case 2:
                currentRssiFilter = -80;
                break;
            case 3:
                currentRssiFilter = -70;
                break;
            case 4:
                currentRssiFilter = -60;
                break;
            default:
                currentRssiFilter = -100;
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        scanManager.startScan();
        updateButtonState();
        appendLog("Scan started.");
        NotificationHelper.updateScannerNotification(this, "Scanning...");

        if (syncOnStartCheckbox.isChecked()) {
            syncWithBackend();
            scanManager.setLastAutosyncTime(System.currentTimeMillis());
        } else {
            scanManager.setLastAutosyncTime(0);
        }
    }

    private void pauseScan() {
        scanManager.pauseScan();
        updateButtonState();
        appendLog("Scan paused.");
        NotificationHelper.updateScannerNotification(this, "Paused — " + deviceMap.size() + " devices");
    }

    private void resumeScan() {
        scanManager.resumeScan();
        updateButtonState();
        appendLog("Scan resumed.");
    }

    private void stopScan() {
        if (scanManager.getScanState() == ScanState.IDLE) return;
        scanManager.stopScan();
        updateButtonState();
        appendLog("Scan stopped. " + deviceMap.size() + " devices total.");
        NotificationHelper.cancelScannerNotification(this);
    }

    @Override
    public void onDevicesFound(Device[] devices) {
        for (Device d : devices) deviceMap.put(d.id, d);
        applyFilterAndSort();
        updateTopStats();
        if (scanManager.getScanState() == ScanState.SCANNING) {
            NotificationHelper.updateScannerNotification(this, deviceMap.size() + " devices tracked");
        }
    }

    @Override
    public void onScanStatusUpdate(String status) {
        statusView.setText(status);
    }

    @Override
    public boolean isAutosyncEnabled() {
        return autosyncCheckbox.isChecked();
    }

    @Override
    public int getAutosyncIntervalMin() {
        return Integer.parseInt(INTERVAL_OPTIONS[autosyncIntervalSpinner.getSelectedItemPosition()]);
    }

    @Override
    public void onAutosyncTriggered() {
        syncWithBackend();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSyncFinished(boolean success, String message) {
        runOnUiThread(() -> {
            syncButton.setEnabled(true);
            syncButton.setText("SYNC");
            appendLog("Sync: " + message);
            Toast.makeText(this, message, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_CODE) {
            if (PermissionManager.checkPermissions(this)) {
                if (ServiceManager.areServicesEnabled(this, this)) startScan();
            } else {
                emptyText.setText("Permissions denied.");
                emptyText.setVisibility(View.VISIBLE);
                appendLog("Permissions denied.");
            }
        }
    }

    private void setupNotificationReceiver() {
        notificationActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NotificationHelper.ACTION_PAUSE_SCAN.equals(intent != null ? intent.getAction() : null)) {
                    if (scanManager.getScanState() == ScanState.SCANNING) pauseScan();
                    else if (scanManager.getScanState() == ScanState.PAUSED) resumeScan();
                }
            }
        };
        ContextCompat.registerReceiver(getApplicationContext(), notificationActionReceiver,
                new IntentFilter(NotificationHelper.ACTION_PAUSE_SCAN), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void appendLog(String message) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        runOnUiThread(() -> {
            logLines.add(0, ts + " » " + message);
            if (logLines.size() > 300) logLines.remove(logLines.size() - 1);
            logAdapter.notifyDataSetChanged();
        });
    }

    private void syncWithBackend() {
        String baseUrl = backendUrlInput.getText().toString().trim();
        String apiKey = apiKeyInput.getText().toString().trim();

        getPreferences(MODE_PRIVATE).edit()
                .putString("backend_url", baseUrl)
                .putString("api_key", apiKey)
                .putBoolean("sync_on_start", syncOnStartCheckbox.isChecked())
                .putBoolean("autosync", autosyncCheckbox.isChecked())
                .putBoolean("keep_screen_on", keepScreenOnCheckbox.isChecked())
                .putInt("autosync_interval_pos", autosyncIntervalSpinner.getSelectedItemPosition())
                .putInt("rssi_filter_pos", rssiFilterSpinner.getSelectedItemPosition())
                .apply();

        syncManager.syncWithBackend(baseUrl, apiKey, syncButton, this);
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void clearAll() {
        stopScan();
        Sniffer.clear();
        Database.clear();
        deviceMap.clear();
        displayList.clear();
        deviceAdapter.notifyDataSetChanged();
        emptyText.setText("Cleared. Start scan to discover devices.");
        emptyText.setVisibility(View.VISIBLE);
        deviceRecyclerView.setVisibility(View.GONE);
        statsView.setText("TOTAL: 0 devices");
        appendLog("Device list cleared.");
    }


    @SuppressLint("NotifyDataSetChanged")
    private void applyFilterAndSort() {
        List<Device> filtered = new ArrayList<>();
        for (Device d : deviceMap.values()) {
            if (matchesSearch(d) && d.rssi >= currentRssiFilter) filtered.add(d);
        }

        Comparator<Device> cmp;
        switch (currentSort) {
            case 1:
                cmp = Comparator.comparingInt(d -> d.rssi);
                break;
            case 2:
                cmp = Comparator.comparing(d -> (d.name != null ? d.name : ""));
                break;
            case 3:
                cmp = (a, b) -> Integer.compare(b.hitCount, a.hitCount);
                break;
            case 4:
                cmp = Comparator.comparing(d -> (d.firstSeen != null ? d.firstSeen : ""));
                break;
            case 5:
                cmp = Comparator.comparingDouble(d -> d.distanceM < 0 ? 9999 : d.distanceM);
                break;
            default:
                cmp = (a, b) -> Short.compare(b.rssi, a.rssi);
                break;
        }
        filtered.sort(cmp);

        displayList.clear();
        displayList.addAll(filtered);
        deviceAdapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            String msg = deviceMap.isEmpty() ? "Waiting for scan..." : "No devices match filter.";
            emptyText.setText(msg);
            emptyText.setVisibility(View.VISIBLE);
            deviceRecyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesSearch(Device d) {
        if (currentSearch.isEmpty()) return true;
        String name = d.name != null ? d.name.toLowerCase(Locale.getDefault()) : "";
        String id = d.id != null ? d.id.toLowerCase(Locale.getDefault()) : "";
        String vendor = d.vendor != null ? d.vendor.toLowerCase(Locale.getDefault()) : "";
        String type = d.deviceType != null ? d.deviceType.toLowerCase(Locale.getDefault()) : "";
        return name.contains(currentSearch) || id.contains(currentSearch)
                || vendor.contains(currentSearch) || type.contains(currentSearch);
    }

    @SuppressLint("SetTextI18n")
    private void updateTopStats() {
        int total = deviceMap.size();
        if (total == 0) {
            statsView.setText("TOTAL: 0 devices");
            return;
        }

        short strongest = Short.MIN_VALUE;
        String strongestName = "None";
        int beacons = 0;
        for (Device d : deviceMap.values()) {
            if (d.rssi > strongest) {
                strongest = d.rssi;
                strongestName = (d.name != null && !d.name.isEmpty()) ? d.name : d.id;
            }
            if (d.isBeacon) beacons++;
        }
        statsView.setText(String.format(Locale.getDefault(),
                "TOTAL: %d  |  BEACONS: %d  |  STRONGEST: %s (%ddBm)",
                total, beacons, strongestName, strongest));
    }

    @SuppressLint("SetTextI18n")
    private void refreshStatsPage() {
        try {
            String json = Database.getSavedStatsJson();
            JSONObject obj = new JSONObject(json);
            int total = obj.optInt("total", 0);
            int beacons = obj.optInt("beacons", 0);
            int connectable = obj.optInt("connectable", 0);
            int avgRssi = obj.optInt("avgRssi", 0);

            StringBuilder sb = new StringBuilder();
            sb.append("Total devices    : ").append(total).append('\n');
            sb.append("Beacons          : ").append(beacons).append('\n');
            sb.append("Connectable      : ").append(connectable).append('\n');
            sb.append("Avg RSSI         : ").append(avgRssi).append(" dBm\n\n");

            sb.append("─── Device Types ───────────\n");
            JSONObject types = obj.optJSONObject("types");
            if (types != null) {
                java.util.Iterator<String> keys = types.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    sb.append(String.format(Locale.getDefault(), "  %-14s %d\n", k, types.optInt(k)));
                }
            }

            sb.append("\n─── Top Vendors ────────────\n");
            JSONObject vendors = obj.optJSONObject("vendors");
            if (vendors != null) {
                List<Map.Entry<String, Integer>> entries = new ArrayList<>();
                java.util.Iterator<String> vk = vendors.keys();
                while (vk.hasNext()) {
                    String k = vk.next();
                    entries.add(new java.util.AbstractMap.SimpleEntry<>(k, vendors.optInt(k)));
                }
                entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                for (int i = 0; i < Math.min(10, entries.size()); i++) {
                    Map.Entry<String, Integer> e = entries.get(i);
                    sb.append(String.format(Locale.getDefault(), "  %-20s %d\n",
                            truncate(e.getKey()), e.getValue()));
                }
            }

            statsDetailText.setText(sb.toString());
        } catch (Exception e) {
            statsDetailText.setText("Stats unavailable.");
        }
    }
}

