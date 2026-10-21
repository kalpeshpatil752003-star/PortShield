# PortShield — Android USB Security Monitor (Complete Java + XML + SQLite Implementation)

**PortShield** is a native Android application built with **Java**, **XML Views**, **SQLite (`SQLiteOpenHelper`)**, and the **Android USB Host API (`UsbManager`)**. It is designed as a clean, honest, one-day working demonstration that inspects accessible USB device descriptors, runs a deterministic rule-based risk assessment engine across simulated and observed USB events, and performs full SQLite CRUD persistence.

## Project Directory Structure

Place every file into the following standard Android Studio project structure under package `com.example.portshield`:

```text
PortShield/
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/portshield/
        │   ├── MainActivity.java
        │   ├── AssessmentActivity.java
        │   ├── UsbInspectActivity.java
        │   ├── UsbMonitor.java
        │   ├── RiskEngine.java
        │   ├── SecurityEvent.java
        │   ├── DatabaseHelper.java
        │   ├── HistoryActivity.java
        │   ├── EventAdapter.java
        │   ├── EventDetailActivity.java
        │   └── SettingsActivity.java
        └── res/
            ├── drawable/
            │   ├── ic_shield.xml
            │   └── bg_status_badge.xml
            ├── layout/
            │   ├── activity_main.xml
            │   ├── activity_assessment.xml
            │   ├── activity_usb_inspect.xml
            │   ├── activity_history.xml
            │   ├── item_security_event.xml
            │   ├── activity_event_detail.xml
            │   └── activity_settings.xml
            └── values/
                ├── colors.xml
                ├── dimens.xml
                ├── strings.xml
                └── themes.xml
```

## Phase 1: Android Studio Project Setup & Configuration

### Step-by-Step Android Studio Setup
1. Open **Android Studio** and select **New Project**.
2. Choose **Empty Views Activity** (do **not** select "Empty Activity" with Jetpack Compose).
3. Configure the project fields:
   - **Name**: `PortShield`
   - **Package name**: `com.example.portshield`
   - **Language**: **Java**
   - **Minimum SDK**: **API 24 (Android 7.0 Nougat)**
   - **Build configuration language**: Groovy DSL (`build.gradle`) or Kotlin DSL (if using `build.gradle.kts`, copy the dependencies block accordingly).
4. Click **Finish** and wait for initial Gradle sync to complete.

### 1.1 `app/build.gradle` (Module: `:app`)
Replace the contents of `app/build.gradle` with the following configuration and click **Sync Now**:

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.portshield'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.portshield"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0-demo"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
}
```

### 1.2 `app/src/main/AndroidManifest.xml`
Declares optional USB Host support (`android:required="false"` so the APK installs cleanly on both physical OTG phones and emulators) and registers all six Activities:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.portshield">

    <!-- Optional USB Host feature: false allows installation on devices/emulators without OTG support -->
    <uses-feature
        android:name="android.hardware.usb.host"
        android:required="false" />

    <!-- Optional permission for Android 13+ local security alert notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PortShield">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".AssessmentActivity"
            android:exported="false"
            android:label="@string/title_assessment"
            android:parentActivityName=".MainActivity" />

        <activity
            android:name=".UsbInspectActivity"
            android:exported="false"
            android:label="@string/title_usb_inspect"
            android:parentActivityName=".MainActivity" />

        <activity
            android:name=".HistoryActivity"
            android:exported="false"
            android:label="@string/title_history"
            android:parentActivityName=".MainActivity" />

        <activity
            android:name=".EventDetailActivity"
            android:exported="false"
            android:label="@string/title_event_detail"
            android:parentActivityName=".HistoryActivity" />

        <activity
            android:name=".SettingsActivity"
            android:exported="false"
            android:label="@string/title_settings"
            android:parentActivityName=".MainActivity" />

    </application>

</manifest>
```

### 1.3 UI Resource Values & Drawables

#### `app/src/main/res/values/colors.xml`
Dark navy cybersecurity color palette with semantic green, amber, orange, and red risk indicators, plus distinct purple (`SIMULATED`) and teal (`OBSERVED`) source badges:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Core Dark Cybersecurity Surfaces -->
    <color name="bg_navy_primary">#0A1124</color>
    <color name="bg_navy_card">#131E3A</color>
    <color name="bg_navy_elevated">#1C2B4F</color>
    <color name="border_subtle">#293B6A</color>

    <!-- Brand & Accent Colors -->
    <color name="accent_cyan">#00D2FF</color>
    <color name="accent_blue">#3B82F6</color>

    <!-- Semantic Risk Indicators -->
    <color name="risk_low_green">#10B981</color>
    <color name="risk_caution_amber">#F59E0B</color>
    <color name="risk_high_orange">#F97316</color>
    <color name="risk_critical_red">#EF4444</color>
    <color name="risk_unknown_gray">#64748B</color>

    <!-- Source & Status Badge Colors -->
    <color name="badge_simulated_purple">#8B5CF6</color>
    <color name="badge_observed_teal">#0EA5E9</color>

    <!-- Typography -->
    <color name="text_primary">#F8FAFC</color>
    <color name="text_secondary">#94A3B8</color>
    <color name="text_muted">#64748B</color>
    <color name="white">#FFFFFF</color>
</resources>
```

#### `app/src/main/res/values/dimens.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="screen_padding">16dp</dimen>
    <dimen name="card_padding">16dp</dimen>
    <dimen name="card_corner_radius">12dp</dimen>
    <dimen name="card_margin_bottom">12dp</dimen>
    <dimen name="button_corner_radius">8dp</dimen>
    <dimen name="text_size_hero_score">36sp</dimen>
    <dimen name="text_size_title">20sp</dimen>
    <dimen name="text_size_section">16sp</dimen>
    <dimen name="text_size_body">14sp</dimen>
    <dimen name="text_size_caption">12sp</dimen>
</resources>
```

#### `app/src/main/res/values/strings.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">PortShield</string>
    <string name="app_subtitle">Android USB Security Monitor &amp; Risk Demonstrator</string>

    <!-- Screen Titles -->
    <string name="title_assessment">Security Assessment</string>
    <string name="title_usb_inspect">USB Device Inspector</string>
    <string name="title_history">Security Event History</string>
    <string name="title_event_detail">Event Record Detail</string>
    <string name="title_settings">Settings &amp; About</string>

    <!-- Disclaimers & Honesty Labels -->
    <string name="disclaimer_banner">Educational &amp; diagnostic tool: Distinguishes real USB Host observations from simulated scenarios. Does not provide hardware-level data blocking or guaranteed BadUSB detection.</string>
    <string name="threshold_disclaimer">Provisional Thresholds (Illustrative Only): 0–24 Low Indicated Risk | 25–49 Caution | 50–74 High Concern | 75–100 Critical Concern</string>

    <!-- Dashboard Labels -->
    <string name="label_monitoring_status">USB Host Monitoring Status</string>
    <string name="label_connection_state">Current Connection State</string>
    <string name="label_latest_assessment">Latest Security Assessment</string>
    <string name="label_simulation_controls">Demonstration Scenario Controls (Simulated)</string>
    <string name="label_navigation_actions">Inspection &amp; Event Management</string>

    <!-- Scenario Buttons -->
    <string name="btn_sim_safe">Simulate: Safe Charging (5/100)</string>
    <string name="btn_sim_data">Simulate: Unexpected Data Connection (35/100)</string>
    <string name="btn_sim_hid">Simulate: Suspicious HID / BadUSB (85/100)</string>

    <!-- Navigation Buttons -->
    <string name="btn_inspect_usb">Inspect Accessible USB Devices</string>
    <string name="btn_view_history">Open Security Event History</string>
    <string name="btn_open_settings">Settings &amp; Demo Preferences</string>
    <string name="btn_scan_usb">Scan Connected USB Devices</string>
    <string name="btn_assess_observed">Evaluate Observed USB State</string>
    <string name="btn_save_assessment">Save Assessment to SQLite</string>
    <string name="btn_back_dashboard">Return to Dashboard</string>
