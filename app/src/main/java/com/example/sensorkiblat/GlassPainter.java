package com.example.sensorkiblat;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Menggambar efek "glass" ala iOS: fill semi-transparan + kilau (sheen) di bagian atas
 * + border gradient tipis. (Android tidak punya backdrop blur asli, ini simulasinya.)
 */
public final class GlassPainter {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float stroke;

    private float lastW = -1f;
    private float lastH = -1f;
    private int lastSheen = 0;
    private int lastBorderA = 0;
    private int lastBorderB = 0;

    public GlassPainter(float density) {
        stroke = 1.2f * density;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(stroke);
    }

    public void draw(Canvas canvas, float w, float h, float radius, boolean accentFill) {
        if (w <= 0 || h <= 0) return;
        ThemeController t = ThemeController.get();

        float half = stroke / 2f;
        rect.set(half, half, w - half, h - half);
        float r = Math.min(radius, Math.min(rect.width(), rect.height()) / 2f);

        fillPaint.setColor(accentFill ? t.accent() : t.glassFill());
        canvas.drawRoundRect(rect, r, r, fillPaint);

        int sheen = t.glassSheen();
        int borderA = t.glassBorderA();
        int borderB = t.glassBorderB();

        if (w != lastW || h != lastH || sheen != lastSheen) {
            sheenPaint.setShader(new LinearGradient(0, 0, 0, h * 0.65f,
                    sheen, sheen & 0x00FFFFFF, Shader.TileMode.CLAMP));
            lastSheen = sheen;
        }

        if (w != lastW || h != lastH || borderA != lastBorderA || borderB != lastBorderB) {
            borderPaint.setShader(new LinearGradient(0, 0, w, h,
                    borderA, borderB, Shader.TileMode.CLAMP));
            lastBorderA = borderA;
            lastBorderB = borderB;
        }

        lastW = w;
        lastH = h;

        canvas.drawRoundRect(rect, r, r, sheenPaint);
        canvas.drawRoundRect(rect, r, r, borderPaint);
    }
}