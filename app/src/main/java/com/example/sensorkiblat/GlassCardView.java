package com.example.sensorkiblat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

/** Kartu / chip ala glassmorphism iOS. Atur sudut dengan app:glassRadius (999dp = pill). */
public class GlassCardView extends FrameLayout implements ThemeController.Listener {

    private final GlassPainter painter;
    private float radius;

    public GlassCardView(Context context) {
        this(context, null);
    }

    public GlassCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        painter = new GlassPainter(density);
        radius = 28f * density;
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassCardView)) {
                radius = a.getDimension(R.styleable.GlassCardView_glassRadius, radius);
            }
        }
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        painter.draw(canvas, getWidth(), getHeight(), radius, false);
        super.onDraw(canvas);
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