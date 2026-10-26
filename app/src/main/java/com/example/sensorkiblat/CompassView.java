package com.example.sensorkiblat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

/**
 * Kompas kaca. Dial (N/E/S/W + derajat) berputar berlawanan arah HP,
 * jarum emas menunjuk Ka'bah dan berubah hijau saat HP menghadap Kiblat.
 */
public class CompassView extends View implements ThemeController.Listener {

    public interface OnAlignedChangeListener {
        void onAlignedChanged(boolean aligned);
    }

    private static final int GOLD = 0xFFFFB020;
    private static final int NORTH_RED = 0xFFFF3B30;
    private static final String[] CARDINALS = {"N", "E", "S", "W"};

    private final float density;
    @SuppressWarnings("deprecation")
    private final float scaledDensity;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private float heading = 0f;           // arah HP terhadap utara sejati (derajat)
    private float qibla = Float.NaN;      // sudut Kiblat terhadap utara sejati (derajat)
    private boolean aligned = false;
    private float alignAmount = 0f;       // 0..1, dianimasikan
    private OnAlignedChangeListener alignedListener;

    private float lastCx = -1f;
    private float lastCy = -1f;
    private float lastRadius = -1f;
    private int lastSheen = 0;
    private int lastBorderA = 0;
    private int lastBorderB = 0;

    public CompassView(Context context) {
        this(context, null);
    }

    public CompassView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("deprecation")
    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    // ------------------------------------------------------------------ API

    public void setHeading(float degrees) {
        heading = degrees;
        updateAligned();
        invalidate();
    }

    public void setQibla(float degrees) {
        qibla = degrees;
        updateAligned();
        invalidate();
    }

    public boolean isAligned() {
        return aligned;
    }

    public void setOnAlignedChangeListener(OnAlignedChangeListener l) {
        alignedListener = l;
    }

    /** Ubah sudut ke rentang -180..180. */
    public static float wrap180(float angle) {
        float a = angle % 360f;
        if (a > 180f) return a - 360f;
        if (a < -180f) return a + 360f;
        return a;
    }

    private void updateAligned() {
        boolean now = false;
        if (!Float.isNaN(qibla)) {
            float diff = Math.abs(wrap180(qibla - heading));
            now = diff <= (aligned ? 5f : 3f);   // hysteresis kecil supaya tidak berkedip
        }
        if (now != aligned) {
            aligned = now;
            if (alignedListener != null) alignedListener.onAlignedChanged(aligned);
        }
    }

    // ----------------------------------------------------------------- draw

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int max = (int) (380 * density);
        if (w > max) w = max;
        setMeasuredDimension(w, w);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        ThemeController t = ThemeController.get();
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - 6f * density;
        if (radius <= 0) return;

        int sheen = t.glassSheen();
        int borderA = t.glassBorderA();
        int borderB = t.glassBorderB();

        if (cx != lastCx || cy != lastCy || radius != lastRadius || sheen != lastSheen) {
            sheenPaint.setShader(new LinearGradient(cx, cy - radius, cx, cy + radius * 0.3f,
                    sheen, sheen & 0x00FFFFFF, Shader.TileMode.CLAMP));
            lastSheen = sheen;
        }

