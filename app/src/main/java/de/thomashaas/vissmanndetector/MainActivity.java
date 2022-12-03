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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    static final int EVENT_CNT = 100;
    static final int EVENT_PART = 10;
    
    SensorManager sensorManager;
    Sensor sensor;
    EditText edOutput;
    StringBuilder sb = new StringBuilder();
    Button btnStart, btnStop;
    private boolean measuring;
    ArrayList<Float> lst = new ArrayList<>();

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
        // sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Create a listener
    private final SensorEventListener accelerationSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            String line = String.format(Locale.CANADA, "%.2f %.2f %.2f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            Log.d("VSMN",line);

            if (!measuring) {
                return;
            }


            lst.add(Math.abs(sensorEvent.values[2]));

            if (lst.size() >= EVENT_CNT) {
                mCalc();
            }
            
            // Log.d("VSMN", String.valueOf(sensorEvent.values[2]));

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private void mCalc() {
        log("mCalc");
        ArrayList<Float> nLst = new ArrayList<>(lst);

        lst.clear();
        nLst.sort(Collections.reverseOrder());
        float sum = 0f;
        for(int i=0; i < EVENT_PART; i++) {
            sum += nLst.get(i);
        }
        float ave = sum / (float) EVENT_PART;
        // Log.d("VSMN", nLst.get(0) + " " + nLst.get(1));
        // Log.d("VSMN", String.valueOf(ave));

        runOnUiThread(() -> {
            edOutput.setText(String.valueOf(ave));
        });

    }

    private void log(String s) {
        Log.d("VSMN", s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStart();
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
        sensorManager.registerListener(accelerationSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void mStop() {
        try {
            sensorManager.unregisterListener(accelerationSensorListener);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }



    public void btnStartClick(View v) {
        enableButtons(false);
        lst.clear();
        measuring = true;
    }

    public void btnStopClick(View v) {
        enableButtons(true);
        measuring = false;
    }

}