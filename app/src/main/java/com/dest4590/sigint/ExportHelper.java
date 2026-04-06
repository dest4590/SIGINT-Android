package com.dest4590.sigint;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.dest4590.sigint.sniffer.Database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExportHelper {
    private static final String TAG = "ExportHelper";

    public static void exportDb(Context context) {
        try {
            String json = Database.getSavedStatsJson();
            if (json == null || json.isEmpty()) {
                Toast.makeText(context, "No saved DB data available.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = "sigint_db_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) os.write(json.getBytes());
                    }
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists() && !dir.mkdirs()) {
                    Toast.makeText(context, "Unable to create downloads directory.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try (FileOutputStream fos = new FileOutputStream(new File(dir, fileName))) {
                    fos.write(json.getBytes());
                }
            }
            Toast.makeText(context, "DB exported to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Export DB failed", e);
            Toast.makeText(context, "Export DB failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}