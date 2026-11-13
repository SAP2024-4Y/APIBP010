package com.hospital.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GeoJsonUtilsTest {

    private final GeometryFactory factory = new GeometryFactory();

    @Test
    void toGeoJsonReturnsLongitudeFirst() {
        // JTS Coordinate: x=longitude, y=latitude
        Point p = factory.createPoint(new Coordinate(77.5946, 12.9716));
        double[] result = GeoJsonUtils.toGeoJson(p);
        assertThat(result[0]).isEqualTo(77.5946);
    }

    @Test
    void toGeoJsonReturnsLatitudeSecond() {
        Point p = factory.createPoint(new Coordinate(77.5946, 12.9716));
        double[] result = GeoJsonUtils.toGeoJson(p);
        assertThat(result[1]).isEqualTo(12.9716);
    }

    @Test
    void toGeoJsonReturnsTwoElementArray() {
        Point p = factory.createPoint(new Coordinate(0.0, 0.0));
        assertThat(GeoJsonUtils.toGeoJson(p)).hasSize(2);
    }

    @Test
    void toGeoJsonPreservesNegativeCoordinates() {
        Point p = factory.createPoint(new Coordinate(-73.9857, 40.7484));
        double[] result = GeoJsonUtils.toGeoJson(p);
        assertThat(result[0]).isEqualTo(-73.9857);
        assertThat(result[1]).isEqualTo(40.7484);
    }
}
