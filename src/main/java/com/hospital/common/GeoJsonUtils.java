package com.hospital.common;

import org.locationtech.jts.geom.Point;

public final class GeoJsonUtils {

    private GeoJsonUtils() {}

    public static double[] toGeoJson(Point p) {
        return new double[]{p.getX(), p.getY()};
    }
}
