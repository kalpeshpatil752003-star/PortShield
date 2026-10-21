package com.example.portshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Locale;

/**
 * Settings Screen (Section 4F).
 * Provides demonstration mode indicator toggle, notification toggle,
 * charging-only baseline toggle, clear event history, and Lock Console / Log Out.
 */
public class SettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvDbRecordCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        SwitchCompat switchNotifications = findViewById(R.id.switchNotifications);
        SwitchCompat switchChargingOnlyIntent = findViewById(R.id.switchChargingOnlyIntent);
        tvDbRecordCount = findViewById(R.id.tvDbRecordCount);
        Button btnClearAllHistory = findViewById(R.id.btnClearAllHistory);
        Button btnSettingsBack = findViewById(R.id.btnSettingsBack);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        switchNotifications.setChecked(prefs.getBoolean(MainActivity.PREF_NOTIFICATIONS, true));
        switchChargingOnlyIntent.setChecked(prefs.getBoolean(MainActivity.PREF_CHARGING_ONLY, true));

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.PREF_NOTIFICATIONS, isChecked).apply()
        );

        switchChargingOnlyIntent.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.PREF_CHARGING_ONLY, isChecked).apply()
        );

        btnClearAllHistory.setOnClickListener(v -> confirmAndClearHistory());
        btnSettingsBack.setOnClickListener(v -> finish());

        refreshRecordCount();
    }

    private void refreshRecordCount() {
        int count = dbHelper.getAllEventsNewestFirst().size();
        tvDbRecordCount.setText(String.format(Locale.US, "Stored security events in SQLite: %d record(s)", count));
    }

    private void confirmAndClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Event History?")
                .setMessage("This will permanently delete all saved security assessment records from the local SQLite database. Continue?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    int deletedCount = dbHelper.clearAllEvents();
                    refreshRecordCount();
                    Toast.makeText(
                            this,
                            "Cleared " + deletedCount + " record(s) from SQLite.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}