package com.example.sensorkiblat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

/** Background gradient + "blob" warna-warni di belakang kartu kaca. Ikut animasi tema. */
public class GlassBackgroundView extends View implements ThemeController.Listener {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int lastW = -1;
    private int lastH = -1;
    private float lastProgress = -1f;

    public GlassBackgroundView(Context context) {
        super(context);
    }

    public GlassBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GlassBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ThemeController.get().addListener(this);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        ThemeController.get().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onThemeProgress(float progress) {
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        ThemeController t = ThemeController.get();
        float progress = t.getProgress();

        if (w != lastW || h != lastH || progress != lastProgress) {
            lastW = w;
            lastH = h;
            lastProgress = progress;

            bgPaint.setShader(new LinearGradient(0, 0, 0, h, t.bgTop(), t.bgBottom(), Shader.TileMode.CLAMP));

            float r1 = w * 0.75f;
            int c1 = t.blob1();
            blob1Paint.setShader(new RadialGradient(w * 0.10f, h * 0.10f, r1, c1, c1 & 0x00FFFFFF, Shader.TileMode.CLAMP));

            float r2 = w * 0.80f;
            int c2 = t.blob2();
            blob2Paint.setShader(new RadialGradient(w * 1.00f, h * 0.42f, r2, c2, c2 & 0x00FFFFFF, Shader.TileMode.CLAMP));

            float r3 = w * 0.90f;
            int c3 = t.blob3();
            blob3Paint.setShader(new RadialGradient(w * 0.25f, h * 0.92f, r3, c3, c3 & 0x00FFFFFF, Shader.TileMode.CLAMP));
        }

        canvas.drawRect(0, 0, w, h, bgPaint);
        canvas.drawCircle(w * 0.10f, h * 0.10f, w * 0.75f, blob1Paint);
        canvas.drawCircle(w * 1.00f, h * 0.42f, w * 0.80f, blob2Paint);
        canvas.drawCircle(w * 0.25f, h * 0.92f, w * 0.90f, blob3Paint);
    }
}