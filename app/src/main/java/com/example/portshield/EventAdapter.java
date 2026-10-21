package com.example.portshield;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying SecurityEvent records in HistoryActivity.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(SecurityEvent event);
    }

    private final Context context;
    private final OnEventClickListener listener;
    private List<SecurityEvent> events = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public EventAdapter(Context context, OnEventClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setEvents(List<SecurityEvent> newEvents) {
        this.events = newEvents != null ? newEvents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_security_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        SecurityEvent event = events.get(position);
        holder.tvItemEventType.setText(event.getEventType());
        holder.tvItemSourceBadge.setText(event.getEventSource());

        int sourceColor = SecurityEvent.SOURCE_OBSERVED.equals(event.getEventSource())
                ? R.color.badge_observed_teal
                : R.color.badge_simulated_purple;
        holder.tvItemSourceBadge.setTextColor(ContextCompat.getColor(context, sourceColor));

        holder.tvItemScore.setText(String.format(Locale.US, "Score: %d/100", event.getRiskScore()));
        int riskColor = MainActivity.getRiskColorRes(event.getRiskScore());
        holder.tvItemScore.setTextColor(ContextCompat.getColor(context, riskColor));

        holder.tvItemCategory.setText(event.getRiskLevel());
        holder.tvItemReviewStatus.setText(event.getReviewStatus());

        int reviewColor = SecurityEvent.REVIEW_REVIEWED.equals(event.getReviewStatus())
                ? R.color.risk_low_green
                : R.color.risk_caution_amber;
        holder.tvItemReviewStatus.setTextColor(ContextCompat.getColor(context, reviewColor));

        String formattedDate = dateFormat.format(new Date(event.getTimestamp()));
        String notePreview = (event.getNotes() != null && !event.getNotes().trim().isEmpty())
                ? " • Note: " + event.getNotes().trim()
                : "";
        holder.tvItemTimestamp.setText("Recorded: " + formattedDate + notePreview);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvItemEventType;
        final TextView tvItemSourceBadge;
        final TextView tvItemScore;
        final TextView tvItemCategory;
        final TextView tvItemReviewStatus;
        final TextView tvItemTimestamp;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemEventType = itemView.findViewById(R.id.tvItemEventType);
            tvItemSourceBadge = itemView.findViewById(R.id.tvItemSourceBadge);
            tvItemScore = itemView.findViewById(R.id.tvItemScore);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemReviewStatus = itemView.findViewById(R.id.tvItemReviewStatus);
            tvItemTimestamp = itemView.findViewById(R.id.tvItemTimestamp);
        }
    }
}