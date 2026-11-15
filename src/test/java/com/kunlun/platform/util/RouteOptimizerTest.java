package com.kunlun.platform.util;

import com.kunlun.platform.entity.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizerTest {

    private Station depot;
    private Station s1, s2, s3;

    @BeforeEach
    void setUp() {
        depot = new Station();
        depot.setId(0L);
        depot.setName("油库");
        depot.setLongitude(new BigDecimal("103.718"));
        depot.setLatitude(new BigDecimal("36.099"));

        s1 = new Station();
        s1.setId(1L);
        s1.setName("站点A");
        s1.setLongitude(new BigDecimal("103.830"));
        s1.setLatitude(new BigDecimal("36.058"));

        s2 = new Station();
        s2.setId(2L);
        s2.setName("站点B");
        s2.setLongitude(new BigDecimal("103.720"));
        s2.setLatitude(new BigDecimal("36.150"));

        s3 = new Station();
        s3.setId(3L);
        s3.setName("站点C");
        s3.setLongitude(new BigDecimal("103.650"));
        s3.setLatitude(new BigDecimal("36.070"));
    }

    @Test
    void testOptimizeRoute_normalCase() {
        List<Station> stations = new ArrayList<>(List.of(s1, s2, s3));
        List<Station> original = new ArrayList<>(stations);

        List<Station> optimized = RouteOptimizer.optimizeRoute(stations, depot);

        assertEquals(3, optimized.size());
        assertTrue(optimized.containsAll(original));
    }

    @Test
    void testOptimizeRoute_emptyList() {
        List<Station> result = RouteOptimizer.optimizeRoute(Collections.emptyList(), depot);
        assertTrue(result.isEmpty());
    }

    @Test
    void testOptimizeRoute_singleStation() {
        List<Station> result = RouteOptimizer.optimizeRoute(Collections.singletonList(s1), depot);
        assertEquals(1, result.size());
        assertEquals(s1, result.get(0));
    }

    @Test
    void testTotalDistance() {
        List<Station> route = List.of(s1, s2, s3);
        double dist = RouteOptimizer.totalDistance(route);
        assertTrue(dist > 0);
    }

    @Test
    void testTotalDistance_shortRoute() {
        assertEquals(0.0, RouteOptimizer.totalDistance(Collections.singletonList(s1)));
        assertEquals(0.0, RouteOptimizer.totalDistance(null));
    }
}
