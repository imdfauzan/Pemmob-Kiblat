package com.example.sensorkiblat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

/** Tombol rounded (pill) ala glassmorphism iOS. app:glassAccent="true" -> tombol biru solid. */
public class GlassButton extends AppCompatTextView implements ThemeController.Listener {

    private final GlassPainter painter;
    private float radius;
    private boolean accent = false;

    public GlassButton(Context context) {
        this(context, null);
    }

    public GlassButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public GlassButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        painter = new GlassPainter(density);
        radius = 999f * density;
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassButton)) {
                radius = a.getDimension(R.styleable.GlassButton_glassRadius, radius);
                accent = a.getBoolean(R.styleable.GlassButton_glassAccent, false);
            }
        }
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        applyColors();
    }

    private void applyColors() {
        ThemeController t = ThemeController.get();
        setTextColor(accent ? 0xFFFFFFFF : t.accent());
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        painter.draw(canvas, getWidth(), getHeight(), radius, accent);
        super.onDraw(canvas);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        animate().scaleX(pressed ? 0.95f : 1f).scaleY(pressed ? 0.95f : 1f).setDuration(120).start();
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
        invalidate();
    }
}