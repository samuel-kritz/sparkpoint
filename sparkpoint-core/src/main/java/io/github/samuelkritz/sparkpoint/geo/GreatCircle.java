package io.github.samuelkritz.sparkpoint.geo;

import java.util.Objects;

/**
 * Distances over the Earth's surface, treating the Earth as a sphere.
 *
 * <p>The spherical model is within about 0.5% of the true (ellipsoidal) distance, which is well
 * inside the accuracy of the inputs: satellite fire detections are located to a few hundred metres
 * at best, and rule thresholds are whole kilometres.
 *
 * <p>A route between two places is modelled as the great-circle segment joining them, the shortest
 * path over the surface. It is not the road a truck would actually drive.
 */
public final class GreatCircle {

    /** Mean Earth radius (IUGG), in kilometres. */
    static final double EARTH_RADIUS_KM = 6371.0088;

    private GreatCircle() {}

    /**
     * Returns the shortest distance over the surface between two points, in kilometres.
     */
    public static double distanceKm(GeoPoint from, GeoPoint to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return EARTH_RADIUS_KM * angularDistance(from, to);
    }

    /**
     * Returns the shortest distance, in kilometres, from a point to any part of the route that runs
     * from {@code segmentStart} to {@code segmentEnd}.
     *
     * <p>If the point lies beside the route, this is the perpendicular (cross-track) distance. If it
     * lies beyond either end, it is the distance to that end.
     *
     * <p>Intended for routes shorter than a quarter of the way around the Earth (about 10,000 km).
     */
    public static double distanceToSegmentKm(GeoPoint point, GeoPoint segmentStart, GeoPoint segmentEnd) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(segmentStart, "segmentStart");
        Objects.requireNonNull(segmentEnd, "segmentEnd");

        double startToPoint = angularDistance(segmentStart, point);
        double startToEnd = angularDistance(segmentStart, segmentEnd);
        if (startToPoint == 0.0 || startToEnd == 0.0) {
            // The point is at the start, or the route has no length: either way the start is the nearest place.
            return EARTH_RADIUS_KM * startToPoint;
        }

        // Angle at the start between the direction of the route and the direction of the point.
        double angleOffRoute = initialBearing(segmentStart, point) - initialBearing(segmentStart, segmentEnd);
        if (Math.cos(angleOffRoute) < 0.0) {
            // The point is behind the start, so the start is the nearest place on the route.
            return EARTH_RADIUS_KM * startToPoint;
        }

        // Cross-track: how far the point is from the full great circle the route lies on.
        double crossTrack = Math.asin(clampToUnit(Math.sin(startToPoint) * Math.sin(angleOffRoute)));
        // Along-track: how far along that circle, from the start, the point's nearest place is.
        double alongTrack = Math.acos(clampToUnit(Math.cos(startToPoint) / Math.cos(crossTrack)));
        if (alongTrack > startToEnd) {
            // The nearest place on the circle is past the end of the route.
            return distanceKm(point, segmentEnd);
        }
        return EARTH_RADIUS_KM * Math.abs(crossTrack);
    }

    /** Angle between two points as seen from the Earth's centre, in radians (haversine formula). */
    private static double angularDistance(GeoPoint from, GeoPoint to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double halfLatitudeDifference = (toLatitude - fromLatitude) / 2;
        double halfLongitudeDifference = Math.toRadians(to.longitude() - from.longitude()) / 2;

        double haversine = square(Math.sin(halfLatitudeDifference))
                + Math.cos(fromLatitude) * Math.cos(toLatitude) * square(Math.sin(halfLongitudeDifference));
        return 2 * Math.asin(Math.sqrt(clampToUnit(haversine)));
    }

    /** Compass direction to set off in from {@code from} to reach {@code to}, in radians clockwise from north. */
    private static double initialBearing(GeoPoint from, GeoPoint to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double longitudeDifference = Math.toRadians(to.longitude() - from.longitude());

        double east = Math.sin(longitudeDifference) * Math.cos(toLatitude);
        double north = Math.cos(fromLatitude) * Math.sin(toLatitude)
                - Math.sin(fromLatitude) * Math.cos(toLatitude) * Math.cos(longitudeDifference);
        return Math.atan2(east, north);
    }

    /** Guards asin and acos against inputs that rounding has pushed a hair outside [-1, 1]. */
    private static double clampToUnit(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private static double square(double value) {
        return value * value;
    }
}
