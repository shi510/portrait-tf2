package com.vcall;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

public class SettingsActivity extends AppCompatActivity {
    Spinner model_spinner;
    Spinner device_spinner;
    String selectedModelName;
    String selectedDeviceName;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();
        setContentView(R.layout.activity_settings);
        model_spinner = (Spinner)findViewById(R.id.model_spinner);
        device_spinner = (Spinner)findViewById(R.id.device_spinner);
        selectedModelName = model_spinner.getSelectedItem().toString();
        selectedDeviceName = device_spinner.getSelectedItem().toString();
        model_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModelName = model_spinner.getSelectedItem().toString();
                intent.putExtra("device_name", selectedDeviceName);
                intent.putExtra("model_name", selectedModelName);
                setResult(0, intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        device_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDeviceName = device_spinner.getSelectedItem().toString();
                intent.putExtra("device_name", selectedDeviceName);
                intent.putExtra("model_name", selectedModelName);
                setResult(0, intent);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
