package com.example.sensorkiblat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

/** Bar gauge Lux (skala logaritmik) dengan penanda ambang gelap & terang. */
public class LuxGaugeView extends View implements ThemeController.Listener {

    private static final float MAX_LUX = 10000f;

    private final float density;
    private final float scaledDensity;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float fraction = 0f;
    private float darkLux = 10f;
    private float lightLux = 50f;
    private ValueAnimator animator;

    private float lastW = -1f;
    private int lastAccent = 0;

    public LuxGaugeView(Context context) {
        this(context, null);
    }

    public LuxGaugeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("deprecation")
    public LuxGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
    }

    public void setLux(float lux) {
        float target = toFraction(lux);
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(fraction, target);
        animator.setDuration(300);
        animator.addUpdateListener(a -> {
            fraction = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setThresholds(float dark, float light) {
        darkLux = dark;
        lightLux = light;
        invalidate();
    }

    private static float toFraction(float lux) {
        double f = Math.log10(1.0 + Math.max(0f, lux)) / Math.log10(1.0 + MAX_LUX);
        return (float) Math.min(1.0, Math.max(0.0, f));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        ThemeController t = ThemeController.get();
        float w = getWidth();
        float barH = 10f * density;
        float barTop = 24f * density;
        float barBottom = barTop + barH;
        float midY = (barTop + barBottom) / 2f;

        // Track
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtils.setAlphaComponent(t.textSecondary(), 60));
        rect.set(0, barTop, w, barBottom);
        canvas.drawRoundRect(rect, barH / 2f, barH / 2f, paint);

        // Isi
        float fillRight = Math.max(barH, fraction * w);
        int accent = t.accent();
        if (w != lastW || accent != lastAccent) {
            fillPaint.setShader(new LinearGradient(0, 0, w, 0,
                    ColorUtils.setAlphaComponent(accent, 90), accent, Shader.TileMode.CLAMP));
            lastW = w;
            lastAccent = accent;
        }
        rect.set(0, barTop, fillRight, barBottom);
        canvas.drawRoundRect(rect, barH / 2f, barH / 2f, fillPaint);

        // Garis ambang
        float xDark = toFraction(darkLux) * w;
        float xLight = toFraction(lightLux) * w;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(2f * density);
        paint.setColor(t.textSecondary());
        canvas.drawLine(xDark, barTop - 5f * density, xDark, barBottom + 5f * density, paint);
        canvas.drawLine(xLight, barTop - 5f * density, xLight, barBottom + 5f * density, paint);

        // Label ambang
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11f * scaledDensity);
        drawLabel(canvas, "Gelap < " + Math.round(darkLux), xDark, 14f * density, w);
        drawLabel(canvas, "Terang > " + Math.round(lightLux), xLight, barBottom + 22f * density, w);

        // Thumb
        float r = 9f * density;
        float tx = Math.max(r, Math.min(w - r, fraction * w));
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(tx, midY, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * density);
        paint.setColor(t.accent());
        canvas.drawCircle(tx, midY, r - 1.25f * density, paint);
    }

    private void drawLabel(Canvas canvas, String text, float cx, float y, float w) {
        float half = paint.measureText(text) / 2f;
        float x = Math.max(half, Math.min(w - half, cx));
        paint.setColor(ThemeController.get().textSecondary());
        canvas.drawText(text, x, y, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ThemeController.get().addListener(this);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        ThemeController.get().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onThemeProgress(float progress) {
        invalidate();
    }
}