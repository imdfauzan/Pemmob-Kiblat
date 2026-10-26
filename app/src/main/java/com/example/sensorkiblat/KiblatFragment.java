package com.example.sensorkiblat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

/**
 * Halaman 2: Kiblat Finder.
 *
 * Alur: Accelerometer + Magnetic Field -> getRotationMatrix() -> getOrientation() -> azimuth
 *       -> koreksi deklinasi (utara magnetik -> utara sejati)
 *       GPS (lat, lon) -> great-circle bearing ke Ka'bah -> sudut Kiblat
 *       Jarum = sudut Kiblat - azimuth HP
 */
public class KiblatFragment extends Fragment implements SensorEventListener, LocationListener {

    private static final float ALPHA = 0.10f;           // low-pass filter sensor
    private static final float AZIMUTH_ALPHA = 0.20f;   // smoothing sudut
    private static final String PREFS = "kiblat_prefs";
    private static boolean askedPermission = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private LocationManager locationManager;

    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private final float[] rotation = new float[9];
    private final float[] orientation = new float[3];
    private boolean haveGravity = false;
    private boolean haveMagnet = false;
    private float smoothedAzimuth = Float.NaN;   // relatif utara magnetik
    private float declination = 0f;              // magnetik -> sejati
    private float lastHeading = 0f;
    private boolean tilted = false;
    private long lastTextUpdate = 0L;

    private double qiblaBearing = Double.NaN;
    private boolean manualMode = false;
    private boolean updatingLocation = false;