</resources>
```

#### `app/src/main/res/values/themes.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.PortShield" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">@color/bg_navy_card</item>
        <item name="colorPrimaryVariant">@color/bg_navy_primary</item>
        <item name="colorOnPrimary">@color/text_primary</item>
        <item name="colorSecondary">@color/accent_cyan</item>
        <item name="colorOnSecondary">@color/bg_navy_primary</item>
        <item name="android:windowBackground">@color/bg_navy_primary</item>
        <item name="android:statusBarColor">@color/bg_navy_primary</item>
        <item name="android:navigationBarColor">@color/bg_navy_primary</item>
    </style>
</resources>
```

#### `app/src/main/res/drawable/ic_shield.xml`
Create a new Vector Drawable resource file named `ic_shield.xml` in `res/drawable/`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="40dp"
    android:height="40dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#00D2FF"
        android:pathData="M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4zM12,11.99h7c-0.53,4.12 -3.28,7.79 -7,8.94V12H5V6.3l7,-3.11v8.8z" />
</vector>
```

#### `app/src/main/res/drawable/bg_status_badge.xml`
Create a shape drawable named `bg_status_badge.xml` in `res/drawable/`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_navy_elevated" />
    <corners android:radius="6dp" />
    <stroke
        android:width="1dp"
        android:color="@color/border_subtle" />
</shape>
```

## Phase 2: XML Screen Layouts (Dashboard, Assessment, USB Inspector, History, Detail, Settings)

### 2.1 Home Dashboard Layout — `app/src/main/res/layout/activity_main.xml`
Displays the PortShield header, Demo Mode badge, live USB Host monitoring status card, latest risk assessment card, the three simulated scenario buttons, and navigation buttons:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/screen_padding">

        <!-- Header Banner -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:layout_marginBottom="12dp">

            <ImageView
                android:id="@+id/imgShieldLogo"
                android:layout_width="44dp"
                android:layout_height="44dp"
                android:contentDescription="@string/app_name"
                android:src="@drawable/ic_shield" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvAppTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/app_name"
                    android:textColor="@color/text_primary"
                    android:textSize="22sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAppSubtitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/app_subtitle"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvDemoModeBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_status_badge"
                android:paddingHorizontal="8dp"
                android:paddingVertical="4dp"
                android:text="DEMO MODE"
                android:textColor="@color/accent_cyan"
                android:textSize="11sp"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- Card 1: Real USB Host & Connection Status -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/label_monitoring_status"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvMonitoringStatus"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Checking USB Host API support..."
                    android:textColor="@color/text_primary"
                    android:textSize="15sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:text="@string/label_connection_state"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/tvConnectionStatus"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="No USB Host devices currently enumerated (Charging-only or disconnected state cannot be verified via USB Host API alone)"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Card 2: Latest Risk Assessment Summary -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_latest_assessment"
                        android:textColor="@color/accent_cyan"
                        android:textSize="13sp"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/tvLatestSourceBadge"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:background="@drawable/bg_status_badge"
                        android:paddingHorizontal="8dp"
                        android:paddingVertical="3dp"
                        android:text="NO EVENTS YET"
                        android:textColor="@color/text_secondary"
                        android:textSize="11sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/tvLatestScore"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="-- / 100"
                        android:textColor="@color/text_primary"
                        android:textSize="32sp"
                        android:textStyle="bold" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="16dp"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:id="@+id/tvLatestCategory"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Awaiting Assessment"
                            android:textColor="@color/text_secondary"
                            android:textSize="16sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tvLatestEventType"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Select a scenario or scan USB devices"
                            android:textColor="@color/text_secondary"
                            android:textSize="12sp" />
                    </LinearLayout>
                </LinearLayout>

                <TextView
                    android:id="@+id/tvLatestSummary"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:text="No security assessments recorded yet. Run a demonstration scenario below or inspect accessible USB devices."
                    android:textColor="@color/text_primary"
                    android:textSize="13sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Card 3: Demonstration Scenario Controls -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/label_simulation_controls"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:layout_marginBottom="8dp"
                    android:text="All scenarios below generate clearly labelled SIMULATED events for demonstration and automatically save them to SQLite."
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnScenarioSafe"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:backgroundTint="@color/risk_low_green"
                    android:text="@string/btn_sim_safe"
                    android:textAllCaps="false"
                    android:textColor="@color/white" />

                <Button
                    android:id="@+id/btnScenarioData"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:backgroundTint="@color/risk_caution_amber"
                    android:text="@string/btn_sim_data"
                    android:textAllCaps="false"
                    android:textColor="@color/bg_navy_primary" />

                <Button
                    android:id="@+id/btnScenarioHid"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:backgroundTint="@color/risk_critical_red"
                    android:text="@string/btn_sim_hid"
                    android:textAllCaps="false"
                    android:textColor="@color/white" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Card 4: Real USB Inspection & Navigation -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="8dp"
                    android:text="@string/label_navigation_actions"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <Button
                    android:id="@+id/btnInspectUsb"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:backgroundTint="@color/accent_blue"
                    android:text="@string/btn_inspect_usb"
                    android:textAllCaps="false"
                    android:textColor="@color/white" />

                <Button
                    android:id="@+id/btnOpenHistory"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:backgroundTint="@color/bg_navy_elevated"
                    android:text="@string/btn_view_history"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />

                <Button
                    android:id="@+id/btnOpenSettings"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:backgroundTint="@color/bg_navy_elevated"
                    android:text="@string/btn_open_settings"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Honesty Disclaimer Footer -->
        <TextView
            android:id="@+id/tvDisclaimerFooter"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="@string/disclaimer_banner"
            android:textColor="@color/text_muted"
            android:textSize="11sp" />

    </LinearLayout>
</ScrollView>
```

### 2.2 Security Assessment Screen Layout — `app/src/main/res/layout/activity_assessment.xml`
Displays the prominent `SIMULATED` vs. `OBSERVED` source banner, illustrative score (`0–100`), risk category, provisional threshold notice, evidence bullets, explanation, recommended protective action, and SQLite save button:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/screen_padding">

        <!-- Source Banner -->
        <TextView
            android:id="@+id/tvAssessmentSourceBanner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp"
            android:background="@drawable/bg_status_badge"
            android:gravity="center"
            android:padding="10dp"
            android:text="EVENT SOURCE: SIMULATED SCENARIO"
            android:textColor="@color/accent_cyan"
            android:textSize="13sp"
            android:textStyle="bold" />

        <!-- Score & Category Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:id="@+id/tvAssessmentEventType"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="EVENT TYPE"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentScore"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="0 / 100"
                    android:textColor="@color/text_primary"
                    android:textSize="38sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentCategory"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Low Indicated Risk"
                    android:textColor="@color/risk_low_green"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentConfidence"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="Confidence / Evidence Quality: Illustrative rule profile"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/tvAssessmentThresholds"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="@string/threshold_disclaimer"
                    android:textColor="@color/text_muted"
                    android:textSize="11sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Explanation & Evidence Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Why This Event Matters"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentExplanation"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="Explanation details appear here."
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="14dp"
                    android:text="Evidence Evaluated by RiskEngine"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentEvidence"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="• Evidence item"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="14dp"
                    android:text="Recommended Protective Action"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAssessmentRecommendation"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="Recommended action appears here."
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Save Status & Action Buttons -->
        <TextView
            android:id="@+id/tvSaveStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:gravity="center"
            android:textColor="@color/risk_low_green"
            android:textSize="13sp" />

        <Button
            android:id="@+id/btnSaveAssessment"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:backgroundTint="@color/accent_blue"
            android:text="@string/btn_save_assessment"
            android:textAllCaps="false"
            android:textColor="@color/white" />

        <Button
            android:id="@+id/btnAssessmentBack"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:backgroundTint="@color/bg_navy_elevated"
            android:text="@string/btn_back_dashboard"
            android:textAllCaps="false"
            android:textColor="@color/text_primary" />

    </LinearLayout>
