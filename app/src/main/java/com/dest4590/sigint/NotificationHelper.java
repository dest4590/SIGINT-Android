package com.dest4590.sigint;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    public static final String CHANNEL_ID = "scanner_status_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_PAUSE_SCAN = "com.dest4590.sigint.ACTION_PAUSE_SCAN";

    public static void createNotificationChannel(Context context) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Scanner Status", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("SIGINT active scan notification.");
        context.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    public static void showScannerNotification(Context context, String text) {
        Intent pi = new Intent(ACTION_PAUSE_SCAN);
        pi.setPackage(context.getPackageName());
        PendingIntent ppi = PendingIntent.getBroadcast(context.getApplicationContext(), 0, pi,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SIGINT")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, "Pause/Resume", ppi);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, b.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void updateScannerNotification(Context context, String text) {
        if (PermissionManager.hasNotificationPermission(context)) {
            showScannerNotification(context, text);
        }
    }

    public static void cancelScannerNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }
}