package com.example.portshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
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
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * USB Device Inspection Screen with Live Auto-Detection.
 */
public class UsbInspectActivity extends AppCompatActivity {

    private UsbMonitor usbMonitor;
    private DatabaseHelper dbHelper;
    private List<UsbMonitor.UsbDeviceInfo> lastScannedDevices;

    private TextView tvUsbHostSupport;
    private TextView tvUsbScanTimestamp;
    private TextView tvUsbDevicesOutput;
    private Button btnRequestUsbPermission;

    // 1. Permission Receiver
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbMonitor.ACTION_USB_PERMISSION.equals(intent.getAction())) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String name = device != null ? device.getDeviceName() : "USB device";
                if (granted) {
                    Toast.makeText(context, "USB permission granted for " + name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "USB permission denied for " + name, Toast.LENGTH_LONG).show();
                }
                performUsbScan(false);
            }
        }
    };

    // 2. LIVE Auto-Detection Receiver (Plugging / Unplugging cable or accessory)
    private final BroadcastReceiver liveUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            triggerHapticFeedback();
            performUsbScan(false);

            if (Intent.ACTION_POWER_CONNECTED.equals(action) || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(context, "USB Plug Detected • Rescanned", Toast.LENGTH_SHORT).show();
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(context, "USB Unplug Detected • Rescanned", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_inspect);

        usbMonitor = new UsbMonitor(this);
        dbHelper = new DatabaseHelper(this);

        tvUsbHostSupport = findViewById(R.id.tvUsbHostSupport);
        tvUsbScanTimestamp = findViewById(R.id.tvUsbScanTimestamp);
        tvUsbDevicesOutput = findViewById(R.id.tvUsbDevicesOutput);
        Button btnScanUsbDevices = findViewById(R.id.btnScanUsbDevices);
        btnRequestUsbPermission = findViewById(R.id.btnRequestUsbPermission);
        Button btnEvaluateObservedUsb = findViewById(R.id.btnEvaluateObservedUsb);
        Button btnUsbBackDashboard = findViewById(R.id.btnUsbBackDashboard);

        // Register permission receiver
        IntentFilter permFilter = new IntentFilter(UsbMonitor.ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, permFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbPermissionReceiver, permFilter);
        }

        btnScanUsbDevices.setOnClickListener(v -> performUsbScan(true));

        btnRequestUsbPermission.setOnClickListener(v -> {
            if (lastScannedDevices != null && !lastScannedDevices.isEmpty()) {
                UsbDevice target = lastScannedDevices.get(0).rawDevice;
                usbMonitor.requestDevicePermission(target);
            }
        });

        btnEvaluateObservedUsb.setOnClickListener(v -> evaluateAndOpenObservedAssessment());
        btnUsbBackDashboard.setOnClickListener(v -> finish());

        performUsbScan(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register Live USB Plug / Unplug Auto-Detection
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbPermissionReceiver);
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

    private void performUsbScan(boolean showToast) {
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        tvUsbScanTimestamp.setText("Observation Timestamp: " + timeStr);

        boolean supported = usbMonitor.isUsbHostSupported();
        if (supported) {
            tvUsbHostSupport.setText("USB Host Mode: SUPPORTED (Android UsbManager Ready)");
            tvUsbHostSupport.setTextColor(ContextCompat.getColor(this, R.color.risk_low_green));
        } else {
            tvUsbHostSupport.setText("USB Host Mode: LIMITED (Host OTG unsupported on this hardware)");
            tvUsbHostSupport.setTextColor(ContextCompat.getColor(this, R.color.risk_caution_amber));
        }

        UsbMonitor.UsbConnectionState state = usbMonitor.getDetailedConnectionState();
        lastScannedDevices = state.hostDevices;

        StringBuilder sb = new StringBuilder();

        if (state.isCompletelyDisconnected()) {
            btnRequestUsbPermission.setVisibility(View.GONE);
            sb.append("========================================\n");
            sb.append("PHYSICAL USB STATUS: DISCONNECTED\n");
            sb.append("========================================\n\n");
            sb.append("• Power Source: Operating on internal battery (VBUS: 0V)\n");
            sb.append("• USB Data Lines: Dormant / Idle\n");
            sb.append("• Downstream OTG Devices: 0 attached\n");
            sb.append("• Upstream Host PC: Not connected\n\n");
            sb.append("No USB cable or accessory is physically plugged into this device.\n");
            sb.append("Tap 'Evaluate Observed USB State' below to record a clean baseline (0/100).");

            tvUsbDevicesOutput.setText(sb.toString());
            if (showToast) {
                Toast.makeText(this, "Scan complete: No USB connection detected (0/100).", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!state.hostDevices.isEmpty()) {
            sb.append("========================================\n");
            sb.append("DOWNSTREAM USB HOST (OTG) PERIPHERALS\n");
            sb.append(String.format(Locale.US, "Found %d accessible device(s)\n", state.hostDevices.size()));
            sb.append("========================================\n\n");

            boolean needsPerm = false;
            for (int i = 0; i < state.hostDevices.size(); i++) {
                UsbMonitor.UsbDeviceInfo info = state.hostDevices.get(i);
                if (!info.hasPermission) needsPerm = true;

                if (i > 0) sb.append("\n----------------------------------------\n\n");
                sb.append("Device #").append(i + 1).append(": ").append(info.deviceName).append("\n");
                sb.append("Product/Manufacturer: ").append(info.productLabel).append("\n");
                sb.append(String.format(Locale.US, "Vendor ID (VID):      0x%04X (%d)\n", info.vendorId, info.vendorId));
                sb.append(String.format(Locale.US, "Product ID (PID):     0x%04X (%d)\n", info.productId, info.productId));
                sb.append("Device Class:         ").append(info.deviceClassName).append("\n");
                sb.append("Interface Count:      ").append(info.interfaceCount).append("\n");
                sb.append("Interface Classes:    ").append(info.interfaceSummary).append("\n");
                sb.append("USB Permission:       ").append(info.hasPermission ? "GRANTED" : "NOT GRANTED").append("\n");
            }
            btnRequestUsbPermission.setVisibility(needsPerm ? View.VISIBLE : View.GONE);
            tvUsbDevicesOutput.setText(sb.toString());
            if (showToast) {
                Toast.makeText(this, "Scan complete: Found " + state.hostDevices.size() + " OTG device(s)!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        btnRequestUsbPermission.setVisibility(View.GONE);
        if (state.isPcConnected) {
            sb.append("========================================\n");
            sb.append("UPSTREAM CONNECTION: HOST PC DETECTED\n");
            sb.append("========================================\n\n");
            sb.append("• Mode:               Phone operating as USB Peripheral\n");
            sb.append("• VBUS Power:         Active (BATTERY_PLUGGED_USB)\n");
            sb.append("• USB Data Lines:     ").append(state.isConfigured ? "CONFIGURED (Host data negotiation active)" : "LOCKED (No data configured)").append("\n");
            sb.append("• ADB Debugging:      ").append(state.isAdbActive ? "ACTIVE (High Concern: Developer shell open)" : "DISABLED").append("\n");
            sb.append("• File Transfer (MTP):").append(state.isMtpActive ? "ACTIVE (Filesystem exposed)" : "Inactive").append("\n");
            sb.append("• PTP Camera Mode:    ").append(state.isPtpActive ? "ACTIVE" : "Inactive").append("\n\n");
            sb.append("Tap 'Evaluate Observed USB State' below to assess risk for this PC connection.");

            tvUsbDevicesOutput.setText(sb.toString());
            if (showToast) {
                Toast.makeText(this, "Scan complete: Connected to Host PC via Type-C.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (state.isAcPlugged) {
            sb.append("========================================\n");
            sb.append("POWER SOURCE: AC WALL CHARGER\n");
            sb.append("========================================\n\n");
            sb.append("• Power Source: Dedicated AC Adapter / Power Bank (BATTERY_PLUGGED_AC)\n");
            sb.append("• USB Data Lines: Inactive (No host communication)\n");
            sb.append("• Downstream OTG Devices: 0 attached\n\n");
            sb.append("Device is charging safely without host data exposure (5/100).");

            tvUsbDevicesOutput.setText(sb.toString());
            if (showToast) {
                Toast.makeText(this, "Scan complete: AC Wall Charger detected.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        tvUsbDevicesOutput.setText("Observing USB state...");
    }

    private void evaluateAndOpenObservedAssessment() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        boolean chargingOnlyIntent = prefs.getBoolean(MainActivity.PREF_CHARGING_ONLY, true);

        UsbMonitor.UsbConnectionState state = usbMonitor.getDetailedConnectionState();
        SecurityEvent observedEvent = RiskEngine.evaluateObservedUsbState(
                usbMonitor.isUsbHostSupported(),
                state,
                chargingOnlyIntent
        );
        long savedId = dbHelper.insertEvent(observedEvent);

        Intent intent = new Intent(UsbInspectActivity.this, AssessmentActivity.class);
        intent.putExtra(AssessmentActivity.EXTRA_EVENT_ID, savedId);
        intent.putExtra(AssessmentActivity.EXTRA_ALREADY_SAVED, savedId != -1);
        startActivity(intent);
    }
}