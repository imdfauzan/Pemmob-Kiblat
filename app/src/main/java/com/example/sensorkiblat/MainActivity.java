package com.example.sensorkiblat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements ThemeController.Listener {

    private static final String KEY_TAB = "current_tab";

    private LightSensorHelper lightHelper;
    private WindowInsetsControllerCompat insetsController;
    private Boolean darkIcons = null;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tema kita atur sendiri lewat ThemeController, jadi abaikan Dark Mode bawaan sistem.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        ThemeController.get().init(this);

        // Edge-to-edge (konten sampai ke bawah status bar & navigation bar)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setWindowTransparent();
        insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        setContentView(R.layout.activity_main);

        lightHelper = new LightSensorHelper(this);

        View root = findViewById(R.id.root);
        View container = findViewById(R.id.fragment_container);
        GlassTabBar navBar = findViewById(R.id.nav_bar);

        float density = getResources().getDisplayMetrics().density;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            container.setPadding(0, bars.top, 0, 0);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) navBar.getLayoutParams();
            lp.bottomMargin = bars.bottom + (int) (16 * density);
            navBar.setLayoutParams(lp);
            return insets;
        });

        navBar.setItems(new String[]{getString(R.string.title_sensor), getString(R.string.title_kiblat)},
                new int[]{R.drawable.ic_nav_sensor, R.drawable.ic_nav_kiblat});
        navBar.setOnTabSelectedListener(this::showTab);

        if (savedInstanceState == null) {
            showTab(0);
        } else {
            currentTab = savedInstanceState.getInt(KEY_TAB, 0);
        }
        navBar.setSelectedIndex(currentTab, false);
    }

    private void showTab(int index) {
        currentTab = index;
        Fragment fragment = (index == 0) ? new LightSensorFragment() : new KiblatFragment();
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Dipakai LightSensorFragment untuk menampilkan nilai Lux. */
    public LightSensorHelper getLightHelper() {
        return lightHelper;
    }

    @Override
    protected void onStart() {
        super.onStart();
        ThemeController.get().addListener(this);
        onThemeProgress(ThemeController.get().getProgress());
    }

    @Override
    protected void onStop() {
        ThemeController.get().removeListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lightHelper.start();   // sensor cahaya aktif di semua halaman, jadi Auto Dark Mode selalu jalan
    }

    @Override
    protected void onPause() {
        lightHelper.stop();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, currentTab);
    }

    /** Ikon status bar / navigation bar ikut berubah (gelap <-> terang). */
    @Override
    public void onThemeProgress(float progress) {
        boolean dark = progress > 0.5f;
        if (darkIcons == null || darkIcons != dark) {
            darkIcons = dark;
            insetsController.setAppearanceLightStatusBars(!dark);
            insetsController.setAppearanceLightNavigationBars(!dark);
        }
    }

    @SuppressWarnings("deprecation")
    private void setWindowTransparent() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }
}