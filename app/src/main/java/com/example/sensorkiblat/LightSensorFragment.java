package com.example.sensorkiblat;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.Objects;

/** Halaman 1: Light Sensor + Auto Dark Mode. */
public class LightSensorFragment extends Fragment
        implements ThemeController.Listener, LightSensorHelper.LuxListener {

    private LightSensorHelper helper;

    private ThemedTextView tvLuxBig;
    private ThemedTextView tvLux;
    private ThemedTextView tvStatus;
    private ThemedTextView tvDarkVal;
    private ThemedTextView tvLightVal;
    private ThemedTextView tvModeHint;
    private LuxGaugeView gauge;
    private GlassSeekBar seekDark;
    private GlassSeekBar seekLight;

    private ValueAnimator luxAnimator;
    private float shownLux = 0f;
    private String lastStatus = "";

    public LightSensorFragment() {
        super(R.layout.fragment_light_sensor);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = ((MainActivity) requireActivity()).getLightHelper();
        ThemeController theme = ThemeController.get();

        tvLuxBig = view.findViewById(R.id.tv_lux_big);
        tvLux = view.findViewById(R.id.tv_lux);
        tvStatus = view.findViewById(R.id.tv_status);
        tvDarkVal = view.findViewById(R.id.tv_dark_val);
        tvLightVal = view.findViewById(R.id.tv_light_val);
        tvModeHint = view.findViewById(R.id.tv_mode_hint);
        gauge = view.findViewById(R.id.gauge);
        GlassTabBar tabMode = view.findViewById(R.id.tab_mode);
        seekDark = view.findViewById(R.id.seek_dark);
        seekLight = view.findViewById(R.id.seek_light);
        GlassCardView cardNoSensor = view.findViewById(R.id.card_no_sensor);

        cardNoSensor.setVisibility(helper.isAvailable() ? View.GONE : View.VISIBLE);

        // --- Segmented control: Auto / Terang / Gelap (index == konstanta MODE_*)
        tabMode.setItems(new String[]{"Auto", "Terang", "Gelap"}, null);
        tabMode.setSelectedIndex(theme.getMode(), false);
        tabMode.setOnTabSelectedListener(index -> {
            theme.setMode(index);
            helper.evaluate();
            updateStatus();
            updateModeHint();
        });

        // --- Slider ambang Lux
        seekDark.setMax(49);                                    // 1..50 lx
        seekDark.setProgress(Math.round(theme.getDarkLux()) - 1);
        seekLight.setMax(280);                                  // 20..300 lx
        seekLight.setProgress(Math.round(theme.getLightLux()) - 20);

        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) onThresholdChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekDark.setOnSeekBarChangeListener(seekListener);
        seekLight.setOnSeekBarChangeListener(seekListener);

        refreshThresholdUi();
        updateStatus();
        updateModeHint();
        theme.addListener(this);
    }

    private void onThresholdChanged() {
        float dark = seekDark.getProgress() + 1;
        float light = seekLight.getProgress() + 20;
        if (light < dark + 10f) {              // jaga jarak minimal supaya hysteresis tetap ada
            light = dark + 10f;
            seekLight.setProgress(Math.round(light) - 20);
        }
        ThemeController.get().setThresholds(dark, light);
        helper.evaluate();
        refreshThresholdUi();
    }

    private void refreshThresholdUi() {
        ThemeController t = ThemeController.get();
        tvDarkVal.setText(String.format(Locale.getDefault(), "< %d lx", Math.round(t.getDarkLux())));
        tvLightVal.setText(String.format(Locale.getDefault(), "> %d lx", Math.round(t.getLightLux())));
        gauge.setThresholds(t.getDarkLux(), t.getLightLux());
    }

    private void updateStatus() {
        ThemeController t = ThemeController.get();
        String s = (t.isDark() ? "Dark Mode" : "Light Mode") + " · "
                + (t.getMode() == ThemeController.MODE_AUTO ? "Auto" : "Manual");
        if (!Objects.equals(s, lastStatus)) {
            lastStatus = s;
            tvStatus.setText(s);
        }
    }

    private void updateModeHint() {
        switch (ThemeController.get().getMode()) {
            case ThemeController.MODE_LIGHT:
                tvModeHint.setText(R.string.hint_mode_light);
                break;
            case ThemeController.MODE_DARK:
                tvModeHint.setText(R.string.hint_mode_dark);
                break;
            default:
                tvModeHint.setText(R.string.hint_mode_auto);
                break;
        }
    }

    // ---------------------------------------------------------- lux updates

    @Override
    public void onResume() {
        super.onResume();
        helper.setLuxListener(this);
        if (helper.getLastLux() >= 0) showLux(helper.getLastLux(), false);
    }

    @Override
    public void onPause() {
        helper.setLuxListener(null);
        super.onPause();
    }

    @Override
    public void onLux(float lux) {
        showLux(lux, true);
    }

    private void showLux(float lux, boolean animate) {
        tvLux.setText(String.format(Locale.getDefault(), "Cahaya: %.1f Lux", lux));
        gauge.setLux(lux);

        if (luxAnimator != null) luxAnimator.cancel();
        if (!animate) {
            shownLux = lux;
            tvLuxBig.setText(formatBig(shownLux));
            return;
        }
        luxAnimator = ValueAnimator.ofFloat(shownLux, lux);
        luxAnimator.setDuration(300);
        luxAnimator.addUpdateListener(a -> {
            shownLux = (Float) a.getAnimatedValue();
            tvLuxBig.setText(formatBig(shownLux));
        });
        luxAnimator.start();
    }

    private String formatBig(float v) {
        return (v < 100f)
                ? String.format(Locale.getDefault(), "%.1f", v)
                : String.format(Locale.getDefault(), "%.0f", v);
    }

    @Override
    public void onThemeProgress(float progress) {
        updateStatus();
    }

    @Override
    public void onDestroyView() {
        ThemeController.get().removeListener(this);
        if (luxAnimator != null) luxAnimator.cancel();
        super.onDestroyView();
    }
}