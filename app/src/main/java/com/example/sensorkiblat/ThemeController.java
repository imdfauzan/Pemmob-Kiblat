package com.example.sensorkiblat;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;

/**
 * Pusat pengatur tema (Light / Dark) untuk seluruh aplikasi.
 *
 * Kenapa tidak pakai AppCompatDelegate.setDefaultNightMode()?
 * Karena fungsi itu memanggil recreate() pada Activity -> layar berkedip & tidak bisa
 * dianimasikan. Di sini kita punya nilai "progress" 0..1 (0 = Light, 1 = Dark) yang
 * dianimasikan dengan ValueAnimator, lalu semua view menggambar ulang warnanya setiap frame.
 */
public final class ThemeController {

    public interface Listener {
        void onThemeProgress(float progress);
    }

    public static final int MODE_AUTO = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String PREFS = "sensor_kiblat_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_DARK = "last_dark";
    private static final String KEY_DARK_LUX = "dark_lux";
    private static final String KEY_LIGHT_LUX = "light_lux";
    private static final long ANIM_MS = 700L;

    private static final ThemeController INSTANCE = new ThemeController();

    public static ThemeController get() {
        return INSTANCE;
    }

    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private final ArrayList<Listener> listeners = new ArrayList<>();
    private SharedPreferences prefs;
    private ValueAnimator animator;
    private boolean initialized = false;

    private float progress = 0f;   // 0 = Light, 1 = Dark
    private boolean dark = false;  // target state
    private int mode = MODE_AUTO;
    private float darkLux = 10f;   // di bawah ini -> Dark Mode
    private float lightLux = 50f;  // di atas ini  -> Light Mode

    private ThemeController() {}

    public void init(Context context) {
        if (initialized) return;
        initialized = true;
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mode = prefs.getInt(KEY_MODE, MODE_AUTO);
        darkLux = prefs.getFloat(KEY_DARK_LUX, 10f);
        lightLux = prefs.getFloat(KEY_LIGHT_LUX, 50f);
        boolean lastDark = prefs.getBoolean(KEY_DARK, false);
        dark = (mode == MODE_DARK) || (mode == MODE_AUTO && lastDark);
        progress = dark ? 1f : 0f;
    }

    // ---------------------------------------------------------------- state

    public int getMode() { return mode; }
    public boolean isDark() { return dark; }
    public float getProgress() { return progress; }
    public float getDarkLux() { return darkLux; }
    public float getLightLux() { return lightLux; }

    public void setMode(int newMode) {
        mode = newMode;
        if (prefs != null) prefs.edit().putInt(KEY_MODE, mode).apply();
        switch (mode) {
            case MODE_LIGHT:
                setDark(false, true);
                break;
            case MODE_DARK:
                setDark(true, true);
                break;
            default:
                break;
        }
    }

    public void setThresholds(float dark, float light) {
        darkLux = dark;
        lightLux = light;
        if (prefs != null) {
            prefs.edit().putFloat(KEY_DARK_LUX, darkLux).putFloat(KEY_LIGHT_LUX, lightLux).apply();
        }
    }

    /** Ganti tema, dengan animasi transisi warna kalau animate = true. */
    public void setDark(boolean newDark, boolean animate) {
        dark = newDark;
        if (prefs != null) prefs.edit().putBoolean(KEY_DARK, dark).apply();

        float target = dark ? 1f : 0f;
        if (animator != null) animator.cancel();

        if (!animate) {
            progress = target;
            notifyListeners();
            return;
        }
        if (progress == target) return;

        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(ANIM_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (Float) a.getAnimatedValue();
            notifyListeners();
        });
        animator.start();
    }

    // ------------------------------------------------------------ listeners

    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        Listener[] copy = listeners.toArray(new Listener[0]);
        for (Listener l : copy) l.onThemeProgress(progress);
    }

    // -------------------------------------------------------------- palette

    private int mix(int light, int darkColor) {
        return (Integer) evaluator.evaluate(progress, light, darkColor);
    }

    public int bgTop()          { return mix(0xFFEAF1FF, 0xFF0A0E1F); }
    public int bgBottom()       { return mix(0xFFFDEEF5, 0xFF1B1238); }
    public int blob1()          { return mix(0x995B9BFF, 0x663B5BFF); }
    public int blob2()          { return mix(0x80FF8FB8, 0x667C3AED); }
    public int blob3()          { return mix(0x7343E0C0, 0x590EA5E9); }

    public int glassFill()      { return mix(0x99FFFFFF, 0x1FFFFFFF); }
    public int glassSheen()     { return mix(0x66FFFFFF, 0x1AFFFFFF); }
    public int glassBorderA()   { return mix(0xE6FFFFFF, 0x4DFFFFFF); }
    public int glassBorderB()   { return mix(0x40FFFFFF, 0x14FFFFFF); }
    public int highlight()      { return mix(0xF2FFFFFF, 0x40FFFFFF); }

    public int textPrimary()    { return mix(0xFF0F172A, 0xFFF5F7FF); }
    public int textSecondary()  { return mix(0xFF64748B, 0xFF9AA3BF); }
    public int accent()         { return mix(0xFF007AFF, 0xFF0A84FF); }
    public int success()        { return mix(0xFF34C759, 0xFF30D158); }
}