</ScrollView>
```

### 2.3 USB Device Inspection Screen Layout — `app/src/main/res/layout/activity_usb_inspect.xml`
Displays USB Host support status, scan timestamp, buttons to scan USB devices, request runtime USB permission, and evaluate the observed USB state:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/screen_padding">

        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Android USB Host API Capability"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvUsbHostSupport"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="Checking USB Host support..."
                    android:textColor="@color/text_primary"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvUsbScanTimestamp"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Last scan: Not yet scanned"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="Note: Standard Android apps can only inspect USB accessories exposed in USB Host (OTG) mode. Wall chargers, power-only cables, or host PCs connected in peripheral mode are not enumerated by UsbManager."
                    android:textColor="@color/text_muted"
                    android:textSize="12sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <Button
            android:id="@+id/btnScanUsbDevices"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:backgroundTint="@color/accent_blue"
            android:text="@string/btn_scan_usb"
            android:textAllCaps="false"
            android:textColor="@color/white" />

        <Button
            android:id="@+id/btnRequestUsbPermission"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:backgroundTint="@color/bg_navy_elevated"
            android:text="Request USB Permission for First Connected Device"
            android:textAllCaps="false"
            android:textColor="@color/text_primary"
            android:visibility="gone" />

        <Button
            android:id="@+id/btnEvaluateObservedUsb"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:backgroundTint="@color/badge_observed_teal"
            android:text="@string/btn_assess_observed"
            android:textAllCaps="false"
            android:textColor="@color/white" />

        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Observed USB Descriptors &amp; Interfaces"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvUsbDevicesOutput"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:fontFamily="monospace"
                    android:text="Tap 'Scan Connected USB Devices' to query Android UsbManager."
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <Button
            android:id="@+id/btnUsbBackDashboard"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:backgroundTint="@color/bg_navy_elevated"
            android:text="@string/btn_back_dashboard"
            android:textAllCaps="false"
            android:textColor="@color/text_primary" />

    </LinearLayout>
</ScrollView>
```

### 2.4 Security Event History Layout — `app/src/main/res/layout/activity_history.xml`
Displays a `RecyclerView` sorted newest first alongside an empty-state view when no records exist:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:orientation="vertical"
    android:padding="@dimen/screen_padding">

    <TextView
        android:id="@+id/tvHistoryHeader"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Saved Security Events (Sorted Newest First)"
        android:textColor="@color/accent_cyan"
        android:textSize="14sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tvHistorySubtitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="2dp"
        android:layout_marginBottom="10dp"
        android:text="Tap any record to view full evidence, mark as reviewed, edit notes, or delete."
        android:textColor="@color/text_secondary"
        android:textSize="12sp" />

    <TextView
        android:id="@+id/tvEmptyHistory"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:gravity="center"
        android:padding="20dp"
        android:text="No security events saved in SQLite yet.\n\nRun a simulated scenario on the Dashboard or evaluate an observed USB scan to populate history."
        android:textColor="@color/text_secondary"
        android:textSize="14sp"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvEventHistory"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <Button
        android:id="@+id/btnHistoryBack"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:backgroundTint="@color/bg_navy_elevated"
        android:text="@string/btn_back_dashboard"
        android:textAllCaps="false"
        android:textColor="@color/text_primary" />

</LinearLayout>
```

### 2.5 History Row Item Layout — `app/src/main/res/layout/item_security_event.xml`
Card layout for each individual `SecurityEvent` row in `HistoryActivity`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="10dp"
    app:cardBackgroundColor="@color/bg_navy_card"
    app:cardCornerRadius="10dp"
    app:cardElevation="3dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="14dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvItemEventType"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="EVENT_TYPE"
                android:textColor="@color/text_primary"
                android:textSize="15sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvItemSourceBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_status_badge"
                android:paddingHorizontal="8dp"
                android:paddingVertical="3dp"
                android:text="SIMULATED"
                android:textColor="@color/accent_cyan"
                android:textSize="11sp"
                android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvItemScore"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Score: 85/100"
                android:textColor="@color/risk_critical_red"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvItemCategory"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:text="Critical Concern"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <TextView
                android:id="@+id/tvItemReviewStatus"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="PENDING"
                android:textColor="@color/risk_caution_amber"
                android:textSize="12sp"
                android:textStyle="bold" />
        </LinearLayout>

        <TextView
            android:id="@+id/tvItemTimestamp"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:text="2026-10-21 13:18:00"
            android:textColor="@color/text_secondary"
            android:textSize="11sp" />

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 2.6 Event Detail Screen Layout — `app/src/main/res/layout/activity_event_detail.xml`
Displays the immutable assessment score, explanation, and evidence at the top, and provides editable controls for marking the record as `REVIEWED`, editing analyst notes (`SQLite UPDATE`), and deleting the record after confirmation (`SQLite DELETE`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/screen_padding">

        <!-- Read-Only Record Integrity Notice -->
        <TextView
            android:id="@+id/tvDetailSourceBanner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp"
            android:background="@drawable/bg_status_badge"
            android:gravity="center"
            android:padding="10dp"
            android:text="SOURCE: SIMULATED (Immutable Score &amp; Evidence)"
            android:textColor="@color/accent_cyan"
            android:textSize="12sp"
            android:textStyle="bold" />

        <!-- Immutable Assessment Record Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:id="@+id/tvDetailEventIdAndTime"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Record #1 • Timestamp"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/tvDetailEventType"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="EVENT_TYPE"
                    android:textColor="@color/text_primary"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDetailScoreAndLevel"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="85 / 100 — Critical Concern"
                    android:textColor="@color/risk_critical_red"
                    android:textSize="20sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:text="Assessment Explanation (Read-Only)"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDetailDescription"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Description"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:text="Recorded Evidence &amp; Recommendation (Read-Only)"
                    android:textColor="@color/accent_cyan"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDetailEvidence"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Evidence items"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Editable Review Status & Analyst Notes Card (SQLite UPDATE) -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Analyst Review &amp; Notes (Editable)"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <CheckBox
                    android:id="@+id/cbMarkReviewed"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:buttonTint="@color/accent_cyan"
                    android:text="Mark this security event as REVIEWED"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="User / Analyst Note:"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp" />

                <EditText
                    android:id="@+id/etEventNotes"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:background="@drawable/bg_status_badge"
                    android:gravity="top|start"
                    android:hint="Add investigation notes or context..."
                    android:inputType="textMultiLine"
                    android:minLines="3"
                    android:padding="10dp"
                    android:textColor="@color/text_primary"
                    android:textColorHint="@color/text_muted"
                    android:textSize="14sp" />

                <Button
                    android:id="@+id/btnUpdateEvent"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:backgroundTint="@color/risk_low_green"
                    android:text="Save Review Status &amp; Note (SQLite UPDATE)"
                    android:textAllCaps="false"
                    android:textColor="@color/white" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <Button
            android:id="@+id/btnDeleteEvent"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:backgroundTint="@color/risk_critical_red"
            android:text="Delete Event Record (SQLite DELETE)"
            android:textAllCaps="false"
            android:textColor="@color/white" />

        <Button
            android:id="@+id/btnDetailBack"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:backgroundTint="@color/bg_navy_elevated"
            android:text="Back to Event History"
            android:textAllCaps="false"
            android:textColor="@color/text_primary" />

    </LinearLayout>
</ScrollView>
```

