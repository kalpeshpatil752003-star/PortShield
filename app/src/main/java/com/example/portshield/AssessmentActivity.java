package com.example.portshield;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

/**
 * Security Assessment Screen (Section 4B).
 * Displays the illustrative risk score (0-100), risk category, scenario/event type,
 * itemized evidence, explanation, recommended action, explicit source label, and save button.
 */
public class AssessmentActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_SCENARIO_TYPE = "extra_scenario_type";
    public static final String EXTRA_ALREADY_SAVED = "extra_already_saved";

    private DatabaseHelper dbHelper;
    private SecurityEvent currentEvent;
    private boolean isSaved;

    private TextView tvAssessmentSourceBanner;
    private TextView tvAssessmentEventType;
    private TextView tvAssessmentScore;
    private TextView tvAssessmentCategory;
    private TextView tvAssessmentConfidence;
    private TextView tvAssessmentExplanation;
    private TextView tvAssessmentEvidence;
    private TextView tvAssessmentRecommendation;
    private TextView tvSaveStatus;
    private Button btnSaveAssessment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        dbHelper = new DatabaseHelper(this);

        tvAssessmentSourceBanner = findViewById(R.id.tvAssessmentSourceBanner);
        tvAssessmentEventType = findViewById(R.id.tvAssessmentEventType);
        tvAssessmentScore = findViewById(R.id.tvAssessmentScore);
        tvAssessmentCategory = findViewById(R.id.tvAssessmentCategory);
        tvAssessmentConfidence = findViewById(R.id.tvAssessmentConfidence);
        tvAssessmentExplanation = findViewById(R.id.tvAssessmentExplanation);
        tvAssessmentEvidence = findViewById(R.id.tvAssessmentEvidence);
        tvAssessmentRecommendation = findViewById(R.id.tvAssessmentRecommendation);
        tvSaveStatus = findViewById(R.id.tvSaveStatus);
        btnSaveAssessment = findViewById(R.id.btnSaveAssessment);
        Button btnAssessmentBack = findViewById(R.id.btnAssessmentBack);

        long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);
        String scenarioType = getIntent().getStringExtra(EXTRA_SCENARIO_TYPE);
        isSaved = getIntent().getBooleanExtra(EXTRA_ALREADY_SAVED, false);

        if (eventId != -1) {
            currentEvent = dbHelper.getEventById(eventId);
        }
        if (currentEvent == null) {
            String fallbackScenario = scenarioType != null ? scenarioType : RiskEngine.SCENARIO_SAFE_CHARGING;
            currentEvent = RiskEngine.evaluateSimulatedScenario(fallbackScenario);
            isSaved = false;
        }

        bindAssessmentToViews(currentEvent);

        btnSaveAssessment.setOnClickListener(v -> {
            if (isSaved) {
                Toast.makeText(this, "Assessment is already saved in SQLite.", Toast.LENGTH_SHORT).show();
                return;
            }
            long id = dbHelper.insertEvent(currentEvent);
            if (id != -1) {
                isSaved = true;
                updateSaveUiState(id);
                Toast.makeText(this, "Saved assessment record #" + id, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error saving assessment to SQLite", Toast.LENGTH_SHORT).show();
            }
        });

        btnAssessmentBack.setOnClickListener(v -> finish());
    }

    private void bindAssessmentToViews(SecurityEvent event) {
        if (SecurityEvent.SOURCE_OBSERVED.equals(event.getEventSource())) {
            tvAssessmentSourceBanner.setText("EVENT SOURCE: ACTUAL USB HOST OBSERVATION (OBSERVED)");
            tvAssessmentSourceBanner.setTextColor(ContextCompat.getColor(this, R.color.badge_observed_teal));
        } else {
            tvAssessmentSourceBanner.setText("EVENT SOURCE: SIMULATED DEMONSTRATION SCENARIO (NOT A REAL ATTACK)");
            tvAssessmentSourceBanner.setTextColor(ContextCompat.getColor(this, R.color.badge_simulated_purple));
        }

        tvAssessmentEventType.setText("SCENARIO / EVENT: " + event.getEventType());
        tvAssessmentScore.setText(String.format(Locale.US, "%d / 100", event.getRiskScore()));

        int riskColor = MainActivity.getRiskColorRes(event.getRiskScore());
        tvAssessmentScore.setTextColor(ContextCompat.getColor(this, riskColor));
        tvAssessmentCategory.setText(event.getRiskLevel());
        tvAssessmentCategory.setTextColor(ContextCompat.getColor(this, riskColor));

        tvAssessmentConfidence.setText("Confidence / Evidence Quality: " + event.getConfidenceIndicator());
        tvAssessmentExplanation.setText(event.getDescription());

        StringBuilder evidenceBuilder = new StringBuilder();
        List<String> items = event.getEvidenceList();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) evidenceBuilder.append("\n\n");
            evidenceBuilder.append("• ").append(items.get(i));
        }
        tvAssessmentEvidence.setText(evidenceBuilder.toString());
        tvAssessmentRecommendation.setText(event.getRecommendedAction());

        if (isSaved && event.getId() != -1) {
            updateSaveUiState(event.getId());
        } else {
            tvSaveStatus.setText("Status: Not yet saved to SQLite history");
            btnSaveAssessment.setEnabled(true);
            btnSaveAssessment.setText(R.string.btn_save_assessment);
        }
    }

    private void updateSaveUiState(long recordId) {
        tvSaveStatus.setText("Saved in local SQLite database (Record #" + recordId + ")");
        btnSaveAssessment.setEnabled(false);
        btnSaveAssessment.setText("Saved to SQLite History (Record #" + recordId + ")");
    }
}