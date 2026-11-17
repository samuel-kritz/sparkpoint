package io.github.samuelkritz.sparkpoint.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Expected values come from two sources that do not share code with the implementation: exact arc
 * lengths on the sphere (one degree of a great circle is R * pi / 180), and reference distances
 * computed separately with 3-D vector geometry.
 */
class GreatCircleTest {

    /** One metre. Far tighter than satellite fire detections, which are accurate to hundreds of metres. */
    private static final Offset<Double> ONE_METRE = within(0.001);

    private static final double ONE_DEGREE_KM = 111.19508;

    private static final GeoPoint ORIGIN = new GeoPoint(0, 0);
    private static final GeoPoint SEATTLE = new GeoPoint(47.6062, -122.3321);
    private static final GeoPoint CHICO = new GeoPoint(39.7285, -121.8375);
    private static final GeoPoint SACRAMENTO = new GeoPoint(38.5816, -121.4944);

    @Nested
    class DistanceBetweenTwoPoints {

        @Test
        void isZeroForTheSamePoint() {
            assertThat(GreatCircle.distanceKm(CHICO, CHICO)).isZero();
        }

        @Test
        void oneDegreeOfLatitudeIsAbout111Km() {
            assertThat(GreatCircle.distanceKm(ORIGIN, new GeoPoint(1, 0))).isCloseTo(ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void oneDegreeOfLongitudeAtTheEquatorIsAbout111Km() {
            assertThat(GreatCircle.distanceKm(ORIGIN, new GeoPoint(0, 1))).isCloseTo(ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void oneDegreeOfLongitudeShrinksAwayFromTheEquator() {
            double at60North = GreatCircle.distanceKm(new GeoPoint(60, 0), new GeoPoint(60, 1));

            // cos(60 degrees) is 0.5, so a degree of longitude is roughly half as long.
            assertThat(at60North).isCloseTo(ONE_DEGREE_KM / 2, within(0.01));
        }

        @Test
        void aQuarterOfTheWayAroundTheEquator() {
            assertThat(GreatCircle.distanceKm(ORIGIN, new GeoPoint(0, 90))).isCloseTo(10007.55722, ONE_METRE);
        }

        @Test
        void oppositeSidesOfTheEarth() {
            assertThat(GreatCircle.distanceKm(ORIGIN, new GeoPoint(0, 180))).isCloseTo(20015.11444, ONE_METRE);
        }

        @Test
        void takesTheShortWayAcrossTheAntimeridian() {
            GeoPoint justWestOfTheLine = new GeoPoint(0, 179.5);
            GeoPoint justEastOfTheLine = new GeoPoint(0, -179.5);

            assertThat(GreatCircle.distanceKm(justWestOfTheLine, justEastOfTheLine))
                    .isCloseTo(ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void matchesReferenceDistancesBetweenRealPlaces() {
            assertThat(GreatCircle.distanceKm(SEATTLE, CHICO)).isCloseTo(876.85909, ONE_METRE);
            assertThat(GreatCircle.distanceKm(CHICO, SACRAMENTO)).isCloseTo(130.91571, ONE_METRE);
        }

        @Test
        void isTheSameInBothDirections() {
            assertThat(GreatCircle.distanceKm(SEATTLE, CHICO))
                    .isCloseTo(GreatCircle.distanceKm(CHICO, SEATTLE), within(1e-9));
        }

        @Test
        void rejectsNullPoints() {
            assertThatNullPointerException().isThrownBy(() -> GreatCircle.distanceKm(null, CHICO));
            assertThatNullPointerException().isThrownBy(() -> GreatCircle.distanceKm(CHICO, null));
        }
    }

    @Nested
    class DistanceFromAPointToARoute {

        private static final GeoPoint ROUTE_START = new GeoPoint(0, 0);
        private static final GeoPoint ROUTE_END = new GeoPoint(0, 10);

        @Test
        void besideTheMiddleOfTheRouteIsThePerpendicularDistance() {
            GeoPoint oneDegreeNorthOfMidRoute = new GeoPoint(1, 5);

            assertThat(GreatCircle.distanceToSegmentKm(oneDegreeNorthOfMidRoute, ROUTE_START, ROUTE_END))
                    .isCloseTo(ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void isTheSameOnEitherSideOfTheRoute() {
            double north = GreatCircle.distanceToSegmentKm(new GeoPoint(1, 5), ROUTE_START, ROUTE_END);
            double south = GreatCircle.distanceToSegmentKm(new GeoPoint(-1, 5), ROUTE_START, ROUTE_END);

            assertThat(south).isCloseTo(north, within(1e-9));
        }

        @Test
        void onTheRouteIsZero() {
            assertThat(GreatCircle.distanceToSegmentKm(new GeoPoint(0, 4), ROUTE_START, ROUTE_END))
                    .isCloseTo(0.0, ONE_METRE);
        }

        @Test
        void atEitherEndOfTheRouteIsZero() {
            assertThat(GreatCircle.distanceToSegmentKm(ROUTE_START, ROUTE_START, ROUTE_END))
                    .isCloseTo(0.0, ONE_METRE);
            assertThat(GreatCircle.distanceToSegmentKm(ROUTE_END, ROUTE_START, ROUTE_END))
                    .isCloseTo(0.0, ONE_METRE);
        }

        @Test
        void pastTheEndOfTheRouteIsTheDistanceToTheEnd() {
            GeoPoint twoDegreesPastTheEnd = new GeoPoint(0, 12);

            assertThat(GreatCircle.distanceToSegmentKm(twoDegreesPastTheEnd, ROUTE_START, ROUTE_END))
                    .isCloseTo(2 * ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void beforeTheStartOfTheRouteIsTheDistanceToTheStart() {
            GeoPoint threeDegreesBeforeTheStart = new GeoPoint(0, -3);

            assertThat(GreatCircle.distanceToSegmentKm(threeDegreesBeforeTheStart, ROUTE_START, ROUTE_END))
                    .isCloseTo(3 * ONE_DEGREE_KM, ONE_METRE);
        }

        @Test
        void diagonallyBeforeTheStartIsTheDistanceToTheStartNotThePerpendicular() {
            GeoPoint northWestOfTheStart = new GeoPoint(1, -1);

            // The perpendicular to the extended line would be about 111 km; the route itself is further away.
            assertThat(GreatCircle.distanceToSegmentKm(northWestOfTheStart, ROUTE_START, ROUTE_END))
                    .isCloseTo(157.24960, ONE_METRE);
        }

        @Test
        void doesNotDependOnTheDirectionOfTheRoute() {
            GeoPoint point = new GeoPoint(1, -1);

            assertThat(GreatCircle.distanceToSegmentKm(point, ROUTE_END, ROUTE_START))
                    .isCloseTo(GreatCircle.distanceToSegmentKm(point, ROUTE_START, ROUTE_END), within(1e-6));
        }

        @Test
        void aRouteThatStartsAndEndsAtTheSamePlaceIsTreatedAsThatPlace() {
            assertThat(GreatCircle.distanceToSegmentKm(SEATTLE, CHICO, CHICO))
                    .isCloseTo(GreatCircle.distanceKm(SEATTLE, CHICO), ONE_METRE);
        }

        @Test
        void matchesReferenceDistancesForARealRoute() {
            GeoPoint eastOfTheRoute = new GeoPoint(39.2, -121.2);
            GeoPoint southEastOfSacramento = new GeoPoint(38.0, -121.0);

            assertThat(GreatCircle.distanceToSegmentKm(eastOfTheRoute, CHICO, SACRAMENTO))
                    .isCloseTo(40.14437, ONE_METRE);
            assertThat(GreatCircle.distanceToSegmentKm(southEastOfSacramento, CHICO, SACRAMENTO))
                    .isCloseTo(77.74370, ONE_METRE);
            assertThat(GreatCircle.distanceToSegmentKm(SEATTLE, CHICO, SACRAMENTO))
                    .isCloseTo(876.85909, ONE_METRE);
        }

        @Test
        void rejectsNullArguments() {
            assertThatNullPointerException()
                    .isThrownBy(() -> GreatCircle.distanceToSegmentKm(null, ROUTE_START, ROUTE_END));
            assertThatNullPointerException()
                    .isThrownBy(() -> GreatCircle.distanceToSegmentKm(CHICO, null, ROUTE_END));
            assertThatNullPointerException()
                    .isThrownBy(() -> GreatCircle.distanceToSegmentKm(CHICO, ROUTE_START, null));
        }
    }
}
