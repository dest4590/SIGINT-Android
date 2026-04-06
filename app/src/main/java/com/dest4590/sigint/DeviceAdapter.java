package com.dest4590.sigint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dest4590.sigint.sniffer.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private final List<Device> devices;

    public DeviceAdapter(List<Device> devices) {
        this.devices = devices;
    }

    private static int rssiColor(Context ctx, short rssi) {
        if (rssi >= -60) return ContextCompat.getColor(ctx, R.color.rssi_good);
        if (rssi >= -80) return ContextCompat.getColor(ctx, R.color.rssi_medium);
        return ContextCompat.getColor(ctx, R.color.rssi_bad);
    }

    private static int badgeColor(Context ctx, String type) {
        switch (type) {
            case "PHONE":
                return ContextCompat.getColor(ctx, R.color.badge_phone);
            case "HEADPHONES":
                return ContextCompat.getColor(ctx, R.color.badge_headphones);
            case "SPEAKER":
                return ContextCompat.getColor(ctx, R.color.badge_speaker);
            case "WATCH":
                return ContextCompat.getColor(ctx, R.color.badge_watch);
            case "BEACON":
                return ContextCompat.getColor(ctx, R.color.badge_beacon);
            case "LAPTOP":
                return ContextCompat.getColor(ctx, R.color.badge_laptop);
            case "IOT":
                return ContextCompat.getColor(ctx, R.color.badge_iot);
            case "CAR":
                return ContextCompat.getColor(ctx, R.color.badge_car);
            case "MEDICAL":
                return ContextCompat.getColor(ctx, R.color.badge_medical);
            case "FITNESS":
                return ContextCompat.getColor(ctx, R.color.badge_fitness);
            default:
                return ContextCompat.getColor(ctx, R.color.badge_unknown);
        }
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_card, parent, false);
        return new DeviceViewHolder(v);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(DeviceViewHolder h, int position) {
        Device d = devices.get(position);
        Context ctx = h.itemView.getContext();

        String name = (d.name != null && !d.name.isEmpty()) ? d.name : "HIDDEN";
        h.nameView.setText(name);

        h.idView.setText(d.id != null ? d.id : "—");

        String vendor = (d.vendor != null && !d.vendor.isEmpty()) ? d.vendor : "UNKNOWN";
        h.vendorView.setText(vendor);

        String type = (d.deviceType != null && !d.deviceType.isEmpty()) ? d.deviceType : "UNKNOWN";
        h.typeBadge.setText(type);
        h.typeBadge.setBackgroundColor(badgeColor(ctx, type));
        h.typeBadge.setTextColor(Color.BLACK);

        h.rssiView.setText(String.format("RSSI: %d dBm", d.rssi));
        int rssiColor = rssiColor(ctx, d.rssi);
        h.rssiView.setTextColor(rssiColor);

        int quality = d.signalQuality();
        h.signalBar.setProgress(quality);
        h.signalBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(rssiColor));

        h.distanceView.setText("~" + d.distanceLabel());

        h.connectableView.setTextColor(d.isConnectable
                ? ContextCompat.getColor(ctx, R.color.rssi_good)
                : ContextCompat.getColor(ctx, R.color.rssi_bad));
        h.connectableView.setText(d.isConnectable ? "●" : "○");

        h.signalMinView.setText(String.format("MIN:%d", d.signalMin));
        h.signalAvgView.setText(String.format("AVG:%.0f", d.signalAvg));
        h.signalMaxView.setText(String.format("MAX:%d", d.signalMax));

        h.hitCountView.setText(String.format("HITS: %d", d.hitCount));
        h.firstSeenView.setText("1st:" + (d.firstSeen != null ? d.firstSeen : "—"));
        h.lastSeenView.setText("Last:" + (d.lastSeen != null ? d.lastSeen : "—"));

        String svc = d.servicesResolved;
        if (svc != null && !svc.isEmpty()) {
            String display = svc.replace("|", " · ");
            h.servicesView.setText(display);
            h.servicesView.setVisibility(View.VISIBLE);
        } else {
            h.servicesView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView, idView, vendorView, typeBadge;
        final TextView rssiView, distanceView, connectableView;
        final ProgressBar signalBar;
        final TextView signalMinView, signalAvgView, signalMaxView;
        final TextView hitCountView, firstSeenView, lastSeenView;
        final TextView servicesView;

        public DeviceViewHolder(View v) {
            super(v);
            nameView = v.findViewById(R.id.device_name);
            idView = v.findViewById(R.id.device_id);
            vendorView = v.findViewById(R.id.device_vendor);
            typeBadge = v.findViewById(R.id.device_type_badge);
            rssiView = v.findViewById(R.id.device_rssi);
            distanceView = v.findViewById(R.id.device_distance);
            connectableView = v.findViewById(R.id.device_connectable);
            signalBar = v.findViewById(R.id.device_signal_bar);
            signalMinView = v.findViewById(R.id.device_signal_min);
            signalAvgView = v.findViewById(R.id.device_signal_avg);
            signalMaxView = v.findViewById(R.id.device_signal_max);
            hitCountView = v.findViewById(R.id.device_hit_count);
            firstSeenView = v.findViewById(R.id.device_first_seen);
            lastSeenView = v.findViewById(R.id.device_last_seen);
            servicesView = v.findViewById(R.id.device_services);
        }
    }
}

