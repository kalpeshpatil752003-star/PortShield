package com.example.portshield;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Home Dashboard Activity for PortShield with Live USB Plug/Unplug Auto-Detection.
 */
public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "PortShieldPrefs";
    public static final String PREF_DEMO_BADGE = "pref_demo_badge";
    public static final String PREF_NOTIFICATIONS = "pref_notifications";
    public static final String PREF_CHARGING_ONLY = "pref_charging_only";
    private static final String CHANNEL_ID = "portshield_security_alerts";

    private TextView tvDemoModeBadge;
    private TextView tvMonitoringStatus;
    private TextView tvConnectionStatus;
    private TextView tvLatestSourceBadge;
    private TextView tvLatestScore;
    private TextView tvLatestCategory;
    private TextView tvLatestEventType;
    private TextView tvLatestSummary;

    private DatabaseHelper dbHelper;
    private UsbMonitor usbMonitor;

    // Live plug/unplug broadcast listener
    private final BroadcastReceiver liveUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            triggerHapticFeedback();
            refreshDashboardState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        usbMonitor = new UsbMonitor(this);
        createNotificationChannel();


        tvMonitoringStatus = findViewById(R.id.tvMonitoringStatus);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvLatestSourceBadge = findViewById(R.id.tvLatestSourceBadge);
        tvLatestScore = findViewById(R.id.tvLatestScore);
        tvLatestCategory = findViewById(R.id.tvLatestCategory);
        tvLatestEventType = findViewById(R.id.tvLatestEventType);
        tvLatestSummary = findViewById(R.id.tvLatestSummary);

        Button btnScenarioSafe = findViewById(R.id.btnScenarioSafe);
        Button btnScenarioData = findViewById(R.id.btnScenarioData);
        Button btnScenarioHid = findViewById(R.id.btnScenarioHid);
        Button btnInspectUsb = findViewById(R.id.btnInspectUsb);
        Button btnOpenHistory = findViewById(R.id.btnOpenHistory);
        Button btnOpenSettings = findViewById(R.id.btnOpenSettings);

        btnScenarioSafe.setOnClickListener(v -> triggerSimulatedScenario(RiskEngine.SCENARIO_SAFE_CHARGING));
        btnScenarioData.setOnClickListener(v -> triggerSimulatedScenario(RiskEngine.SCENARIO_UNEXPECTED_DATA));
        btnScenarioHid.setOnClickListener(v -> triggerSimulatedScenario(RiskEngine.SCENARIO_SUSPICIOUS_HID));

        btnInspectUsb.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UsbInspectActivity.class)));
        btnOpenHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        btnOpenSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardState();

        // Register live receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(liveUsbReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(liveUsbReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(liveUsbReceiver);
        } catch (Exception ignored) {
        }
    }

    private void triggerHapticFeedback() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void refreshDashboardState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean showDemoBadge = prefs.getBoolean(PREF_DEMO_BADGE, true);
        tvDemoModeBadge.setVisibility(showDemoBadge ? View.VISIBLE : View.GONE);

        boolean hostSupported = usbMonitor.isUsbHostSupported();
        if (hostSupported) {
            tvMonitoringStatus.setText("Active • Android USB Host & Peripheral Monitor Ready");
            tvMonitoringStatus.setTextColor(ContextCompat.getColor(this, R.color.risk_low_green));
        } else {
            tvMonitoringStatus.setText("Limited • USB Host Mode Unsupported on This Hardware");
            tvMonitoringStatus.setTextColor(ContextCompat.getColor(this, R.color.risk_caution_amber));
        }

        UsbMonitor.UsbConnectionState state = usbMonitor.getDetailedConnectionState();
        if (state.isCompletelyDisconnected()) {
            tvConnectionStatus.setText("OBSERVED: Disconnected (Running on internal battery • 0 USB devices attached)");
        } else if (state.isPcConnected) {
            String adbTag = state.isAdbActive ? "ADB: ACTIVE" : "ADB: Inactive";
            String dataTag = state.isConfigured ? "Data: Configured" : "Data: Locked";
            tvConnectionStatus.setText("OBSERVED: Connected to Host PC via Type-C (" + adbTag + " • " + dataTag + ")");
        } else if (state.isAcPlugged) {
            tvConnectionStatus.setText("OBSERVED: Connected to AC Wall Charger (Power-only profile)");
        } else if (!state.hostDevices.isEmpty()) {
            tvConnectionStatus.setText(String.format(
                    Locale.US,
                    "OBSERVED: %d USB Host device(s) attached (First: %s, VID:0x%04X, PID:0x%04X)",
                    state.hostDevices.size(),
                    state.hostDevices.get(0).deviceName,
                    state.hostDevices.get(0).vendorId,
                    state.hostDevices.get(0).productId
            ));
        } else {
            tvConnectionStatus.setText("OBSERVED: Cable attached (Negotiating connection...)");
        }

        SecurityEvent latest = dbHelper.getLatestEvent();
        if (latest == null) {
            tvLatestSourceBadge.setText("NO EVENTS YET");
            tvLatestSourceBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLatestScore.setText("-- / 100");
            tvLatestScore.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvLatestCategory.setText("Awaiting Assessment");
            tvLatestCategory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLatestEventType.setText("Select a scenario or scan USB devices");
            tvLatestSummary.setText("No security assessments recorded in SQLite yet. Run a simulated scenario below or inspect USB devices.");
        } else {
            tvLatestSourceBadge.setText("SOURCE: " + latest.getEventSource());
            int badgeColor = SecurityEvent.SOURCE_OBSERVED.equals(latest.getEventSource())
                    ? R.color.badge_observed_teal
                    : R.color.badge_simulated_purple;
            tvLatestSourceBadge.setTextColor(ContextCompat.getColor(this, badgeColor));

            tvLatestScore.setText(String.format(Locale.US, "%d / 100", latest.getRiskScore()));
            int riskColor = getRiskColorRes(latest.getRiskScore());
            tvLatestScore.setTextColor(ContextCompat.getColor(this, riskColor));

            tvLatestCategory.setText(latest.getRiskLevel());
            tvLatestCategory.setTextColor(ContextCompat.getColor(this, riskColor));
            tvLatestEventType.setText("Event: " + latest.getEventType() + " (" + latest.getReviewStatus() + ")");
            tvLatestSummary.setText(latest.getDescription());
        }
    }

    private void triggerSimulatedScenario(String scenarioType) {
        SecurityEvent event = RiskEngine.evaluateSimulatedScenario(scenarioType);
        long insertedId = dbHelper.insertEvent(event);

        if (insertedId != -1) {
            Toast.makeText(this, "Simulated scenario saved to SQLite (Record #" + insertedId + ")", Toast.LENGTH_SHORT).show();
        }

        if (event.getRiskScore() >= 50) {
            maybeSendAlertNotification(event);
        }

        Intent intent = new Intent(MainActivity.this, AssessmentActivity.class);
        intent.putExtra(AssessmentActivity.EXTRA_EVENT_ID, insertedId);
        intent.putExtra(AssessmentActivity.EXTRA_SCENARIO_TYPE, scenarioType);
        intent.putExtra(AssessmentActivity.EXTRA_ALREADY_SAVED, insertedId != -1);
        startActivity(intent);
    }

    public static int getRiskColorRes(int score) {
        if (score <= 24) {
            return R.color.risk_low_green;
        } else if (score <= 49) {
            return R.color.risk_caution_amber;
        } else if (score <= 74) {
            return R.color.risk_high_orange;
        } else {
            return R.color.risk_critical_red;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "PortShield Security Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Alerts for high-concern simulated or observed USB assessments");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void maybeSendAlertNotification(SecurityEvent event) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_NOTIFICATIONS, true)) return;
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_shield)
                    .setContentTitle("PortShield [" + event.getEventSource() + "]: " + event.getRiskLevel())
                    .setContentText(event.getEventType() + " (" + event.getRiskScore() + "/100) — " + event.getDescription())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify((int) (System.currentTimeMillis() & 0xFFFFFFF), builder.build());
            }
        } catch (SecurityException ignored) {
        }
    }
}