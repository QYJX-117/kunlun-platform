package com.kunlun.platform.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafetyStockCalculatorTest {

    @Test
    void testCalculateNormal() {
        // 7 天销量，均值 100，手动验证标准差
        double[] sales = {100.0, 110.0, 90.0, 105.0, 95.0, 115.0, 85.0};
        int leadTime = 4;
        double serviceLevel = 0.95; // Z=1.65

        double result = SafetyStockCalculator.calculate(sales, leadTime, serviceLevel);

        // mean = 700/7 = 100, sumSq = 600, stdDev = sqrt(600/6) = 10
        // safetyStock = 1.65 * 10 * sqrt(4) = 1.65 * 10 * 2 = 33.0
        assertEquals(33.0, result, 0.01);
    }

    @Test
    void testCalculateInsufficientData() {
        double[] sales = {100.0};
        assertEquals(0.0, SafetyStockCalculator.calculate(sales, 3, 0.95));
    }

    @Test
    void testCalculateNullInput() {
        assertEquals(0.0, SafetyStockCalculator.calculate(null, 3, 0.95));
    }

    @Test
    void testDifferentServiceLevels() {
        double[] sales = {100.0, 110.0, 90.0, 105.0};
        // serviceLevel 0.90, Z=1.28
        double result90 = SafetyStockCalculator.calculate(sales, 1, 0.90);
        // serviceLevel 0.99, Z=2.33
        double result99 = SafetyStockCalculator.calculate(sales, 1, 0.99);
        // 0.99 应有更高的安全库存
        assertTrue(result99 > result90);
    }
}
