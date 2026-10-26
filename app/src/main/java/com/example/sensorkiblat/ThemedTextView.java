package com.example.sensorkiblat;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/** TextView yang warnanya otomatis ikut tema (pakai app:glassTextRole di XML). */
public class ThemedTextView extends AppCompatTextView implements ThemeController.Listener {

    public static final int ROLE_PRIMARY = 0;
    public static final int ROLE_SECONDARY = 1;
    public static final int ROLE_ACCENT = 2;
    public static final int ROLE_SUCCESS = 3;

    private int role = ROLE_PRIMARY;

    public ThemedTextView(Context context) {
        this(context, null);
    }

    public ThemedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ThemedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ThemedTextView)) {
                role = a.getInt(R.styleable.ThemedTextView_glassTextRole, ROLE_PRIMARY);
            }
        }
        applyColor();
    }

    public void setRole(int newRole) {
        if (role != newRole) {
            role = newRole;
            applyColor();
        }
    }

    private void applyColor() {
        ThemeController t = ThemeController.get();
        int color;
        switch (role) {
            case ROLE_SECONDARY: color = t.textSecondary(); break;
            case ROLE_ACCENT:    color = t.accent(); break;
            case ROLE_SUCCESS:   color = t.success(); break;
            default:             color = t.textPrimary(); break;
        }
        setTextColor(color);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ThemeController.get().addListener(this);
        applyColor();
    }

    @Override
    protected void onDetachedFromWindow() {
        ThemeController.get().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onThemeProgress(float progress) {
        applyColor();
    }
}