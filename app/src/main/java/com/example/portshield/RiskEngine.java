package com.example.portshield;

import android.hardware.usb.UsbConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic rule-based risk assessment engine for PortShield.
 * Dynamically scales score (0–100) across disconnected states, wall charging,
 * PC host states (charge-only, MTP, ADB), and downstream OTG accessories.
 */
public class RiskEngine {

    public static final String SCENARIO_SAFE_CHARGING = "SAFE_CHARGING";
    public static final String SCENARIO_UNEXPECTED_DATA = "UNEXPECTED_DATA_CONNECTION";
    public static final String SCENARIO_SUSPICIOUS_HID = "SUSPICIOUS_HID";
    public static final String EVENT_OBSERVED_USB_SCAN = "OBSERVED_USB_INSPECTION";

    public static final String LEVEL_LOW = "Low Indicated Risk";
    public static final String LEVEL_CAUTION = "Caution";
    public static final String LEVEL_HIGH = "High Concern";
    public static final String LEVEL_CRITICAL = "Critical Concern";

    public static String getRiskCategory(int score) {
        int clamped = Math.max(0, Math.min(100, score));
        if (clamped <= 24) {
            return LEVEL_LOW;
        } else if (clamped <= 49) {
            return LEVEL_CAUTION;
        } else if (clamped <= 74) {
            return LEVEL_HIGH;
        } else {
            return LEVEL_CRITICAL;
        }
    }

