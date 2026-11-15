package com.kunlun.platform.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExponentialSmoothingTest {

    @Test
    void testForecast_normalSequence() {
        List<Double> sales = Arrays.asList(100.0, 120.0, 110.0, 130.0);
        double result = ExponentialSmoothing.forecast(sales, 0.3);
        // S_0=100, S_1=0.3*120+0.7*100=106, S_2=0.3*110+0.7*106=107.2, S_3=0.3*130+0.7*107.2=114.04
        assertEquals(114.04, result, 0.01);
    }

    @Test
    void testForecast_emptyList() {
        assertEquals(0.0, ExponentialSmoothing.forecast(Collections.emptyList(), 0.3));
    }

    @Test
    void testForecast_nullList() {
        assertEquals(0.0, ExponentialSmoothing.forecast(null, 0.3));
    }

    @Test
    void testForecast_singleElement() {
        assertEquals(50.0, ExponentialSmoothing.forecast(Collections.singletonList(50.0), 0.3));
    }

    @Test
    void testCompareAlpha_returnsValidAlpha() {
        List<Double> sales = Arrays.asList(100.0, 120.0, 110.0, 130.0, 125.0);
        double best = ExponentialSmoothing.compareAlpha(sales, new double[]{0.1, 0.3, 0.5});
        assertTrue(best >= 0.1 && best <= 0.5);
    }

    @Test
    void testCompareAlpha_insufficientData() {
        assertEquals(0.3, ExponentialSmoothing.compareAlpha(
                Collections.singletonList(100.0), new double[]{0.1, 0.3}));
    }
}
