package de.thomashaas.vissmanndetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    SensorManager sensorManager;
    Sensor sensor;
    EditText edOutput;
    StringBuilder sb = new StringBuilder();
    Button btnStart, btnStop;
    ArrayList<Float> x = new ArrayList<>(), y = new ArrayList<>(), z = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edOutput = findViewById(R.id.edOutput);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        enableButtons(true);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    // Create a listener
    private final SensorEventListener accelerationSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            // String line = String.format(Locale.CANADA, "%.2f %.2f %.2f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);

            // Log.d("VSMN",
            //         line
            // );

            Log.d("VSMN", String.valueOf(sensorEvent.values[2]));

            x.add(Math.abs(sensorEvent.values[0]));
            y.add(Math.abs(sensorEvent.values[1]));
            z.add(Math.abs(sensorEvent.values[2]));

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // mStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStop();
    }

    private void enableButtons(Boolean enableStart) {
        btnStart.setEnabled(enableStart);
        btnStop.setEnabled(!enableStart);
    }

    private void mStart() {
        sb.setLength(0);
        x.clear();
        y.clear();
        z.clear();
        enableButtons(false);
        sensorManager.registerListener(accelerationSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void mStop() {

        enableButtons(true);
        try {
            sensorManager.unregisterListener(accelerationSensorListener);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        Toast.makeText(MainActivity.this, String.valueOf(x.size()), Toast.LENGTH_SHORT).show();

        mCalc();

        edOutput.setText(sb);
    }

    private void mCalc() {
        x.sort(Collections.reverseOrder());
        y.sort(Collections.reverseOrder());
        z.sort(Collections.reverseOrder());

        Log.d("VSMN", z.toString());

        float sx = 0, sy = 0, sz = 0f;

        float len = Math.min(x.size() / 10f, 100f);

        for (int i = 0; i < len; i++) {
            sx += x.get(i);
            sy += y.get(i);
            sz += z.get(i);
        }

        sb.append(String.format(Locale.CANADA, "%.2f %.2f %.2f", sx / len, sy / len, sz / len));

    }

    public void btnStartClick(View v) {
        mStart();
    }

    public void btnStopClick(View v) {
        mStop();
    }

}