        if (cx != lastCx || cy != lastCy || radius != lastRadius || borderA != lastBorderA || borderB != lastBorderB) {
            borderPaint.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                    borderA, borderB, Shader.TileMode.CLAMP));
            borderPaint.setStrokeWidth(1.4f * density);
            lastBorderA = borderA;
            lastBorderB = borderB;
        }

        lastCx = cx;
        lastCy = cy;
        lastRadius = radius;

        // 1. Disc kaca
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(t.glassFill());
        canvas.drawCircle(cx, cy, radius, paint);

        canvas.drawCircle(cx, cy, radius, sheenPaint);
        canvas.drawCircle(cx, cy, radius - 0.7f * density, borderPaint);

        // 2. Cincin hijau saat menghadap Kiblat
        if (alignAmount > 0.01f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.5f * density);
            paint.setColor(ColorUtils.setAlphaComponent(t.success(), (int) (255 * alignAmount)));
            canvas.drawCircle(cx, cy, radius - 1.8f * density, paint);
        }

        // 3. Dial berputar
        canvas.save();
        canvas.rotate(-heading, cx, cy);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int tickColor = t.textSecondary();
        for (int deg = 0; deg < 360; deg += 5) {
            boolean cardinal = deg % 90 == 0;
            boolean major = deg % 30 == 0;
            float len = cardinal ? 14f * density : (major ? 10f * density : 5f * density);
            paint.setStrokeWidth(cardinal ? 2.4f * density : (major ? 1.6f * density : density));
            paint.setColor(cardinal ? t.textPrimary() : tickColor);
            canvas.save();
            canvas.rotate(deg, cx, cy);
            float top = cy - radius + 10f * density;
            canvas.drawLine(cx, top, cx, top + len, paint);
            canvas.restore();
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90f, cx, cy);
            paint.setTextSize(20f * scaledDensity);
            paint.setColor(i == 0 ? NORTH_RED : t.textPrimary());
            canvas.drawText(CARDINALS[i], cx, cy - radius + 48f * density, paint);
            canvas.restore();
        }
        paint.setTextSize(10f * scaledDensity);
        paint.setColor(tickColor);
        for (int deg = 30; deg < 360; deg += 30) {
            if (deg % 90 == 0) continue;
            canvas.save();
            canvas.rotate(deg, cx, cy);
            canvas.drawText(Integer.toString(deg), cx, cy - radius + 38f * density, paint);
            canvas.restore();
        }

        // 4. Jarum Kiblat + ikon Ka'bah
        if (!Float.isNaN(qibla)) {
            canvas.save();
            canvas.rotate(qibla, cx, cy);

            int needleColor = ColorUtils.blendARGB(GOLD, t.success(), alignAmount);
            float tipY = cy - radius * 0.56f;
            float half = 9f * density;
            path.reset();
            path.moveTo(cx, tipY);
            path.lineTo(cx + half, cy);
            path.lineTo(cx, cy + 16f * density);
            path.lineTo(cx - half, cy);
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(needleColor);
            canvas.drawPath(path, paint);

            // Ka'bah (tetap tegak di layar)
            float kY = tipY - 16f * density;
            canvas.save();
            canvas.rotate(-(qibla - heading), cx, kY);
            float s = 10f * density;
            rect.set(cx - s, kY - s, cx + s, kY + s);
            paint.setColor(0xFF1C1C1E);
            canvas.drawRoundRect(rect, 3f * density, 3f * density, paint);
            paint.setColor(GOLD);
            canvas.drawRect(cx - s, kY - 5f * density, cx + s, kY - 2f * density, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(density);
            paint.setColor(ColorUtils.setAlphaComponent(GOLD, 200));
            canvas.drawRoundRect(rect, 3f * density, 3f * density, paint);
            canvas.restore();

            canvas.restore();
        }
        canvas.restore();

        // 5. Pusat + penunjuk arah HP (tetap, tidak berputar)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(t.textPrimary());
        canvas.drawCircle(cx, cy, 6f * density, paint);

        float topY = cy - radius + 2f * density;
        path.reset();
        path.moveTo(cx - 8f * density, topY - 6f * density);
        path.lineTo(cx + 8f * density, topY - 6f * density);
        path.lineTo(cx, topY + 10f * density);
        path.close();
        paint.setColor(t.accent());
        canvas.drawPath(path, paint);

        // animasi cincin hijau
        float target = aligned ? 1f : 0f;
        if (Math.abs(alignAmount - target) > 0.01f) {
            alignAmount += (target - alignAmount) * 0.2f;
            postInvalidateOnAnimation();
        } else {
            alignAmount = target;
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