### 2.7 Settings Screen Layout — `app/src/main/res/layout/activity_settings.xml`
Provides toggles for the Demo Mode badge, high-risk notifications, and charging-only intent assumptions, plus a confirmed "Clear All Event History" button and an architectural honesty statement:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_navy_primary"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/screen_padding">

        <!-- Demo & Monitoring Preferences Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Demonstration &amp; Monitoring Preferences"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <androidx.appcompat.widget.SwitchCompat
                    android:id="@+id/switchDemoIndicator"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:checked="true"
                    android:text="Show 'DEMO MODE' badge on Dashboard"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />

                <androidx.appcompat.widget.SwitchCompat
                    android:id="@+id/switchNotifications"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:checked="true"
                    android:text="Enable High-Risk Security Alert Notifications"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />

                <androidx.appcompat.widget.SwitchCompat
                    android:id="@+id/switchChargingOnlyIntent"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:checked="true"
                    android:text="Assume 'Charging-Only Intent' during USB scans"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Database Management Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Local SQLite Database Management"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDbRecordCount"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="Stored security events: 0 records"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp" />

                <Button
                    android:id="@+id/btnClearAllHistory"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:backgroundTint="@color/risk_critical_red"
                    android:text="Clear All Event History (Confirmation Required)"
                    android:textAllCaps="false"
                    android:textColor="@color/white" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- About & Technical Scope Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/card_margin_bottom"
            app:cardBackgroundColor="@color/bg_navy_card"
            app:cardCornerRadius="@dimen/card_corner_radius"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/card_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="About PortShield &amp; Architectural Honesty"
                    android:textColor="@color/accent_cyan"
                    android:textSize="14sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAboutVersion"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="Version: 1.0.0-demo (Java + XML + SQLite + Android USB Host API)"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="6dp"
                    android:text="PortShield is an educational demonstration app. It inspects USB device descriptors exposed by Android's UsbManager and demonstrates deterministic rule-based risk scoring. It does not intercept raw USB packet frames, block hardware data lines, or detect all BadUSB firmware implants."
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <Button
            android:id="@+id/btnSettingsBack"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:backgroundTint="@color/bg_navy_elevated"
            android:text="@string/btn_back_dashboard"
            android:textAllCaps="false"
            android:textColor="@color/text_primary" />

    </LinearLayout>
</ScrollView>
```

## Phase 3: Domain Model & Deterministic Risk Assessment Engine

### 3.1 `app/src/main/java/com/example/portshield/SecurityEvent.java`
Encapsulates a security assessment record and serializes/deserializes evidence lists, recommended actions, and confidence indicators to and from JSON for SQLite storage:

```java
package com.example.portshield;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable assessment and persistence model representing a security event.
 * Keeps original evidence and calculated score immutable after creation, while
 * allowing review_status and user notes to be updated in SQLite.
 */
public class SecurityEvent {

    public static final String SOURCE_SIMULATED = "SIMULATED";
    public static final String SOURCE_OBSERVED = "OBSERVED";

    public static final String REVIEW_PENDING = "PENDING";
    public static final String REVIEW_REVIEWED = "REVIEWED";

    private long id;
    private final String eventType;
    private final int riskScore;
    private final String riskLevel;
    private final String description;
    private final List<String> evidenceList;
    private final String recommendedAction;
    private final String confidenceIndicator;
    private final String eventSource;
    private final long timestamp;
    private String reviewStatus;
    private String notes;

    public SecurityEvent(
            long id,
            String eventType,
            int riskScore,
            String riskLevel,
            String description,
            List<String> evidenceList,
            String recommendedAction,
            String confidenceIndicator,
            String eventSource,
            long timestamp,
            String reviewStatus,
            String notes) {
        this.id = id;
        this.eventType = eventType != null ? eventType : "UNKNOWN_EVENT";
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel != null ? riskLevel : "Unknown";
        this.description = description != null ? description : "";
        this.evidenceList = evidenceList != null ? new ArrayList<>(evidenceList) : new ArrayList<>();
        this.recommendedAction = recommendedAction != null ? recommendedAction : "";
        this.confidenceIndicator = confidenceIndicator != null ? confidenceIndicator : "Unverified";
        this.eventSource = eventSource != null ? eventSource : SOURCE_SIMULATED;
        this.timestamp = timestamp;
        this.reviewStatus = reviewStatus != null ? reviewStatus : REVIEW_PENDING;
        this.notes = notes != null ? notes : "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEvidenceList() {
        return new ArrayList<>(evidenceList);
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getConfidenceIndicator() {
        return confidenceIndicator;
    }

    public String getEventSource() {
        return eventSource;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Serializes the evidence list, recommended action, and confidence indicator into
     * a structured JSON string for storage in the SQLite evidence_json column.
     */
    public String toEvidenceJsonString() {
        JSONObject root = new JSONObject();
        try {
            JSONArray arr = new JSONArray();
            for (String item : evidenceList) {
                arr.put(item);
            }
            root.put("evidence", arr);
            root.put("recommended_action", recommendedAction);
            root.put("confidence", confidenceIndicator);
        } catch (JSONException e) {
            return "{}";
        }
        return root.toString();
    }

    /**
     * Helper class for unpacking evidence_json from SQLite.
     */
    public static class EvidencePayload {
        public final List<String> evidenceList;
        public final String recommendedAction;
        public final String confidenceIndicator;

        public EvidencePayload(List<String> evidenceList, String recommendedAction, String confidenceIndicator) {
            this.evidenceList = evidenceList;
            this.recommendedAction = recommendedAction;
            this.confidenceIndicator = confidenceIndicator;
        }
    }

    public static EvidencePayload parseEvidenceJson(String jsonString) {
        List<String> list = new ArrayList<>();
        String action = "Review connection state and disconnect untrusted USB accessories if unexpected.";
        String confidence = "Recorded assessment";
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new EvidencePayload(list, action, confidence);
        }
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray arr = root.optJSONArray("evidence");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.optString(i));
                }
            }
            action = root.optString("recommended_action", action);
            confidence = root.optString("confidence", confidence);
        } catch (JSONException e) {
            list.add(jsonString);
        }
        return new EvidencePayload(list, action, confidence);
    }
}

```

### 3.2 `app/src/main/java/com/example/portshield/RiskEngine.java`
Implements deterministic, UI-independent rule evaluation for the three required simulated scenarios (`SAFE_CHARGING` → `5/100`, `UNEXPECTED_DATA_CONNECTION` → `35/100`, `SUSPICIOUS_HID` → `85/100`) as well as real USB Host observations (`OBSERVED_USB_INSPECTION`):

```java
package com.example.portshield;

