package com.example.portshield;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Security Event History Screen with Forensic CSV Export.
 */
public class HistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvEventHistory;
    private TextView tvEmptyHistory;
    private EventAdapter adapter;
    private Button btnExportHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        rvEventHistory = findViewById(R.id.rvEventHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        btnExportHistory = findViewById(R.id.btnExportHistory);
        Button btnHistoryBack = findViewById(R.id.btnHistoryBack);

        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this, event -> {
            Intent intent = new Intent(HistoryActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        rvEventHistory.setAdapter(adapter);

        btnExportHistory.setOnClickListener(v -> exportForensicLogToCsv());
        btnHistoryBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryRecords();
    }

    private void loadHistoryRecords() {
        List<SecurityEvent> events = dbHelper.getAllEventsNewestFirst();
        if (events.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvEventHistory.setVisibility(View.GONE);
            btnExportHistory.setEnabled(false);
            btnExportHistory.setAlpha(0.5f);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvEventHistory.setVisibility(View.VISIBLE);
            btnExportHistory.setEnabled(true);
            btnExportHistory.setAlpha(1.0f);
            adapter.setEvents(events);
        }
    }

    /**
     * Compiles all SQLite security event records into an RFC 4180 CSV,
     * writes it to local app storage, and opens the system Share Sheet.
     */
    private void exportForensicLogToCsv() {
        List<SecurityEvent> events = dbHelper.getAllEventsNewestFirst();
        if (events.isEmpty()) {
            Toast.makeText(this, "No records in SQLite database to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("Record_ID,Timestamp_Unix,Date_Time,Event_Type,Event_Source,Risk_Score,Risk_Level,Review_Status,Description,Evidence_Items,Recommended_Action,Confidence,Analyst_Notes\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        for (SecurityEvent ev : events) {
            String dateFormatted = sdf.format(new Date(ev.getTimestamp()));
            String evidenceJoined = String.join(" | ", ev.getEvidenceList());

            csv.append(ev.getId()).append(",");
            csv.append(ev.getTimestamp()).append(",");
            csv.append(escapeCsv(dateFormatted)).append(",");
            csv.append(escapeCsv(ev.getEventType())).append(",");
            csv.append(escapeCsv(ev.getEventSource())).append(",");
            csv.append(ev.getRiskScore()).append(",");
            csv.append(escapeCsv(ev.getRiskLevel())).append(",");
            csv.append(escapeCsv(ev.getReviewStatus())).append(",");
            csv.append(escapeCsv(ev.getDescription())).append(",");
            csv.append(escapeCsv(evidenceJoined)).append(",");
            csv.append(escapeCsv(ev.getRecommendedAction())).append(",");
            csv.append(escapeCsv(ev.getConfidenceIndicator())).append(",");
            csv.append(escapeCsv(ev.getNotes())).append("\n");
        }

        String fileName = "portshield_forensic_log_" + System.currentTimeMillis() + ".csv";
        File exportDir = getExternalFilesDir(null);
        if (exportDir == null) exportDir = getFilesDir();
        File csvFile = new File(exportDir, fileName);

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(csv.toString());
            writer.flush();

            // Share / Export intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PortShield Forensic Security Log Export (" + events.size() + " records)");
            shareIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());

            startActivity(Intent.createChooser(shareIntent, "Export PortShield Forensic Log"));
            Toast.makeText(this, "Forensic log ready! (" + events.size() + " records saved to " + fileName + ")", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to write CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\"";
    }
}