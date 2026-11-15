package com.kunlun.platform.util;

import com.kunlun.platform.entity.Station;
import java.math.BigDecimal;

public class DistanceUtil {

    private static final double EARTH_RADIUS = 6371.0;

    /**
     * Haversine 公式计算两点间大圆距离（公里），保留 2 位小数。
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * 直接传入两个 Station 实体计算距离。
     */
    public static double haversine(Station a, Station b) {
        double lat1 = a.getLatitude().doubleValue();
        double lon1 = a.getLongitude().doubleValue();
        double lat2 = b.getLatitude().doubleValue();
        double lon2 = b.getLongitude().doubleValue();
        return haversine(lat1, lon1, lat2, lon2);
    }
}