import android.hardware.usb.UsbConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, rule-based risk assessment engine for PortShield.
 * Completely independent of UI classes so it can be unit-tested or invoked from any component.
 *
 * Provisional Risk Thresholds (illustrative only, not statistical probabilities):
 * - 0 to 24: Low Indicated Risk
 * - 25 to 49: Caution
 * - 50 to 74: High Concern
 * - 75 to 100: Critical Concern
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

    /**
     * Maps an illustrative score (0-100) to a provisional severity category.
     */
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

    /**
     * Evaluates one of the three predefined simulated scenarios required for demonstration.
     */
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
                    "No suspicious indicators were selected in this simulation. Note: absence of observable USB Host interfaces does not guarantee hardware-level safety on untrusted ports.",
                    evidence,
                    "Continue charging if using a trusted wall outlet or personal power bank. When using public charging kiosks, use a physical USB data blocker.",
                    "SIMULATED (Illustrative baseline scenario — provisional threshold 0–24)",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else if (SCENARIO_UNEXPECTED_DATA.equals(scenarioType)) {
            int score = 35;
            evidence.add("[SIMULATED] User stated intent: Charging only at an untrusted USB port.");
            evidence.add("[SIMULATED] Connection capability: Active USB data lines detected alongside power delivery.");
            evidence.add("[SIMULATED] Exposed interface: Data-capable USB interface present without explicit file-transfer intent.");
            evidence.add("[UNVERIFIED] Low-level MTP/PTP/ADB handshake packets are not observable by standard Android apps.");
            return new SecurityEvent(
                    -1,
                    SCENARIO_UNEXPECTED_DATA,
                    score,
                    getRiskCategory(score),
                    "A data-capable connection conflicts with the user's stated charging-only intent. A data connection alone does not prove malicious intent, but increases exposure if file transfer or debugging is enabled.",
                    evidence,
                    "Verify that USB preference in Android System Settings is set to 'No data transfer' (Charge only) and ensure Developer USB Debugging (ADB) is disabled. Disconnect if you expected a power-only port.",
                    "SIMULATED (Intent-mismatch rule triggered — provisional threshold 25–49)",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else if (SCENARIO_SUSPICIOUS_HID.equals(scenarioType)) {
            int score = 85;
            evidence.add("[SIMULATED] Unexpected USB Interface Class 0x03 (Human Interface Device — Keyboard) declared.");
            evidence.add("[SIMULATED] Composite device anomaly: Mass Storage (0x08) + HID Keyboard (0x03) presented simultaneously.");
            evidence.add("[SIMULATED] Context conflict: User expected a charging cable or storage drive, not an input keyboard.");
            evidence.add("[DISCLAIMER] This is a harmless software simulation illustrating how a BadUSB/Rubber Ducky style device registers as an HID keyboard.");
            return new SecurityEvent(
                    -1,
                    SCENARIO_SUSPICIOUS_HID,
                    score,
                    getRiskCategory(score),
                    "The scenario includes a simulated unexpected HID interface and suspicious composite behavior. Keystroke-injection tools often impersonate USB keyboards to type automated commands.",
                    evidence,
                    "Disconnect the USB device or cable immediately. Do not grant USB Host permissions, and check whether any unexpected settings, browser pages, or terminal prompts were opened.",
                    "SIMULATED (Composite HID anomaly rule — provisional threshold 75–100)",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        } else {
            int score = 25;
            evidence.add("[SIMULATED] Unrecognized scenario identifier: " + scenarioType);
            evidence.add("[UNKNOWN] Evidence could not be matched to a known rule profile.");
            return new SecurityEvent(
                    -1,
                    "UNKNOWN_SCENARIO",
                    score,
                    getRiskCategory(score),
                    "An unrecognized scenario was requested. Unknown evidence remains classified as unknown/caution rather than safe.",
                    evidence,
                    "Select one of the supported demonstration scenarios or run an actual USB Host device scan.",
                    "UNKNOWN (Unverified input)",
                    SecurityEvent.SOURCE_SIMULATED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }
    }

    /**
     * Evaluates real USB device descriptors returned by Android's UsbManager.
     * Preserves strict honesty:
     * - Does NOT classify unknown/no-host state as guaranteed safe.
     * - Does NOT infer malicious intent solely from an unfamiliar Vendor ID or legitimate HID keyboard.
     */
    public static SecurityEvent evaluateObservedUsbState(
             boolean usbHostSupported,
             List<UsbMonitor.UsbDeviceInfo> devices,
             boolean userChargingOnlyIntent) {

        long now = System.currentTimeMillis();
        List<String> evidence = new ArrayList<>();

        if (!usbHostSupported) {
            int score = 25;
            evidence.add("[OBSERVED] Android PackageManager reports FEATURE_USB_HOST is NOT supported on this device.");
            evidence.add("[UNKNOWN] USB peripheral descriptors cannot be enumerated via UsbManager.");
            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    "USB Host mode is unsupported on this hardware. Because connection state cannot be verified, the risk state remains Caution/Unknown rather than Safe.",
                    evidence,
                    "Rely on Android system USB notifications and physical port hygiene (use a trusted AC adapter).",
                    "OBSERVED LIMITATION (Hardware does not support USB Host API)",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        if (devices == null || devices.isEmpty()) {
            int score = 15;
            evidence.add("[OBSERVED] UsbManager.getDeviceList() returned 0 attached USB Host peripherals.");
            evidence.add("[UNKNOWN / UNVERIFIED] Phone may be disconnected, charging from an AC wall adapter, or connected as a USB peripheral to a computer (which UsbManager Host API cannot see).");
            return new SecurityEvent(
                    -1,
                    EVENT_OBSERVED_USB_SCAN,
                    score,
                    getRiskCategory(score),
                    "No USB Host peripheral is currently visible to Android's UsbManager. Note that peripheral-mode connections to a PC or dumb charging cables are not visible through the USB Host API.",
                    evidence,
                    "If connected to an untrusted charger or computer, check the Android system notification shade to confirm USB mode is set to 'Charging only'.",
                    "OBSERVED (Partial visibility — 0 Host peripherals enumerated)",
                    SecurityEvent.SOURCE_OBSERVED,
                    now,
                    SecurityEvent.REVIEW_PENDING,
                    ""
            );
        }

        boolean hasHidInterface = false;
        boolean hasStorageOrDataInterface = false;
        boolean hasMultipleDistinctClasses = false;
        int totalInterfaces = 0;

        for (UsbMonitor.UsbDeviceInfo dev : devices) {
            evidence.add(String.format(
                    "[OBSERVED] Device: %s | VID: 0x%04X (%d) | PID: 0x%04X (%d) | Class: %s | Permission: %s",
                    dev.deviceName,
                    dev.vendorId, dev.vendorId,
                    dev.productId, dev.productId,
                    dev.deviceClassName,
                    dev.hasPermission ? "Granted" : "Not Granted (Public descriptors only)"
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
            evidence.add("[OBSERVED] Interfaces (" + dev.interfaceCount + "): " + dev.interfaceSummary);
        }

        evidence.add("[NOTE] An unfamiliar Vendor ID or legitimate HID interface alone does not prove malicious intent.");

        int score;
        String explanation;
        String recommendation;

        if (hasMultipleDistinctClasses && userChargingOnlyIntent) {
            score = 70;
            explanation = "Observed a composite USB device exposing both HID (input) and non-HID data interfaces while 'Charging-Only Intent' preference is active. While legitimate docks and headsets use composite descriptors, unexpected HID interfaces warrant review.";
            recommendation = "Confirm whether you intentionally connected a USB hub, dock, or keyboard. If you only plugged in a charging cable or storage drive, disconnect the device immediately.";
        } else if (hasHidInterface && userChargingOnlyIntent) {
            score = 50;
            explanation = "Observed an attached USB Human Interface Device (HID) while 'Charging-Only Intent' is enabled. If you connected a physical keyboard, mouse, or YubiKey, this is normal; if you only connected a charging cable, treat with high concern.";
            recommendation = "Verify that the connected accessory is a known input device. Disconnect immediately if no keyboard or mouse should be present.";
        } else if (userChargingOnlyIntent && (hasStorageOrDataInterface || totalInterfaces > 0)) {
            score = 35;
            explanation = "Observed an active data-capable USB Host device while 'Charging-Only Intent' is enabled. Data capability conflicts with charging-only expectations.";
            recommendation = "If you did not intend to connect a USB data accessory, disconnect the cable and use a power-only adapter.";
        } else {
            score = 20;
            explanation = "Observed " + devices.size() + " connected USB Host device(s) matching expected accessory usage. Remember that standard Android APIs only inspect declared descriptors and cannot verify device firmware.";
            recommendation = "Only grant USB permissions to applications and accessories you trust.";
        }

        return new SecurityEvent(
                -1,
                EVENT_OBSERVED_USB_SCAN,
                score,
                getRiskCategory(score),
                explanation,
                evidence,
                recommendation,
                "OBSERVED via Android UsbManager (Descriptor-level inspection only)",
                SecurityEvent.SOURCE_OBSERVED,
                now,
                SecurityEvent.REVIEW_PENDING,
                ""
        );
    }
}

```

## Phase 4: Local SQLite Persistence & Full CRUD Operations

### 4.1 `app/src/main/java/com/example/portshield/DatabaseHelper.java`
Implements `SQLiteOpenHelper` for table `security_events` with parameterized queries and `ContentValues` for `insertEvent` (CREATE), `getAllEventsNewestFirst` / `getEventById` / `getLatestEvent` (READ), `updateReviewAndNotes` (UPDATE — preserving original score and evidence), and `deleteEventById` / `clearAllEvents` (DELETE):

```java
package com.example.portshield;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite persistence helper implementing full CRUD operations on the
 * security_events table using parameterized queries and ContentValues.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "PortShieldDbHelper";
    private static final String DATABASE_NAME = "portshield_events.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_EVENTS = "security_events";
    public static final String COL_ID = "id";
    public static final String COL_EVENT_TYPE = "event_type";
    public static final String COL_RISK_SCORE = "risk_score";
    public static final String COL_RISK_LEVEL = "risk_level";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_EVIDENCE_JSON = "evidence_json";
    public static final String COL_EVENT_SOURCE = "event_source";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_REVIEW_STATUS = "review_status";
    public static final String COL_NOTES = "notes";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_EVENTS + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_EVENT_TYPE + " TEXT NOT NULL, "
                    + COL_RISK_SCORE + " INTEGER NOT NULL, "
                    + COL_RISK_LEVEL + " TEXT NOT NULL, "
                    + COL_DESCRIPTION + " TEXT, "
                    + COL_EVIDENCE_JSON + " TEXT, "
                    + COL_EVENT_SOURCE + " TEXT NOT NULL, "
                    + COL_TIMESTAMP + " INTEGER NOT NULL, "
                    + COL_REVIEW_STATUS + " TEXT NOT NULL, "
                    + COL_NOTES + " TEXT"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    /**
     * CREATE: Inserts a new SecurityEvent into security_events and returns its row ID.
     */
    public long insertEvent(SecurityEvent event) {
        if (event == null) return -1;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_EVENT_TYPE, event.getEventType());
            values.put(COL_RISK_SCORE, event.getRiskScore());
            values.put(COL_RISK_LEVEL, event.getRiskLevel());
            values.put(COL_DESCRIPTION, event.getDescription());
            values.put(COL_EVIDENCE_JSON, event.toEvidenceJsonString());
            values.put(COL_EVENT_SOURCE, event.getEventSource());
            values.put(COL_TIMESTAMP, event.getTimestamp());
            values.put(COL_REVIEW_STATUS, event.getReviewStatus());
            values.put(COL_NOTES, event.getNotes());

            long id = db.insert(TABLE_EVENTS, null, values);
            event.setId(id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting security event", e);
            return -1;
        }
    }

    /**
     * READ: Retrieves all security events ordered newest first (timestamp DESC, id DESC).
     * Returns an empty list if no records exist.
     */
    public List<SecurityEvent> getAllEventsNewestFirst() {
        List<SecurityEvent> events = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC, " + COL_ID + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    events.add(cursorToEvent(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading all security events", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }

    /**
     * READ: Retrieves the single newest event for the Home Dashboard, or null if empty.
     */
    public SecurityEvent getLatestEvent() {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC, " + COL_ID + " DESC",
                    "1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEvent(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching latest event", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * READ: Retrieves a single SecurityEvent by its primary key ID.
     */
    public SecurityEvent getEventById(long id) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEvent(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching event by ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * UPDATE: Updates only the review_status and notes columns for an existing record.
     * Strictly preserves original score, risk_level, description, evidence_json, and source.
     */
    public boolean updateReviewAndNotes(long id, String reviewStatus, String notes) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_REVIEW_STATUS, reviewStatus != null ? reviewStatus : SecurityEvent.REVIEW_PENDING);
            values.put(COL_NOTES, notes != null ? notes : "");

            int rows = db.update(
                    TABLE_EVENTS,
                    values,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating event ID: " + id, e);
            return false;
        }
    }

    /**
     * DELETE: Deletes a single security event by ID.
     */
    public boolean deleteEventById(long id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rows = db.delete(
                    TABLE_EVENTS,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting event ID: " + id, e);
            return false;
        }
    }

    /**
     * DELETE: Clears all security events from the table.
     */
    public int clearAllEvents() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            return db.delete(TABLE_EVENTS, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all events", e);
            return 0;
        }
    }

    private SecurityEvent cursorToEvent(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        String eventType = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TYPE));
        int riskScore = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RISK_SCORE));
        String riskLevel = cursor.getString(cursor.getColumnIndexOrThrow(COL_RISK_LEVEL));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION));
        String evidenceJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVIDENCE_JSON));
        String eventSource = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_SOURCE));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
        String reviewStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_STATUS));
        String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES));

        SecurityEvent.EvidencePayload payload = SecurityEvent.parseEvidenceJson(evidenceJson);

        return new SecurityEvent(
                id,
                eventType,
                riskScore,
                riskLevel,
                description,
                payload.evidenceList,
                payload.recommendedAction,
                payload.confidenceIndicator,
                eventSource,
                timestamp,
                reviewStatus,
                notes
        );
    }
}

