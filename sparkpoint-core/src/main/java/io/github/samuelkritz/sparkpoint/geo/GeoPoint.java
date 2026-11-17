package io.github.samuelkritz.sparkpoint.geo;

/**
 * A position on the Earth's surface in decimal degrees (WGS 84, the datum used by
 * both NASA FIRMS and EPA AirNow).
 *
 * @param latitude  degrees north of the equator, from -90 to 90 inclusive
 * @param longitude degrees east of the prime meridian, from -180 to 180 inclusive
 */
public record GeoPoint(double latitude, double longitude) {

    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    /**
     * @throws IllegalArgumentException if either coordinate is outside its range or is not a number
     */
    public GeoPoint {
        requireWithin("latitude", latitude, MAX_LATITUDE);
        requireWithin("longitude", longitude, MAX_LONGITUDE);
    }

    private static void requireWithin(String name, double value, double limit) {
        // Written as a negated "in range" test so that NaN, which fails every comparison, is rejected.
        if (!(value >= -limit && value <= limit)) {
            throw new IllegalArgumentException(
                    name + " must be between " + -limit + " and " + limit + " degrees, but was " + value);
        }
    }
}
