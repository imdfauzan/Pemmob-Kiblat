package com.example.sensorkiblat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.graphics.ColorUtils;

/** SeekBar yang warnanya ikut tema. */
public class GlassSeekBar extends AppCompatSeekBar implements ThemeController.Listener {

    public GlassSeekBar(Context context) {
        super(context);
        applyColors();
    }

    public GlassSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyColors();
    }

    public GlassSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyColors();
    }

    private void applyColors() {
        ThemeController t = ThemeController.get();
        int accent = t.accent();
        setProgressTintList(ColorStateList.valueOf(accent));
        setThumbTintList(ColorStateList.valueOf(accent));
        setProgressBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(t.textSecondary(), 80)));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ThemeController.get().addListener(this);
        applyColors();
    }

    @Override
    protected void onDetachedFromWindow() {
        ThemeController.get().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onThemeProgress(float progress) {
        applyColors();
    }
}