```

## Phase 5: Android USB Host API Inspection (`UsbMonitor.java`)

### 5.1 `app/src/main/java/com/example/portshield/UsbMonitor.java`
Wraps `UsbManager`, `UsbDevice`, `UsbInterface`, and `UsbConstants` to inspect real attached USB OTG devices, format Vendor ID (`VID`), Product ID (`PID`), device class, and interface classes, and request runtime USB permission safely without fabricating data:

```java
package com.example.portshield;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper around Android's USB Host API (UsbManager, UsbDevice, UsbInterface).
 * Strictly reports only what Android exposes without fabricating descriptors or claiming
 * low-level bus packet visibility.
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

    /**
     * Checks whether the current Android device supports USB Host (OTG) mode.
     */
    public boolean isUsbHostSupported() {
        PackageManager pm = context.getPackageManager();
        return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST) && usbManager != null;
    }

    /**
     * Enumerates all currently attached USB Host devices accessible via UsbManager.
     */
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
                    Log.w(TAG, "Could not query USB permission state", e);
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
                        Log.w(TAG, "Error inspecting USB interface " + i, e);
                    }
                }

                String ifaceSummary = ifaceBuilder.length() > 0
                        ? ifaceBuilder.toString()
                        : "No interfaces declared in descriptor";

                String productLabel = "Unavailable (Requires permission or not provided)";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        String pName = device.getProductName();
                        String mName = device.getManufacturerName();
                        if (pName != null || mName != null) {
                            productLabel = (mName != null ? mName + " " : "") + (pName != null ? pName : "");
                            productLabel = productLabel.trim();
                        }
                    } catch (SecurityException se) {
                        productLabel = "Restricted until USB permission is granted";
                    } catch (Exception ignored) {
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
            Log.e(TAG, "Exception while enumerating USB devices", e);
        }

        return results;
    }

    /**
     * Requests runtime permission from the user to access the given UsbDevice.
     */
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

    /**
     * Maps standard Android UsbConstants integer class codes to readable names.
     */
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

    /**
     * Structured, UI-friendly value object representing an observed USB device.
     */
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

```

## Phase 6: Java Activity Classes & Navigation

### 6.1 `app/src/main/java/com/example/portshield/MainActivity.java`
Connects the Home Dashboard UI to `RiskEngine`, `DatabaseHelper`, `UsbMonitor`, and Android Notifications:

```java
package com.example.portshield;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

