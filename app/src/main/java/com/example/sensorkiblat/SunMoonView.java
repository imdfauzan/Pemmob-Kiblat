package com.example.sensorkiblat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

/** Ikon matahari yang berubah (morphing) menjadi bulan sabit mengikuti progress tema. */
public class SunMoonView extends View implements ThemeController.Listener {

    private static final int SUN = 0xFFFFB400;
    private static final int MOON = 0xFFDCE3FF;
    // x, y (relatif terhadap radius), ukuran
    private static final float[][] STARS = {
            {0.78f, -0.62f, 0.050f}, {0.52f, -0.90f, 0.035f},
            {0.95f, -0.25f, 0.030f}, {-0.82f, -0.58f, 0.030f}
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path circle = new Path();
    private final Path bite = new Path();
    private final Path body = new Path();

    private float lastCx = -1f;
    private float lastCy = -1f;
    private float lastBase = -1f;
    private int lastColor = 0;

    public SunMoonView(Context context) {
        super(context);
    }

    public SunMoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SunMoonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float p = ThemeController.get().getProgress();
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float base = Math.min(w, h) / 2f;
        float r = base * 0.42f;
        int color = ColorUtils.blendARGB(SUN, MOON, p);

        // Halo
        if (cx != lastCx || cy != lastCy || base != lastBase || color != lastColor) {
            haloPaint.setShader(new RadialGradient(cx, cy, base, ColorUtils.setAlphaComponent(color, 0x55),
                    color & 0x00FFFFFF, Shader.TileMode.CLAMP));
            lastCx = cx;
            lastCy = cy;
            lastBase = base;
            lastColor = color;
        }
        haloPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, base, haloPaint);

        // Sinar matahari (memudar & berputar saat jadi malam)
        float rayAlpha = 1f - Math.min(1f, p * 1.8f);
        if (rayAlpha > 0.02f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(base * 0.07f);
            paint.setColor(ColorUtils.setAlphaComponent(color, (int) (255 * rayAlpha)));
            float inner = r * 1.35f;
            float outer = inner + base * 0.18f * rayAlpha;
            canvas.save();
            canvas.rotate(p * 90f, cx, cy);
            for (int i = 0; i < 8; i++) {
                canvas.save();
                canvas.rotate(i * 45f, cx, cy);
                canvas.drawLine(cx, cy - inner, cx, cy - outer, paint);
                canvas.restore();
            }
            canvas.restore();
        }

        // Badan: lingkaran dikurangi lingkaran "gigitan" -> bulan sabit
        circle.reset();
        circle.addCircle(cx, cy, r, Path.Direction.CW);
        float dist = r * (2.6f + (0.5f - 2.6f) * p);
        float biteX = cx + dist * 0.7071f;
        float biteY = cy - dist * 0.7071f;
        bite.reset();
        bite.addCircle(biteX, biteY, r * 0.9f, Path.Direction.CW);
        body.reset();
        body.op(circle, bite, Path.Op.DIFFERENCE);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(body, paint);

        // Bintang (muncul saat malam)
        if (p > 0.05f) {
            paint.setColor(ColorUtils.setAlphaComponent(0xFFFFFFFF, (int) (255 * p)));
            for (float[] s : STARS) {
                canvas.drawCircle(cx + s[0] * base * 0.8f, cy + s[1] * base * 0.8f, s[2] * base, paint);
            }
        }
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
}