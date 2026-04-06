package com.dest4590.sigint;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private final List<String> logs;

    public LogAdapter(List<String> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView line = new TextView(parent.getContext());
        line.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.white));
        line.setTextSize(12f);
        line.setTypeface(android.graphics.Typeface.MONOSPACE);
        line.setPadding(12, 8, 12, 8);
        return new LogViewHolder(line);
    }

    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        holder.lineView.setText(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView lineView;

        public LogViewHolder(View itemView) {
            super(itemView);
            lineView = (TextView) itemView;
        }
    }
}
