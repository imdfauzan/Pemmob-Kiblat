package com.example.sensorkiblat;

/** Perhitungan arah Kiblat (initial bearing great-circle) dan jarak ke Ka'bah (haversine). */
public final class QiblaCalculator {

    public static final double KAABA_LAT = 21.4225;   // derajat LU
    public static final double KAABA_LON = 39.8262;   // derajat BT
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private QiblaCalculator() {}

    /** Sudut Kiblat dari utara SEJATI (0..360, searah jarum jam). */
    public static double bearing(double lat, double lon) {
        double phi1 = Math.toRadians(lat);
        double phi2 = Math.toRadians(KAABA_LAT);
        double dLon = Math.toRadians(KAABA_LON - lon);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);

        double degrees = Math.toDegrees(Math.atan2(y, x));
        return (degrees + 360.0) % 360.0;
    }

    /** Jarak great-circle ke Ka'bah dalam kilometer. */
    public static double distanceKm(double lat, double lon) {
        double phi1 = Math.toRadians(lat);
        double phi2 = Math.toRadians(KAABA_LAT);
        double dPhi = phi2 - phi1;
        double dLambda = Math.toRadians(KAABA_LON - lon);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
