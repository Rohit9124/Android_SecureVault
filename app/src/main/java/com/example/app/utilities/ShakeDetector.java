package com.example.app.utilities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Detects shake gestures using the device's accelerometer.
 * Triggers callback when 2 strong shakes are detected.
 */
public class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 15.0f; // Acceleration threshold for shake detection
    private static final int SHAKE_COUNT_REQUIRED = 2; // Number of shakes required to trigger
    private static final long SHAKE_RESET_TIME_MS = 3000; // Time window for shake detection (3 seconds)

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private OnShakeListener shakeListener;

    private int shakeCount = 0;
    private long lastShakeTime = 0;

    /**
     * Interface for shake detection callback.
     */
    public interface OnShakeListener {
        void onShake();
    }

    /**
     * Constructor for ShakeDetector.
     *
     * @param context The application context
     */
    public ShakeDetector(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    /**
     * Sets the shake listener callback.
     *
     * @param listener The listener to be notified when shake is detected
     */
    public void setOnShakeListener(OnShakeListener listener) {
        this.shakeListener = listener;
    }

    /**
     * Starts listening for shake events.
     * Should be called when the feature is enabled.
     */
    public void start() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Stops listening for shake events.
     * Should be called when the feature is disabled or activity is paused.
     */
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Reset shake count when stopping
        shakeCount = 0;
        lastShakeTime = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate acceleration magnitude (excluding gravity)
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

            long currentTime = System.currentTimeMillis();

            // Check if acceleration exceeds threshold
            if (acceleration > SHAKE_THRESHOLD) {
                // Reset count if too much time has passed since last shake
                if (currentTime - lastShakeTime > SHAKE_RESET_TIME_MS) {
                    shakeCount = 0;
                }

                shakeCount++;
                lastShakeTime = currentTime;

                // Trigger callback if required shake count is reached
                if (shakeCount >= SHAKE_COUNT_REQUIRED) {
                    if (shakeListener != null) {
                        shakeListener.onShake();
                    }
                    // Reset after triggering
                    shakeCount = 0;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for shake detection
    }
}
