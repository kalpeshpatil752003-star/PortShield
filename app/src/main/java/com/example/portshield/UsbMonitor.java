package com.example.portshield;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper around Android's USB Host API and Peripheral USB State.
 * Inspects both downstream Host OTG accessories (UsbManager) and upstream
 * PC/charger connections (BatteryManager + USB_STATE) without stale cached values.
 */
public class UsbMonitor {

    private static final String TAG = "PortShieldUsbMonitor";
    public static final String ACTION_USB_PERMISSION = "com.example.portshield.USB_PERMISSION";

    private final Context context;
    private final UsbManager usbManager;

    public UsbMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
    }

    public boolean isUsbHostSupported() {
        PackageManager pm = context.getPackageManager();
        return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST) && usbManager != null;
    }

    /**
     * Reads synchronous real-time connection telemetry across battery power,
     * USB peripheral state (PC host lines), and downstream OTG accessories.
     */
    public UsbConnectionState getDetailedConnectionState() {
        long now = System.currentTimeMillis();

        // 1. Inspect live battery charging status
        Intent batteryIntent = null;
        try {
            batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            Log.w(TAG, "Failed to read battery intent", e);
        }

        int plugged = (batteryIntent != null) ? batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : 0;
        boolean isUsbPlugged = (plugged == BatteryManager.BATTERY_PLUGGED_USB);
        boolean isAcPlugged = (plugged == BatteryManager.BATTERY_PLUGGED_AC);
        boolean isPlugged = (plugged > 0);

        // 2. Inspect sticky USB_STATE broadcast ONLY if actually drawing power
        // Prevents stale cached broadcasts from claiming a PC connection while unplugged
        Intent usbIntent = null;
        try {
            usbIntent = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        } catch (Exception e) {
            Log.w(TAG, "Failed to read USB_STATE intent", e);
        }

        boolean usbStateConnected = false;
        boolean configured = false;
        boolean adb = false;
        boolean mtp = false;
        boolean ptp = false;
        boolean unlocked = false;

        if (isPlugged && usbIntent != null) {
            usbStateConnected = usbIntent.getBooleanExtra("connected", false);
            configured = usbIntent.getBooleanExtra("configured", false);
            adb = usbIntent.getBooleanExtra("adb", false);
            mtp = usbIntent.getBooleanExtra("mtp", false);
            ptp = usbIntent.getBooleanExtra("ptp", false);
            unlocked = usbIntent.getBooleanExtra("unlocked", false);
        }

        boolean isPcConnected = isPlugged && (isUsbPlugged || (usbStateConnected && !isAcPlugged));

        // 3. Inspect downstream USB Host OTG accessories
        List<UsbDeviceInfo> hostDevices = scanAccessibleDevices();

        return new UsbConnectionState(
                isPlugged,
                isUsbPlugged,
                isAcPlugged,
                isPcConnected,
                configured,
                adb,
                mtp,
                ptp,
                unlocked,
                hostDevices,
                now
        );
    }

    public List<UsbDeviceInfo> scanAccessibleDevices() {
        List<UsbDeviceInfo> results = new ArrayList<>();
        if (!isUsbHostSupported() || usbManager == null) {
            return results;
        }

        try {
            HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
            if (deviceMap == null || deviceMap.isEmpty()) {
                return results;
            }

            long scanTime = System.currentTimeMillis();
            for (UsbDevice device : deviceMap.values()) {
                if (device == null) continue;

                boolean hasPerm = false;
                try {
                    hasPerm = usbManager.hasPermission(device);
                } catch (Exception e) {
                    Log.w(TAG, "Could not query USB permission", e);
                }

                int vendorId = device.getVendorId();
                int productId = device.getProductId();
                int deviceClass = device.getDeviceClass();
                String deviceClassName = mapUsbClassName(deviceClass);
                int interfaceCount = device.getInterfaceCount();

                List<Integer> interfaceClasses = new ArrayList<>();
                StringBuilder ifaceBuilder = new StringBuilder();

                for (int i = 0; i < interfaceCount; i++) {
                    try {
                        UsbInterface usbInterface = device.getInterface(i);
                        int iClass = usbInterface.getInterfaceClass();
                        int iSubClass = usbInterface.getInterfaceSubclass();
                        int iProtocol = usbInterface.getInterfaceProtocol();
                        interfaceClasses.add(iClass);

                        if (ifaceBuilder.length() > 0) {
                            ifaceBuilder.append("; ");
                        }
                        ifaceBuilder.append(String.format(
                                Locale.US,
                                "Ifc#%d: %s (0x%02X, sub:0x%02X, proto:0x%02X)",
                                i, mapUsbClassName(iClass), iClass, iSubClass, iProtocol
                        ));
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading interface", e);
                    }
                }

                String ifaceSummary = ifaceBuilder.length() > 0
                        ? ifaceBuilder.toString()
                        : "No interfaces declared in descriptor";

                String productLabel = "Restricted until USB permission is granted";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        String pName = device.getProductName();
                        String mName = device.getManufacturerName();
                        if (pName != null || mName != null) {
                            productLabel = ((mName != null ? mName + " " : "") + (pName != null ? pName : "")).trim();
                        }
                    } catch (SecurityException ignored) {
                    }
                }

                results.add(new UsbDeviceInfo(
                        device,
                        device.getDeviceName(),
                        productLabel,
                        vendorId,
                        productId,
                        deviceClass,
                        deviceClassName,
                        interfaceCount,
                        interfaceClasses,
                        ifaceSummary,
                        hasPerm,
                        scanTime
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception enumerating USB host devices", e);
        }

        return results;
    }

    public void requestDevicePermission(UsbDevice device) {
        if (usbManager == null || device == null) return;
        try {
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(context.getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            usbManager.requestPermission(device, permissionIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to request USB permission", e);
        }
    }

    public static String mapUsbClassName(int usbClass) {
        switch (usbClass) {
            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "Per-Interface Defined (0x00)";
            case UsbConstants.USB_CLASS_AUDIO:
                return "Audio (0x01)";
            case UsbConstants.USB_CLASS_COMM:
                return "Communications / CDC (0x02)";
            case UsbConstants.USB_CLASS_HID:
                return "HID - Human Interface Device (0x03)";
            case UsbConstants.USB_CLASS_PHYSICA:
                return "Physical (0x05)";
            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "Still Image / PTP (0x06)";
            case UsbConstants.USB_CLASS_PRINTER:
                return "Printer (0x07)";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "Mass Storage (0x08)";
            case UsbConstants.USB_CLASS_HUB:
                return "USB Hub (0x09)";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "CDC Data (0x0A)";
            case UsbConstants.USB_CLASS_CSCID:
                return "Smart Card / CSCID (0x0B)";
            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "Content Security (0x0D)";
            case UsbConstants.USB_CLASS_VIDEO:
                return "Video (0x0E)";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "Wireless Controller (0xE0)";
            case UsbConstants.USB_CLASS_MISC:
                return "Miscellaneous (0xEF)";
            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application Specific (0xFE)";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor Specific (0xFF)";
            default:
                return String.format(Locale.US, "Unknown/Other Class (0x%02X)", usbClass);
        }
    }

    public static class UsbConnectionState {
        public final boolean isPlugged;
        public final boolean isUsbPlugged;
        public final boolean isAcPlugged;
        public final boolean isPcConnected;
        public final boolean isConfigured;
        public final boolean isAdbActive;
        public final boolean isMtpActive;
        public final boolean isPtpActive;
        public final boolean isDataUnlocked;
        public final List<UsbDeviceInfo> hostDevices;
        public final long timestamp;

        public UsbConnectionState(
                boolean isPlugged,
                boolean isUsbPlugged,
                boolean isAcPlugged,
                boolean isPcConnected,
                boolean isConfigured,
                boolean isAdbActive,
                boolean isMtpActive,
                boolean isPtpActive,
                boolean isDataUnlocked,
                List<UsbDeviceInfo> hostDevices,
                long timestamp) {
            this.isPlugged = isPlugged;
            this.isUsbPlugged = isUsbPlugged;
            this.isAcPlugged = isAcPlugged;
            this.isPcConnected = isPcConnected;
            this.isConfigured = isConfigured;
            this.isAdbActive = isAdbActive;
            this.isMtpActive = isMtpActive;
            this.isPtpActive = isPtpActive;
            this.isDataUnlocked = isDataUnlocked;
            this.hostDevices = hostDevices != null ? hostDevices : new ArrayList<>();
            this.timestamp = timestamp;
        }

        public boolean isCompletelyDisconnected() {
            return !isPlugged && hostDevices.isEmpty();
        }
    }

    public static class UsbDeviceInfo {
        public final UsbDevice rawDevice;
        public final String deviceName;
        public final String productLabel;
        public final int vendorId;
        public final int productId;
        public final int deviceClass;
        public final String deviceClassName;
        public final int interfaceCount;
        public final List<Integer> interfaceClasses;
        public final String interfaceSummary;
        public final boolean hasPermission;
        public final long timestamp;

        public UsbDeviceInfo(
                UsbDevice rawDevice,
                String deviceName,
                String productLabel,
                int vendorId,
                int productId,
                int deviceClass,
                String deviceClassName,
                int interfaceCount,
                List<Integer> interfaceClasses,
                String interfaceSummary,
                boolean hasPermission,
                long timestamp) {
            this.rawDevice = rawDevice;
            this.deviceName = deviceName;
            this.productLabel = productLabel;
            this.vendorId = vendorId;
            this.productId = productId;
            this.deviceClass = deviceClass;
            this.deviceClassName = deviceClassName;
            this.interfaceCount = interfaceCount;
            this.interfaceClasses = interfaceClasses;
            this.interfaceSummary = interfaceSummary;
            this.hasPermission = hasPermission;
            this.timestamp = timestamp;
        }
    }
}