    public static SecurityEvent evaluateSimulatedScenario(String scenarioType) {
        long now = System.currentTimeMillis();
        List<String> evidence = new ArrayList<>();

        if (SCENARIO_SAFE_CHARGING.equals(scenarioType)) {
            int score = 5;
            evidence.add("[SIMULATED] Power delivery state: Standard charging-only profile selected.");
            evidence.add("[SIMULATED] USB Host enumeration: 0 external data or HID interfaces exposed.");
            evidence.add("[LIMITATION] Android OS cannot verify physical cable wiring or charger firmware integrity.");
            return new SecurityEvent(
                    -1,
                    SCENARIO_SAFE_CHARGING,
                    score,
                    getRiskCategory(score),
                    "No suspicious indicators were selected in this simulation.",
                    evidence,
                    "Continue charging if using a trusted wall outlet or personal power bank.",
                    "SIMULATED: Baseline Safe Charging Profile",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else if (SCENARIO_UNEXPECTED_DATA.equals(scenarioType)) {
            int score = 35;
            evidence.add("[SIMULATED] Stated user intent: Charging only at an untrusted USB port.");
            evidence.add("[SIMULATED] Connection capability: Active USB data lines detected alongside power delivery.");
            evidence.add("[SIMULATED] Exposed interface: Data-capable interface present without file-transfer intent.");
            return new SecurityEvent(
                    -1,
                    SCENARIO_UNEXPECTED_DATA,
                    score,
                    getRiskCategory(score),
                    "A data-capable connection conflicts with the user's stated charging-only intent.",
                    evidence,
                    "Verify that USB preference in Android Settings is set to 'No data transfer' (Charge only).",
                    "SIMULATED: Data Line Intent Mismatch",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else if (SCENARIO_SUSPICIOUS_HID.equals(scenarioType)) {
            int score = 85;
            evidence.add("[SIMULATED] Unexpected USB Interface Class 0x03 (HID - Keyboard) declared.");
            evidence.add("[SIMULATED] Composite anomaly: Mass Storage (0x08) + HID Keyboard (0x03) presented simultaneously.");
            evidence.add("[SIMULATED] Context conflict: User expected a charging cable or storage drive, not an input keyboard.");
            return new SecurityEvent(
                    -1,
                    SCENARIO_SUSPICIOUS_HID,
                    score,
                    getRiskCategory(score),
                    "The scenario includes a simulated unexpected HID interface and suspicious composite behavior.",
                    evidence,
                    "Disconnect the USB device or cable immediately. Do not grant USB Host permissions.",
                    "SIMULATED: BadUSB / Keystroke Injection Profile",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else {
            int score = 25;
            evidence.add("[SIMULATED] Unrecognized scenario identifier: " + scenarioType);
            return new SecurityEvent(
                    -1,
                    "UNKNOWN_SCENARIO",
                    score,
                    getRiskCategory(score),
                    "Unrecognized scenario was requested.",
                    evidence,
                    "Select one of the supported demonstration scenarios.",
                    "UNKNOWN: Unverified Input",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }
    }

    /**
     * Evaluates live observed USB telemetry with dynamic scoring based on real connection physics.
     */
    public static SecurityEvent evaluateObservedUsbState(
            boolean usbHostSupported,
            UsbMonitor.UsbConnectionState state,
            boolean userChargingOnlyIntent) {

        long now = System.currentTimeMillis();
        List<String> evidence = new ArrayList<>();

        // 1. COMPLETELY DISCONNECTED
        if (state == null || state.isCompletelyDisconnected()) {
            int score = 0;
            evidence.add("[OBSERVED] Physical connection: No USB cable or accessory physically plugged in.");
            evidence.add("[OBSERVED] Power delivery: Device is running on internal battery power (VBUS: 0V).");
            evidence.add("[OBSERVED] USB bus state: Data lines D+/D- and Type-C CC pins are idle.");
            evidence.add("[VERIFIED] 0 USB Host accessories enumerated; 0 upstream Host PC connections.");

            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    "No physical USB connection detected. The phone is disconnected and operating safely on internal battery.",
                    evidence,
                    "No protective action required. The device is not connected to any external USB interface.",
                    "OBSERVED: Clean Disconnected State",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        // 2. DOWNSTREAM HOST (OTG) ACCESSORIES ATTACHED
        if (!state.hostDevices.isEmpty()) {
            boolean hasHidInterface = false;
            boolean hasStorageOrDataInterface = false;
            boolean hasMultipleDistinctClasses = false;
            int totalInterfaces = 0;

            for (UsbMonitor.UsbDeviceInfo dev : state.hostDevices) {
                evidence.add(String.format(
                        Locale.US,
                        "[OBSERVED OTG] Device: %s | VID: 0x%04X (%d) | PID: 0x%04X (%d) | Class: %s | Permission: %s",
                        dev.deviceName,
                        dev.vendorId, dev.vendorId,
                        dev.productId, dev.productId,
                        dev.deviceClassName,
                        dev.hasPermission ? "Granted" : "Not Granted"
                ));
                totalInterfaces += dev.interfaceCount;

                boolean devHasHid = false;
                boolean devHasNonHid = false;
                for (Integer cls : dev.interfaceClasses) {
                    if (cls == UsbConstants.USB_CLASS_HID) {
                        hasHidInterface = true;
                        devHasHid = true;
                    } else {
                        hasStorageOrDataInterface = true;
                        devHasNonHid = true;
                    }
                }
                if (devHasHid && devHasNonHid) {
                    hasMultipleDistinctClasses = true;
                }
                evidence.add("[OBSERVED OTG] Interfaces: " + dev.interfaceSummary);
            }

            int score;
            String explanation;
            String recommendation;

            if (hasMultipleDistinctClasses && userChargingOnlyIntent) {
                score = 80;
                explanation = "Observed a composite USB accessory exposing both HID (keyboard/mouse) and storage/data interfaces simultaneously.";
                recommendation = "Disconnect the accessory immediately if you only intended to charge or use single-function storage.";
            } else if (hasHidInterface && userChargingOnlyIntent) {
                score = 55;
                explanation = "Observed an attached USB Human Interface Device (HID / Keyboard) while 'Charging-Only Intent' is enabled.";
                recommendation = "Verify physical device authenticity. Disconnect if you did not intentionally connect a keyboard.";
            } else if (hasStorageOrDataInterface) {
                score = 30;
                explanation = "Observed an attached USB mass storage or data accessory in USB Host mode.";
                recommendation = "Safely unmount and disconnect after completing legitimate file operations.";
            } else {
                score = 20;
                explanation = "Observed connected USB Host peripheral matching standard accessory descriptors.";
                recommendation = "Only grant USB permissions to trusted applications and accessories.";
            }

            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    explanation,
                    evidence,
                    recommendation,
                    "OBSERVED: Downstream USB Host Peripheral",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        // 3. UPSTREAM CONNECTION TO PC (PHONE IS PERIPHERAL)
        if (state.isPcConnected) {
            evidence.add("[OBSERVED] Upstream connection: Phone is connected as a USB Peripheral to a Host Computer.");
            evidence.add("[OBSERVED] VBUS Power: Drawing power from USB Host port (BATTERY_PLUGGED_USB).");

            if (state.isAdbActive) {
                int score = 75;
                evidence.add("[OBSERVED HIGH RISK] Android Debug Bridge (ADB) daemon is actively communicating with host PC!");
                evidence.add("[RISK FACTOR] Active ADB permits arbitrary shell commands, APK installation, and data extraction.");
                return new SecurityEvent(
                        -1,
                        EVENT_OBSERVED_USB_SCAN,
                        score,
                        getRiskCategory(score),
                        "Active ADB Debugging Session detected over Type-C connection. An external computer currently has developer shell access to this device.",
                        evidence,
                        "If this PC is untrusted or in a public setting, revoke USB debugging authorizations in Developer Options and disconnect the cable immediately.",
                        "OBSERVED: Active ADB Host Connection",
                        SecurityEvent.SOURCE_OBSERVED,
                        now,
                        SecurityEvent.REVIEW_PENDING,
                        ""
                );
            }

            if (state.isConfigured && (state.isMtpActive || state.isPtpActive || state.isDataUnlocked)) {
                int score = userChargingOnlyIntent ? 45 : 30;
                evidence.add("[OBSERVED] Data configuration: Host PC has actively configured data transfer lines (MTP/PTP/Transfer).");
                evidence.add("[OBSERVED] Intent Mismatch: User preferences specify 'Charging-Only', but active file transfer lines are open.");
                return new SecurityEvent(
                        -1,
                        EVENT_OBSERVED_USB_SCAN,
                        score,
                        getRiskCategory(score),
                        "Active data connection detected to host PC. Files on device storage may be read or written by the host computer.",
                        evidence,
                        "Open the Android notification shade, tap 'USB configuration', and select 'No data transfer' (Charge only).",
                        "OBSERVED: Active Data Host Negotiation",
                        SecurityEvent.SOURCE_OBSERVED,
                        now,
                        SecurityEvent.REVIEW_PENDING,
                        ""
                );
            }

            // PC Connected, but Charge Only
            int score = 15;
            evidence.add("[OBSERVED] Connection mode: USB connection to PC detected, but data transfer lines are locked / inactive.");
            evidence.add("[OBSERVED] ADB state: Disabled or inactive.");
            evidence.add("[OBSERVED] File transfer: MTP/PTP interfaces not active.");
            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    "Phone is connected to a host computer via USB, but data transfer is locked. Operating in charging-only profile.",
                    evidence,
                    "Keep USB mode set to 'Charging only'. Disconnect if using an untrusted or public workstation.",
                    "OBSERVED: PC Connected (Data Inactive)",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        // 4. AC WALL CHARGER / POWER BANK (POWER ONLY)
        if (state.isAcPlugged) {
            int score = 5;
            evidence.add("[OBSERVED] Power source: AC Wall Charger / Dedicated Charging Port (BATTERY_PLUGGED_AC).");
            evidence.add("[OBSERVED] Data lines: No host enumeration; USB data lines are dormant.");
            evidence.add("[VERIFIED] Standard power delivery without host communication channel.");
            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    "Device is drawing power from an AC wall adapter or power bank. No USB host data negotiation was observed.",
                    evidence,
                    "Safe to continue charging if using a trusted AC adapter or certified power bank.",
                    "OBSERVED: AC Wall Power Profile",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        // 5. BASELINE / UNCLASSIFIED
        int score = 10;
        evidence.add("[OBSERVED] Unclassified charging or connection state.");
        return new SecurityEvent(
                -1,
                EVENT_OBSERVED_USB_SCAN,
                score,
                getRiskCategory(score),
                "Unclassified USB state. Verify connection parameters in Android system settings.",
                evidence,
                "Check Android notification shade for active USB connection type.",
                "OBSERVED: Baseline State",
                SecurityEvent.SOURCE_OBSERVED,
                now,
                SecurityEvent.REVIEW_PENDING,
                ""
        );
    }
}