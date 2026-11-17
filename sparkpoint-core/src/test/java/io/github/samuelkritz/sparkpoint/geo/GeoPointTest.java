package io.github.samuelkritz.sparkpoint.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GeoPointTest {

    @Test
    void keepsTheCoordinatesItWasGiven() {
        GeoPoint chico = new GeoPoint(39.7285, -121.8375);

        assertThat(chico.latitude()).isEqualTo(39.7285);
        assertThat(chico.longitude()).isEqualTo(-121.8375);
    }

    @ParameterizedTest
    @CsvSource({
        "90.0, 180.0",
        "-90.0, -180.0",
        "0.0, 0.0",
    })
    void acceptsCoordinatesOnTheEdgesOfTheValidRange(double latitude, double longitude) {
        GeoPoint point = new GeoPoint(latitude, longitude);

        assertThat(point.latitude()).isEqualTo(latitude);
        assertThat(point.longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest
    @ValueSource(doubles = {90.0001, -90.0001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsLatitudeOutsideMinus90To90(double latitude) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GeoPoint(latitude, 0.0))
                .withMessageContaining("latitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {180.0001, -180.0001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsLongitudeOutsideMinus180To180(double longitude) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GeoPoint(0.0, longitude))
                .withMessageContaining("longitude");
    }

    @Test
    void pointsWithTheSameCoordinatesAreEqual() {
        assertThat(new GeoPoint(39.7285, -121.8375)).isEqualTo(new GeoPoint(39.7285, -121.8375));
    }
}
