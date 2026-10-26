package com.example.sensorkiblat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import java.util.Objects;

/**
 * Logika Light Sensor (dikembangkan dari LightSensorActivity di PPT dosen).
 *
 * Sama seperti kode dosen:
 *   lux < ambang gelap  -> Dark Mode
 *   lux > ambang terang -> Light Mode
 *   di antaranya        -> tidak berubah (hysteresis, mencegah flicker)
 *
 * Tambahan: debounce 800 ms supaya tema tidak "kedip" kalau cahaya berubah sesaat.
 */
public class LightSensorHelper implements SensorEventListener {

    public interface LuxListener {
        void onLux(float lux);
    }

    private static final long DEBOUNCE_MS = 800L;

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LuxListener luxListener;
    private float lastLux = -1f;
    private Boolean pendingDark = null;

    private final Runnable applyRunnable = () -> {
        ThemeController theme = ThemeController.get();
        Boolean target = pendingDark;
        pendingDark = null;
        if (target == null || theme.getMode() != ThemeController.MODE_AUTO) return;

        // cek ulang: kondisi harus masih sama setelah 800 ms
        Boolean candidate = candidateFor(lastLux, theme);
        if (Objects.equals(candidate, target)) {
            theme.setDark(target, true);
        }
    };

    public LightSensorHelper(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = (sensorManager == null) ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public boolean isAvailable() {
        return lightSensor != null;
    }

    public float getLastLux() {
        return lastLux;
    }

    public void setLuxListener(LuxListener listener) {
        luxListener = listener;
    }

    /** Panggil di onResume(). */
    public void start() {
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /** Panggil di onPause() supaya tidak boros baterai. */
    public void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        cancelPending();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            lastLux = lux;
            if (luxListener != null) luxListener.onLux(lux);
            evaluate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /** Nilai Lux terakhir dievaluasi ulang (dipanggil juga saat mode / ambang diubah). */
    public void evaluate() {
        ThemeController theme = ThemeController.get();
        if (theme.getMode() != ThemeController.MODE_AUTO || lastLux < 0) {
            cancelPending();
            return;
        }
        Boolean candidate = candidateFor(lastLux, theme);
        if (candidate == null || candidate == theme.isDark()) {
            cancelPending();
            return;
        }
        if (!Objects.equals(candidate, pendingDark)) {
            cancelPending();
            pendingDark = candidate;
            handler.postDelayed(applyRunnable, DEBOUNCE_MS);
        }
    }

    private Boolean candidateFor(float lux, ThemeController theme) {
        if (lux < theme.getDarkLux()) return Boolean.TRUE;    // ruangan gelap  -> Dark Mode
        if (lux > theme.getLightLux()) return Boolean.FALSE;  // ruangan terang -> Light Mode
        return null;                                          // zona hysteresis
    }

    private void cancelPending() {
        pendingDark = null;
        handler.removeCallbacks(applyRunnable);
    }
}