    private CompassView compass;
    private ThemedTextView tvStatus;
    private ThemedTextView tvHeading;
    private ThemedTextView tvQibla;
    private ThemedTextView tvDiff;
    private ThemedTextView tvDistance;
    private ThemedTextView tvLocation;
    private ThemedTextView tvAccuracy;
    private GlassCardView cardCalibration;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!isAdded() || tvLocation == null) return;
                if (hasLocationPermission()) {
                    startLocationUpdates();
                } else {
                    tvLocation.setText(R.string.permission_location_denied);
                    Toast.makeText(requireContext(),
                            R.string.permission_location_denied_toast,
                            Toast.LENGTH_LONG).show();
                }
            });

    public KiblatFragment() {
        super(R.layout.fragment_kiblat);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context ctx = requireContext();
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        compass = view.findViewById(R.id.compass);
        tvStatus = view.findViewById(R.id.tv_status);
        tvHeading = view.findViewById(R.id.tv_heading);
        tvQibla = view.findViewById(R.id.tv_qibla);
        tvDiff = view.findViewById(R.id.tv_diff);
        tvDistance = view.findViewById(R.id.tv_distance);
        tvLocation = view.findViewById(R.id.tv_location);
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        cardCalibration = view.findViewById(R.id.card_calibration);

        compass.setOnAlignedChangeListener(aligned -> {
            if (aligned) {
                view.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                        ? HapticFeedbackConstants.CONFIRM
                        : HapticFeedbackConstants.LONG_PRESS);
            }
            updateStatusChip();
        });

        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> refreshLocation());
        view.findViewById(R.id.btn_manual).setOnClickListener(v -> showManualDialog());

        loadSavedLocation();
        updateStatusChip();

        if (!hasLocationPermission() && !askedPermission) {
            askedPermission = true;
            requestLocationPermission();
        }
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (!manualMode && hasLocationPermission()) startLocationUpdates();
    }

    @Override
    public void onPause() {
        sensorManager.unregisterListener(this);
        stopLocationUpdates();
        super.onPause();
    }

    // -------------------------------------------------------------- sensors

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                lowPass(event.values, gravity, haveGravity);
                haveGravity = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                lowPass(event.values, geomagnetic, haveMagnet);
                haveMagnet = true;
                break;
            default:
                return;
        }
        if (!haveGravity || !haveMagnet) return;

        if (SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotation, orientation);
            float rawAzimuth = (float) Math.toDegrees(orientation[0]);   // -180..180
            float pitch = (float) Math.toDegrees(orientation[1]);
            float roll = (float) Math.toDegrees(orientation[2]);
            float normalizedAzimuth = (rawAzimuth + 360f) % 360f;

            // smoothing sudut yang aman untuk lompatan 359 -> 0
            if (Float.isNaN(smoothedAzimuth)) {
                smoothedAzimuth = normalizedAzimuth;
            } else {
                float delta = CompassView.wrap180(normalizedAzimuth - smoothedAzimuth);
                smoothedAzimuth = (smoothedAzimuth + AZIMUTH_ALPHA * delta + 360f) % 360f;
            }

            float trueHeading = (smoothedAzimuth + declination + 360f) % 360f;
            boolean tiltedNow = Math.abs(pitch) > 55f || Math.abs(roll) > 55f;
            onHeading(trueHeading, tiltedNow);
        }
    }

    private void lowPass(float[] input, float[] output, boolean initialized) {
        for (int i = 0; i < 3; i++) {
            output[i] = initialized ? output[i] + ALPHA * (input[i] - output[i]) : input[i];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD || tvAccuracy == null) return;
        String label;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:   label = "Tinggi"; break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM: label = "Sedang"; break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:    label = "Rendah"; break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:      label = "Tidak andal"; break;
            default:                                          label = "-"; break;
        }
        tvAccuracy.setText(label);
        boolean needCalibration = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                || accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE;
        cardCalibration.setVisibility(needCalibration ? View.VISIBLE : View.GONE);
    }

    private void onHeading(float heading, boolean tiltedNow) {
        lastHeading = heading;
        tilted = tiltedNow;
        compass.setHeading(heading);

        long now = SystemClock.uptimeMillis();
        if (now - lastTextUpdate < 100) return;   // update teks maksimal 10x/detik
        lastTextUpdate = now;

        String[] names = {"U", "TL", "T", "TG", "S", "BD", "B", "BL"};
        int deg = Math.round(heading) % 360;
        tvHeading.setText(String.format(Locale.getDefault(), "%d° %s", deg, names[Math.round(heading / 45f) % 8]));

        if (!Double.isNaN(qiblaBearing)) {
            float diff = CompassView.wrap180((float) qiblaBearing - heading);
            tvDiff.setText(String.format(Locale.getDefault(), "%.0f° ke %s",
                    Math.abs(diff), diff >= 0 ? "kanan" : "kiri"));
        }
        updateStatusChip();
    }

    private void updateStatusChip() {
        if (tvStatus == null) return;
        String text;
        int role;
        if (accelerometer == null || magnetometer == null) {
            text = "Perangkat tidak punya sensor kompas";
            role = ThemedTextView.ROLE_SECONDARY;
        } else if (tilted) {
            text = "Letakkan HP mendatar (datar)";
            role = ThemedTextView.ROLE_SECONDARY;
        } else if (Double.isNaN(qiblaBearing)) {
            text = "Menunggu lokasi…";
            role = ThemedTextView.ROLE_SECONDARY;
        } else if (compass.isAligned()) {
            text = "Menghadap Kiblat ✓";
            role = ThemedTextView.ROLE_SUCCESS;
        } else {
            float diff = CompassView.wrap180((float) qiblaBearing - lastHeading);
            text = String.format(Locale.getDefault(), "Putar ke %s %.0f°",
                    diff >= 0 ? "kanan" : "kiri", Math.abs(diff));
            role = ThemedTextView.ROLE_ACCENT;
        }
        tvStatus.setText(text);
        tvStatus.setRole(role);
    }

    // ------------------------------------------------------------- location

    private boolean hasLocationPermission() {
        Context ctx = requireContext();
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locationManager == null || !hasLocationPermission() || updatingLocation) return;

        // 1) pakai lokasi terakhir yang diketahui supaya langsung muncul
        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                if (!locationManager.getAllProviders().contains(provider)) continue;
                Location l = locationManager.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        if (best != null) applyLocation(best, sourceName(best.getProvider()), true);

        // 2) minta update baru
        boolean any = false;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (locationManager.getAllProviders().contains(provider)
                        && locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 2000L, 1f, this);
                    any = true;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        updatingLocation = any;
        if (!any && best == null && Double.isNaN(qiblaBearing)) {
            tvLocation.setText(R.string.status_gps_off);
        } else if (any && Double.isNaN(qiblaBearing)) {
            tvLocation.setText(R.string.status_searching_location);
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) locationManager.removeUpdates(this);
        updatingLocation = false;
    }

    private void refreshLocation() {
        manualMode = false;
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        stopLocationUpdates();
        startLocationUpdates();
        Toast.makeText(requireContext(), "Memperbarui lokasi…", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (manualMode) return;
        applyLocation(location, sourceName(location.getProvider()), true);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    private String sourceName(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            return "GPS";
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            return "Jaringan";
        } else {
            return "Lokasi";
        }
    }

    private void applyLocation(Location loc, String source, boolean save) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();

        qiblaBearing = QiblaCalculator.bearing(lat, lon);
        double km = QiblaCalculator.distanceKm(lat, lon);
        declination = new GeomagneticField((float) lat, (float) lon,
                (float) loc.getAltitude(), System.currentTimeMillis()).getDeclination();

        compass.setQibla((float) qiblaBearing);
        tvQibla.setText(String.format(Locale.getDefault(), "%.1f°", qiblaBearing));
        tvDistance.setText(String.format(Locale.getDefault(), "%,.0f km", km));
        tvLocation.setText(String.format(Locale.US, "%.4f, %.4f · %s", lat, lon, source));
        updateStatusChip();

        if (save) {
            requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("lat", "" + lat)
                    .putString("lon", "" + lon)
                    .apply();
        }
    }

    private void loadSavedLocation() {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lat = sp.getString("lat", null);
        String lon = sp.getString("lon", null);
        if (lat == null || lon == null) return;
        try {
            Location l = new Location("saved");
            l.setLatitude(Double.parseDouble(lat));
            l.setLongitude(Double.parseDouble(lon));
            applyLocation(l, "Tersimpan", false);
        } catch (NumberFormatException ignored) {
        }
    }

    // -------------------------------------------------------- manual coords

    private void showManualDialog() {
        Context ctx = requireContext();
        float d = getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (20 * d), (int) (8 * d), (int) (20 * d), 0);

        int type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED;
        EditText etLat = new EditText(ctx);
        etLat.setHint("Latitude (contoh: -6.2000)");
        etLat.setInputType(type);
        EditText etLon = new EditText(ctx);
        etLon.setHint("Longitude (contoh: 106.8167)");
        etLon.setInputType(type);
        box.addView(etLat);
        box.addView(etLon);

        new AlertDialog.Builder(ctx)
                .setTitle("Atur Lokasi Manual")
                .setView(box)
                .setPositiveButton("Terapkan", (dialog, which) -> {
                    try {
                        double lat = Double.parseDouble(etLat.getText().toString().trim().replace(',', '.'));
                        double lon = Double.parseDouble(etLon.getText().toString().trim().replace(',', '.'));
                        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                            throw new NumberFormatException("range");
                        }
                        manualMode = true;
                        stopLocationUpdates();
                        Location l = new Location("manual");
                        l.setLatitude(lat);
                        l.setLongitude(lon);
                        applyLocation(l, "Manual", true);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "Koordinat tidak valid", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}