/**
 * Home Dashboard Activity for PortShield.
 * Displays live USB Host monitoring status, connection state, latest risk assessment,
 * three simulated scenario triggers, and navigation to USB Inspector, History, and Settings.
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        usbMonitor = new UsbMonitor(this);
        createNotificationChannel();

        tvDemoModeBadge = findViewById(R.id.tvDemoModeBadge);
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

        btnInspectUsb.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UsbInspectActivity.class);
            startActivity(intent);
        });

        btnOpenHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnOpenSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardState();
    }

    private void refreshDashboardState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean showDemoBadge = prefs.getBoolean(PREF_DEMO_BADGE, true);
        tvDemoModeBadge.setVisibility(showDemoBadge ? View.VISIBLE : View.GONE);

        // 1. Update real USB Host capability and currently visible devices
        boolean hostSupported = usbMonitor.isUsbHostSupported();
        if (hostSupported) {
            tvMonitoringStatus.setText("Active • Android USB Host API Supported");
            tvMonitoringStatus.setTextColor(ContextCompat.getColor(this, R.color.risk_low_green));

            List<UsbMonitor.UsbDeviceInfo> connected = usbMonitor.scanAccessibleDevices();
            if (connected.isEmpty()) {
                tvConnectionStatus.setText(
                        "OBSERVED: 0 USB Host accessories enumerated.\n"
                                + "UNKNOWN STATE: Wall-charger power-only vs. PC peripheral mode cannot be distinguished via UsbManager alone."
                );
            } else {
                tvConnectionStatus.setText(String.format(
                        Locale.US,
                        "OBSERVED: %d USB Host device(s) attached (First: %s, VID:0x%04X, PID:0x%04X). Tap 'Inspect Accessible USB Devices' for full descriptors.",
                        connected.size(),
                        connected.get(0).deviceName,
                        connected.get(0).vendorId,
                        connected.get(0).productId
                ));
            }
        } else {
            tvMonitoringStatus.setText("Limited • USB Host Mode Unsupported on This Device");
            tvMonitoringStatus.setTextColor(ContextCompat.getColor(this, R.color.risk_caution_amber));
            tvConnectionStatus.setText("UNKNOWN STATE: Hardware does not report android.hardware.usb.host support.");
        }

        // 2. Load latest saved assessment from SQLite
        SecurityEvent latest = dbHelper.getLatestEvent();
        if (latest == null) {
            tvLatestSourceBadge.setText("NO EVENTS YET");
            tvLatestSourceBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLatestScore.setText("-- / 100");
            tvLatestScore.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvLatestCategory.setText("Awaiting Assessment");
            tvLatestCategory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLatestEventType.setText("Select a scenario or scan USB devices");
            tvLatestSummary.setText("No security assessments recorded in SQLite yet. Run one of the three simulated scenarios below or inspect accessible USB devices.");
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

    /**
     * Evaluates a predefined simulated scenario, saves it to SQLite immediately,
     * optionally posts a local notification for high-risk events, and opens AssessmentActivity.
     */
    private void triggerSimulatedScenario(String scenarioType) {
        SecurityEvent event = RiskEngine.evaluateSimulatedScenario(scenarioType);
        long insertedId = dbHelper.insertEvent(event);

        if (insertedId != -1) {
            Toast.makeText(
                    this,
                    "Simulated scenario saved to SQLite (Record #" + insertedId + ")",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(this, "Warning: Failed to save scenario to SQLite", Toast.LENGTH_SHORT).show();
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
        if (!prefs.getBoolean(PREF_NOTIFICATIONS, true)) {
            return;
        }
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
            // Android 13+ may block notifications if POST_NOTIFICATIONS is not granted at runtime
        }
    }
}

```

### 6.2 `app/src/main/java/com/example/portshield/AssessmentActivity.java`
Displays the detailed assessment breakdown and allows manual saving if not already persisted:

```java
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

```

### 6.3 `app/src/main/java/com/example/portshield/UsbInspectActivity.java`
Scans real connected USB devices via `UsbMonitor`, handles permission prompts via a `BroadcastReceiver`, and evaluates observed device states through `RiskEngine`:

```java
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
 * USB Device Inspection Screen (Section 4C & Section 7).
 * Uses UsbMonitor to query Android's USB Host API, display real descriptors,
 * request USB permission when needed, and evaluate observed USB state via RiskEngine.
 */
public class UsbInspectActivity extends AppCompatActivity {

    private UsbMonitor usbMonitor;
    private DatabaseHelper dbHelper;
    private List<UsbMonitor.UsbDeviceInfo> lastScannedDevices;

    private TextView tvUsbHostSupport;
    private TextView tvUsbScanTimestamp;
    private TextView tvUsbDevicesOutput;
    private Button btnRequestUsbPermission;

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
                    Toast.makeText(context, "USB permission denied for " + name + " (Showing public descriptors only)", Toast.LENGTH_LONG).show();
                }
                performUsbScan();
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

        IntentFilter filter = new IntentFilter(UsbMonitor.ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbPermissionReceiver, filter);
        }

        btnScanUsbDevices.setOnClickListener(v -> performUsbScan());

        btnRequestUsbPermission.setOnClickListener(v -> {
            if (lastScannedDevices != null && !lastScannedDevices.isEmpty()) {
                UsbDevice target = lastScannedDevices.get(0).rawDevice;
                usbMonitor.requestDevicePermission(target);
            }
        });

        btnEvaluateObservedUsb.setOnClickListener(v -> evaluateAndOpenObservedAssessment());

        btnUsbBackDashboard.setOnClickListener(v -> finish());

        performUsbScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (Exception ignored) {
        }
    }

    private void performUsbScan() {
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        tvUsbScanTimestamp.setText("Observation Timestamp: " + timeStr);

        boolean supported = usbMonitor.isUsbHostSupported();
        if (!supported) {
            tvUsbHostSupport.setText("USB Host Mode: UNSUPPORTED on this device/emulator");
            tvUsbHostSupport.setTextColor(ContextCompat.getColor(this, R.color.risk_caution_amber));
            tvUsbDevicesOutput.setText(
                    "Android PackageManager reports that FEATURE_USB_HOST is not available.\n\n"
                            + "No fake device descriptors are fabricated. You can still evaluate this unverified state or run simulated scenarios from the Dashboard."
            );
            btnRequestUsbPermission.setVisibility(View.GONE);
            return;
        }

        tvUsbHostSupport.setText("USB Host Mode: SUPPORTED (Android UsbManager Ready)");
        tvUsbHostSupport.setTextColor(ContextCompat.getColor(this, R.color.risk_low_green));

        lastScannedDevices = usbMonitor.scanAccessibleDevices();
        if (lastScannedDevices.isEmpty()) {
            btnRequestUsbPermission.setVisibility(View.GONE);
            tvUsbDevicesOutput.setText(
                    "0 accessible USB Host devices detected by UsbManager.getDeviceList().\n\n"
                            + "Why this happens:\n"
                            + "• No USB OTG accessory is physically attached, OR\n"
                            + "• The phone is plugged into a wall power brick (no USB data enumeration), OR\n"
                            + "• The phone is acting as a USB peripheral connected to a host computer (Android's USB Host API only enumerates peripherals attached TO the phone via OTG)."
            );
            return;
        }

        boolean needsPerm = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lastScannedDevices.size(); i++) {
            UsbMonitor.UsbDeviceInfo info = lastScannedDevices.get(i);
            if (!info.hasPermission) {
                needsPerm = true;
            }
            if (i > 0) {
                sb.append("\n----------------------------------------\n\n");
            }
            sb.append("Device #").append(i + 1).append(": ").append(info.deviceName).append("\n");
            sb.append("Product/Manufacturer: ").append(info.productLabel).append("\n");
            sb.append(String.format(Locale.US, "Vendor ID (VID):      0x%04X (%d)\n", info.vendorId, info.vendorId));
            sb.append(String.format(Locale.US, "Product ID (PID):     0x%04X (%d)\n", info.productId, info.productId));
            sb.append("Device Class:         ").append(info.deviceClassName).append("\n");
            sb.append("Interface Count:      ").append(info.interfaceCount).append("\n");
            sb.append("Interface Classes:    ").append(info.interfaceSummary).append("\n");
            sb.append("USB Permission:       ").append(info.hasPermission ? "GRANTED" : "NOT GRANTED (Tap button above to request)").append("\n");
            sb.append("Observed Timestamp:   ").append(timeStr);
        }

        tvUsbDevicesOutput.setText(sb.toString());
        btnRequestUsbPermission.setVisibility(needsPerm ? View.VISIBLE : View.GONE);
    }

    private void evaluateAndOpenObservedAssessment() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        boolean chargingOnlyIntent = prefs.getBoolean(MainActivity.PREF_CHARGING_ONLY, true);

        boolean hostSupported = usbMonitor.isUsbHostSupported();
        List<UsbMonitor.UsbDeviceInfo> devices = usbMonitor.scanAccessibleDevices();

        SecurityEvent observedEvent = RiskEngine.evaluateObservedUsbState(
                hostSupported,
                devices,
                chargingOnlyIntent
        );
        long savedId = dbHelper.insertEvent(observedEvent);

        Intent intent = new Intent(UsbInspectActivity.this, AssessmentActivity.class);
        intent.putExtra(AssessmentActivity.EXTRA_EVENT_ID, savedId);
        intent.putExtra(AssessmentActivity.EXTRA_ALREADY_SAVED, savedId != -1);
        startActivity(intent);
    }
}

