package com.example.portshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Security Analyst Authentication & Registration Screen.
 * - If no PIN is registered yet: Guides user through 4-digit PIN setup.
 * - If PIN already exists: Prompts for the registered passcode to unlock the console.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String PREF_ANALYST_PIN = "pref_analyst_pin";

    private SharedPreferences prefs;
    private boolean isRegistrationMode = false;

    private TextView tvCardHeader;
    private TextView tvCardSubheader;
    private EditText etAnalystPin;
    private EditText etConfirmPin;
    private Button btnLoginAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);

        tvCardHeader = findViewById(R.id.tvCardHeader);
        tvCardSubheader = findViewById(R.id.tvCardSubheader);
        etAnalystPin = findViewById(R.id.etAnalystPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnLoginAction = findViewById(R.id.btnLoginAction);

        // Check if a PIN has already been registered
        isRegistrationMode = !prefs.contains(PREF_ANALYST_PIN);

        if (isRegistrationMode) {
            setupRegistrationView();
        } else {
            setupLoginView();
        }

        btnLoginAction.setOnClickListener(v -> {
            if (isRegistrationMode) {
                handleRegistration();
            } else {
                handleLogin();
            }
        });
    }

    private void setupRegistrationView() {
        tvCardHeader.setText("INITIALIZE ANALYST PASSCODE");
        tvCardSubheader.setText("First-Time Setup: Create a 4-digit master passcode to protect this diagnostic console.");
        etAnalystPin.setHint("Create 4-Digit PIN");
        etConfirmPin.setVisibility(View.VISIBLE);
        btnLoginAction.setText("Register Passcode & Unlock");
    }

    private void setupLoginView() {
        tvCardHeader.setText("SECURITY ANALYST ACCESS");
        tvCardSubheader.setText("Enter your registered 4-digit analyst passcode to unlock console.");
        etAnalystPin.setHint("••••");
        etConfirmPin.setVisibility(View.GONE);
        btnLoginAction.setText("Authenticate & Open Console");
    }

    private void handleRegistration() {
        String pin1 = etAnalystPin.getText() != null ? etAnalystPin.getText().toString().trim() : "";
        String pin2 = etConfirmPin.getText() != null ? etConfirmPin.getText().toString().trim() : "";

        if (pin1.length() != 4) {
            Toast.makeText(this, "PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin1.equals(pin2)) {
            Toast.makeText(this, "PINs do not match. Please re-enter.", Toast.LENGTH_SHORT).show();
            etConfirmPin.setText("");
            return;
        }

        // Store the user's chosen PIN in local SharedPreferences
        prefs.edit().putString(PREF_ANALYST_PIN, pin1).apply();
        Toast.makeText(this, "Passcode Registered Successfully!", Toast.LENGTH_SHORT).show();

        openMainActivity();
    }

    private void handleLogin() {
        String enteredPin = etAnalystPin.getText() != null ? etAnalystPin.getText().toString().trim() : "";
        String savedPin = prefs.getString(PREF_ANALYST_PIN, "");

        if (enteredPin.isEmpty()) {
            Toast.makeText(this, "Please enter your 4-digit passcode.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredPin.equals(savedPin)) {
            Toast.makeText(this, "Analyst Authenticated.", Toast.LENGTH_SHORT).show();
            openMainActivity();
        } else {
            Toast.makeText(this, "Access Denied: Incorrect Passcode.", Toast.LENGTH_SHORT).show();
            etAnalystPin.setText("");
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}