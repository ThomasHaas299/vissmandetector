package de.thomashaas.vissmanndetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    static final int EVENT_CNT = 100;
    static final int EVENT_PART = 10;
    private final ArrayList<Integer> lst = new ArrayList<>();
    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView tvCurrentValue, tvCalcValue;
    private EditText edOffset;
    private Button btnStartTransmission;
    private Button btnStopTransmission;
    private boolean transmitting;
    private int offset = 0;
    // Create a listener
    private final SensorEventListener accelerationSensorListener = new SensorEventListener() {
        @SuppressWarnings("CommentedOutCode")
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            // String line = String.format(Locale.CANADA, "%.2f %.2f %.2f", sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            // log(line);

            int value = (int) (Math.abs(sensorEvent.values[2]) * 100);

            lst.add(value);

            if (lst.size() >= EVENT_CNT) {
                mCalc();
            }

            // Log.d("VSMN", String.valueOf(sensorEvent.values[2]));

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.tvCurrentValueLabel)).setText("Aktueller Wert:");
        ((TextView) findViewById(R.id.tvOffsetLabel)).setText("Offset:");
        ((TextView) findViewById(R.id.tvCalcValueLabel)).setText("Wert abzgl. Offset:");

        tvCurrentValue = findViewById(R.id.tvCurrentValue);
        tvCalcValue = findViewById(R.id.tvCalcValue);
        edOffset = findViewById(R.id.edOffset);
        Button btnSaveOffset = findViewById(R.id.btnSaveOffset);
        btnStartTransmission = findViewById(R.id.btnStartTransmission);
        btnStopTransmission = findViewById(R.id.btnStopTransmission);

        tvCurrentValue.setText("");
        tvCalcValue.setText("");
        edOffset.setText(String.valueOf(offset));
        btnSaveOffset.setText("Offset speichern");
        btnStartTransmission.setText("Übertragung starten");
        btnStopTransmission.setText("Übertragung stoppen");

        // OnClickListener
        btnSaveOffset.setOnClickListener(view -> saveOffset());
        btnStartTransmission.setOnClickListener(view -> {
            enableButtons(false);
            transmitting = true;
        });
        btnStopTransmission.setOnClickListener(view -> {
            enableButtons(true);
            transmitting = false;
        });

        enableButtons(true);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void saveOffset() {
        log("save Offset");

        try {
            offset = Integer.parseInt(edOffset.getText().toString());
            Toast.makeText(this, "Offset gespeichert", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }

    private void mCalc() {
        // log("mCalc");
        ArrayList<Integer> nLst = new ArrayList<>(lst);

        lst.clear();
        nLst.sort(Collections.reverseOrder());

        // log(nLst.toString());

        int sum = 0;
        for (int i = 0; i < EVENT_PART; i++) {
            sum += nLst.get(i);
        }
        int ave = sum / EVENT_PART;

        // log(String.valueOf(ave));

        tvCurrentValue.setText(String.valueOf(ave));

        int calcValue = Math.abs(ave - offset);
        tvCalcValue.setText(String.valueOf(calcValue));

    }

    @SuppressWarnings("SameParameterValue")
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
        btnStartTransmission.setEnabled(enableStart);
        btnStopTransmission.setEnabled(!enableStart);
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

}