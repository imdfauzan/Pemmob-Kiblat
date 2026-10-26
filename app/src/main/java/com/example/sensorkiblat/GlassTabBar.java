package com.example.sensorkiblat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * Tab bar berbentuk pill ala iOS (dipakai untuk navbar bawah DAN segmented control Auto/Terang/Gelap).
 * Indikator terpilih bergeser dengan animasi pegas.
 */
public class GlassTabBar extends View implements ThemeController.Listener {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private final float density;
    private final GlassPainter painter;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private String[] labels = new String[0];
    private Drawable[] icons = new Drawable[0];
    private int selected = 0;
    private float indicator = 0f;
    private ValueAnimator animator;
    private OnTabSelectedListener listener;

    public GlassTabBar(Context context) {
        this(context, null);
    }

    public GlassTabBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("deprecation")
    public GlassTabBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        painter = new GlassPainter(density);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
    }

    public void setItems(String[] labels, int[] iconRes) {
        this.labels = labels;
        icons = new Drawable[labels.length];
        if (iconRes != null) {
            for (int i = 0; i < labels.length; i++) {
                if (iconRes[i] != 0) {
                    Drawable d = ContextCompat.getDrawable(getContext(), iconRes[i]);
                    icons[i] = (d == null) ? null : d.mutate();
                }
            }
        }
        invalidate();
    }

    public void setOnTabSelectedListener(OnTabSelectedListener l) {
        listener = l;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void setSelectedIndex(int index, boolean animate) {
        int targetIndex = Math.max(0, index);
        selected = targetIndex;
        if (animator != null) animator.cancel();
        if (!animate) {
            indicator = targetIndex;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(indicator, targetIndex);
        animator.setDuration(320);
        animator.setInterpolator(new OvershootInterpolator(0.9f));
        animator.addUpdateListener(a -> {
            indicator = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = labels.length;
        if (n == 0) return;
        ThemeController t = ThemeController.get();
        float w = getWidth();
        float h = getHeight();

        painter.draw(canvas, w, h, h / 2f, false);

        float pad = 4f * density;
        float cellW = (w - 2 * pad) / n;
        float pos = Math.max(0f, Math.min(n - 1, indicator));

        // Indikator (pill putih yang bergeser)
        highlightPaint.setColor(t.highlight());
        float left = pad + pos * cellW;
        rect.set(left, pad, left + cellW, h - pad);
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, highlightPaint);

        // Ikon + label
        for (int i = 0; i < n; i++) {
            float cx = pad + cellW * (i + 0.5f);
            float sel = Math.max(0f, Math.min(1f, 1f - Math.abs(pos - i)));
            int color = ColorUtils.blendARGB(t.textSecondary(), t.accent(), sel);
            textPaint.setColor(color);

            Drawable icon = icons[i];
            float iconSize = (icon != null) ? 20f * density : 0f;
            float gap = (icon != null) ? 6f * density : 0f;
            float textW = textPaint.measureText(labels[i]);
            float startX = cx - (iconSize + gap + textW) / 2f;

            if (icon != null) {
                int top = (int) (h / 2f - iconSize / 2f);
                icon.setBounds((int) startX, top, (int) (startX + iconSize), (int) (top + iconSize));
                icon.setTint(color);
                icon.draw(canvas);
            }
            float baseline = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(labels[i], startX + iconSize + gap, baseline, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (labels.length == 0) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                float pad = 4f * density;
                float cellW = (getWidth() - 2 * pad) / labels.length;
                int idx = (int) ((e.getX() - pad) / cellW);
                idx = Math.max(0, Math.min(labels.length - 1, idx));
                if (idx != selected) {
                    setSelectedIndex(idx, true);
                    if (listener != null) listener.onTabSelected(idx);
                }
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
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