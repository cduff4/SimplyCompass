package com.example.simply_compass;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView compassImg;
    private TextView azimuthTxt;
    private VideoView calibrateVid;
    private AdView adView;
    private int azimuth;
    private SensorManager sensorManager;
    private Sensor rotationVector, accelerometer, magnetometer;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[9];
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean haveSensor = false, haveSensor2 = false;
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        adView.loadAd(adRequest);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        compassImg = findViewById(R.id.compassImg);
        azimuthTxt = findViewById(R.id.azimuthTxt);

        calibrateVid = findViewById(R.id.calibrateVid);
        calibrateVid.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.calibrate_sensors));
        calibrateVid.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        start();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            azimuth = (int)((Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360);
        }

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if(lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (int)((Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360);
        }

        azimuth = Math.round(azimuth);
        compassImg.setRotation(-azimuth);

        String where;
        if (azimuth >= 350 || azimuth <= 10) {
            where = "N";
        }
        else if (azimuth > 280) {
            where = "NW";
        }
        else if (azimuth > 260) {
            where = "W";
        }
        else if (azimuth > 190) {
            where = "SW";
        }
        else if (azimuth > 170) {
            where = "S";
        }
        else if (azimuth > 100) {
            where = "SE";
        }
        else if (azimuth > 80) {
            where = "E";
        }
        else {
            where = "NE";
        }

        azimuthTxt.setText(String.format("%d° %s", azimuth, where));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if ((!haveSensor || sensor.getType() != Sensor.TYPE_ROTATION_VECTOR)
                && (!haveSensor2 || (sensor.getType() != Sensor.TYPE_ACCELEROMETER
                && sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD))) {
            return;
        }

        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW && !calibrateVid.isPlaying()) {
            calibrateVid.setVisibility(View.VISIBLE);
            compassImg.setVisibility(View.INVISIBLE);
            azimuthTxt.setVisibility(View.INVISIBLE);
            calibrateVid.start();
        }
        else {
            calibrateVid.setVisibility(View.INVISIBLE);
            compassImg.setVisibility(View.VISIBLE);
            azimuthTxt.setVisibility(View.VISIBLE);
            calibrateVid.stopPlayback();
        }
    }

    public void start(){
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                noSensorAlert();
            }
            else {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                haveSensor = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        else {
            rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void noSensorAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device does not support the simply_compass.")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        finish();
                    }
                });
    }

    public void stop(){
        if(haveSensor && haveSensor2){
            sensorManager.unregisterListener(this, accelerometer);
            sensorManager.unregisterListener(this, magnetometer);
        }
        else if(haveSensor) {
            sensorManager.unregisterListener(this, rotationVector);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
