package com.example.portshield;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Event Detail Screen (Section 4E).
 * Displays the complete event record (with immutable score, evidence, and source),
 * and allows the user to update review_status and notes (SQLite UPDATE) or
 * delete the record after confirmation (SQLite DELETE).
 */
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private DatabaseHelper dbHelper;
    private SecurityEvent currentEvent;

    private TextView tvDetailSourceBanner;
    private TextView tvDetailEventIdAndTime;
    private TextView tvDetailEventType;
    private TextView tvDetailScoreAndLevel;
    private TextView tvDetailDescription;
    private TextView tvDetailEvidence;
    private CheckBox cbMarkReviewed;
    private EditText etEventNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        dbHelper = new DatabaseHelper(this);

        tvDetailSourceBanner = findViewById(R.id.tvDetailSourceBanner);
        tvDetailEventIdAndTime = findViewById(R.id.tvDetailEventIdAndTime);
        tvDetailEventType = findViewById(R.id.tvDetailEventType);
        tvDetailScoreAndLevel = findViewById(R.id.tvDetailScoreAndLevel);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailEvidence = findViewById(R.id.tvDetailEvidence);
        cbMarkReviewed = findViewById(R.id.cbMarkReviewed);
        etEventNotes = findViewById(R.id.etEventNotes);

        Button btnUpdateEvent = findViewById(R.id.btnUpdateEvent);
        Button btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        Button btnDetailBack = findViewById(R.id.btnDetailBack);

        long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);
        currentEvent = dbHelper.getEventById(eventId);

        if (currentEvent == null) {
            Toast.makeText(this, "Event record not found in SQLite.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateEventDetails(currentEvent);

        btnUpdateEvent.setOnClickListener(v -> saveReviewAndNotes());
        btnDeleteEvent.setOnClickListener(v -> confirmAndDeleteEvent());
        btnDetailBack.setOnClickListener(v -> finish());
    }

    private void populateEventDetails(SecurityEvent event) {
        tvDetailSourceBanner.setText(
                "SOURCE: " + event.getEventSource() + " (Score & Evidence Are Read-Only)"
        );
        int sourceColor = SecurityEvent.SOURCE_OBSERVED.equals(event.getEventSource())
                ? R.color.badge_observed_teal
                : R.color.badge_simulated_purple;
        tvDetailSourceBanner.setTextColor(ContextCompat.getColor(this, sourceColor));

        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date(event.getTimestamp()));
        tvDetailEventIdAndTime.setText("Record #" + event.getId() + " • Recorded: " + formattedTime);
        tvDetailEventType.setText(event.getEventType());

        tvDetailScoreAndLevel.setText(String.format(
                Locale.US, "%d / 100 — %s", event.getRiskScore(), event.getRiskLevel()
        ));
        int riskColor = MainActivity.getRiskColorRes(event.getRiskScore());
        tvDetailScoreAndLevel.setTextColor(ContextCompat.getColor(this, riskColor));

        tvDetailDescription.setText(event.getDescription());

        StringBuilder evBuilder = new StringBuilder();
        List<String> items = event.getEvidenceList();
        for (String item : items) {
            evBuilder.append("• ").append(item).append("\n");
        }
        evBuilder.append("\nConfidence: ").append(event.getConfidenceIndicator());
        evBuilder.append("\n\nRecommended Action:\n").append(event.getRecommendedAction());
        tvDetailEvidence.setText(evBuilder.toString());

        cbMarkReviewed.setChecked(SecurityEvent.REVIEW_REVIEWED.equals(event.getReviewStatus()));
        etEventNotes.setText(event.getNotes());
    }

    private void saveReviewAndNotes() {
        String newStatus = cbMarkReviewed.isChecked()
                ? SecurityEvent.REVIEW_REVIEWED
                : SecurityEvent.REVIEW_PENDING;
        String newNotes = etEventNotes.getText() != null
                ? etEventNotes.getText().toString().trim()
                : "";

        boolean updated = dbHelper.updateReviewAndNotes(currentEvent.getId(), newStatus, newNotes);
        if (updated) {
            currentEvent.setReviewStatus(newStatus);
            currentEvent.setNotes(newNotes);
            Toast.makeText(this, "Updated review status and notes in SQLite.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update record in SQLite.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmAndDeleteEvent() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Security Event?")
                .setMessage("Are you sure you want to permanently delete Record #" + currentEvent.getId() + " from the local SQLite database?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteEventById(currentEvent.getId());
                    if (deleted) {
                        Toast.makeText(this, "Record deleted from SQLite.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error deleting record.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}