```

### 6.4 `app/src/main/java/com/example/portshield/HistoryActivity.java`
Loads all stored SQLite events sorted newest first and opens `EventDetailActivity` when a record is tapped:

```java
package com.example.portshield;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Security Event History Screen (Section 4D).
 * Displays locally saved SQLite records sorted newest first.
 * Selecting any item opens EventDetailActivity.
 */
public class HistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvEventHistory;
    private TextView tvEmptyHistory;
    private EventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        rvEventHistory = findViewById(R.id.rvEventHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        Button btnHistoryBack = findViewById(R.id.btnHistoryBack);

        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this, event -> {
            Intent intent = new Intent(HistoryActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        rvEventHistory.setAdapter(adapter);

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
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvEventHistory.setVisibility(View.VISIBLE);
            adapter.setEvents(events);
        }
    }
}

```

### 6.5 `app/src/main/java/com/example/portshield/EventAdapter.java`
Binds `SecurityEvent` items to `item_security_event.xml` in the History `RecyclerView`:

```java
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

```

### 6.6 `app/src/main/java/com/example/portshield/EventDetailActivity.java`
Displays the read-only score and evidence, updates review status and user notes in SQLite, and deletes individual records after confirmation:

```java
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

```

### 6.7 `app/src/main/java/com/example/portshield/SettingsActivity.java`
Manages demonstration preferences in `SharedPreferences` and clears the SQLite `security_events` table after user confirmation:

```java
package com.example.portshield;

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
 * charging-only baseline toggle, clear event history with confirmation, and About info.
 */
public class SettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvDbRecordCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        SwitchCompat switchDemoIndicator = findViewById(R.id.switchDemoIndicator);
        SwitchCompat switchNotifications = findViewById(R.id.switchNotifications);
        SwitchCompat switchChargingOnlyIntent = findViewById(R.id.switchChargingOnlyIntent);
        tvDbRecordCount = findViewById(R.id.tvDbRecordCount);
        Button btnClearAllHistory = findViewById(R.id.btnClearAllHistory);
        Button btnSettingsBack = findViewById(R.id.btnSettingsBack);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        switchDemoIndicator.setChecked(prefs.getBoolean(MainActivity.PREF_DEMO_BADGE, true));
        switchNotifications.setChecked(prefs.getBoolean(MainActivity.PREF_NOTIFICATIONS, true));
        switchChargingOnlyIntent.setChecked(prefs.getBoolean(MainActivity.PREF_CHARGING_ONLY, true));

        switchDemoIndicator.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.PREF_DEMO_BADGE, isChecked).apply()
        );

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

```

## Phase 7: Manual Verification & 12-Point Test Plan

| # | Test Case | Steps to Execute | Expected Result |
| :--- | :--- | :--- | :--- |
| **1** | **App Launch** | Build and run the app on an Android emulator or physical phone (`Shift + F10`). | App launches into `MainActivity` without crashes; dark navy theme and shield header render cleanly. |
| **2** | **Dashboard Initial State** | Launch on a fresh install before running any scenarios. | Dashboard shows USB Host support status, `NO EVENTS YET` badge, `-- / 100` score, and `Awaiting Assessment`. |
| **3** | **Simulated Scenarios & Scores** | Tap each of the 3 simulation buttons on the Dashboard (`Safe Charging`, `Unexpected Data Connection`, `Suspicious HID`). | Produces exact illustrative scores: `5/100` (Low Indicated Risk), `35/100` (Caution), and `85/100` (Critical Concern) with itemized evidence and explanations. |
| **4** | **SQLite CREATE** | Trigger any scenario or tap `Save Assessment to SQLite` on the Assessment screen. | Toast confirms the generated SQLite Record ID (`Record #1`, `#2`, etc.), and Dashboard updates its latest summary on return. |
| **5** | **SQLite READ (Newest First)** | Tap `Open Security Event History` after running multiple scenarios. | `HistoryActivity` lists all saved events ordered from newest timestamp at the top to oldest at the bottom. |
| **6** | **Event Detail Navigation** | Tap any row in `HistoryActivity`. | `EventDetailActivity` opens showing the Record ID, timestamp, score, read-only explanation, evidence list, and recommended action. |
| **7** | **SQLite UPDATE (Review & Notes)** | Check `Mark this security event as REVIEWED`, enter `"Verified during lab demo"`, and tap `Save Review Status & Note`. | Toast confirms SQLite update; returning to `HistoryActivity` shows `REVIEWED` in green and the note preview while original score and evidence remain untouched. |
| **8** | **SQLite DELETE (With Confirmation)** | Open an event in `EventDetailActivity`, tap `Delete Event Record`, and confirm in the dialog. | Record is removed from SQLite and disappears from `HistoryActivity`. Also test `Clear All Event History` in `SettingsActivity`. |
| **9** | **Persistence Across Restart** | Force-stop the app from Android Recents or run `adb shell am force-stop com.example.portshield`, then relaunch. | Dashboard still displays the latest saved assessment and `HistoryActivity` retains all remaining SQLite records. |
| **10** | **USB Inspection (Visible & Unavailable)** | Open `Inspect Accessible USB Devices` and tap `Scan Connected USB Devices` both with nothing attached and with a USB OTG accessory (flash drive, mouse, or keyboard) attached. | With no OTG device, explains why `0` devices are visible without fabricating data; with an OTG device attached, displays real `VID`, `PID`, device class, and interface classes. |
| **11** | **Simulated vs. Observed Distinction** | Compare records created by the 3 Dashboard scenario buttons against a record created by `Evaluate Observed USB State` in `UsbInspectActivity`. | Scenario records display purple `SIMULATED` badges; real USB scan evaluations display teal `OBSERVED` badges. |
| **12** | **Honest Security Framing** | Review banners and explanations across Dashboard, Assessment, USB Inspector, and Settings. | Every simulated scenario explicitly states it is a simulation, unknown states remain `UNKNOWN / Caution`, and no screen claims hardware-level blocking or guaranteed BadUSB detection. |

### Building an Installable Debug APK & ADB Commands
1. In Android Studio, select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Once complete, click **Locate** in the notification balloon or find the APK at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. To install via ADB on a connected physical Android phone with USB debugging enabled:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. To monitor database and USB inspection logs in real time:
   ```bash
   adb logcat -s PortShieldUsbMonitor PortShieldDbHelper
   ```
