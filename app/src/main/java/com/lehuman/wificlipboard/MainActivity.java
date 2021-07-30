package com.lehuman.wificlipboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class MainActivity extends Activity {
    TextView ServerAddress;
    EditText portNumberBox, timeoutNumberBox;
    SwitchMaterial toastSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerAddress = findViewById(R.id.ServerAddress);
        portNumberBox = findViewById(R.id.portNumberBox);
        timeoutNumberBox = findViewById(R.id.timeoutNumberBox);
        toastSwitch = findViewById(R.id.toastSwitch);

        toastSwitch.setChecked(Settings.getTOAST(this));
        timeoutNumberBox.setText(String.valueOf(Settings.getTIMEOUT(this)));
        portNumberBox.setText(String.valueOf(Settings.getPORT(this)));
        updateIP();
    }

    private void updateIP() {
        ServerAddress.setText(String.format(Locale.US, "%s:%d", Utility.getIPAddress(), Settings.getPORT(this)));
    }

    public void onReset(View view) {
        Context context = view.getContext();
        Settings.reset(context);
        SingleTapWidget.reloadSettings(context);
        toastSwitch.setChecked(Settings.getTOAST(context));
        timeoutNumberBox.setText(String.valueOf(Settings.getTIMEOUT(context)));
        portNumberBox.setText(String.valueOf(Settings.getPORT(context)));
        updateIP();
        Toast.makeText(context, "Settings reset", Toast.LENGTH_SHORT).show();
    }

    public void onClickUpdate(View view) {
        String port = String.valueOf(portNumberBox.getText());
        String timeout = String.valueOf(timeoutNumberBox.getText());
        boolean toasts = toastSwitch.isChecked();
        Context context = view.getContext();

        try {
            Settings.setPORT(context, Integer.parseInt(port));
            Settings.setTIMEOUT(context, Integer.parseInt(timeout));
            Settings.setTOAST(context, toasts);
        } catch (NumberFormatException e) {
            Toast.makeText(context, "Invalid Input", Toast.LENGTH_SHORT).show();
            return;
        }

        SingleTapWidget.reloadSettings(context);
        updateIP();
        Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show();
    }
}