package com.kunlun.platform.util;

import java.util.Map;

public class SafetyStockCalculator {

    private static final Map<Double, Double> SERVICE_LEVEL_Z = Map.of(
            0.90, 1.28,
            0.95, 1.65,
            0.99, 2.33
    );

    /**
     * 计算安全库存。
     * 公式: safetyStock = Z * σ * √leadTime
     * σ 为样本标准差（分母 n-1）
     *
     * @param dailySales   最近 N 天的日销量
     * @param leadTime     提前期（天）
     * @param serviceLevel 服务水平（如 0.95）
     * @return 安全库存量；数据不足 2 条返回 0
     */
    public static double calculate(double[] dailySales, int leadTime, double serviceLevel) {
        if (dailySales == null || dailySales.length < 2) {
            return 0.0;
        }
        double z = SERVICE_LEVEL_Z.getOrDefault(serviceLevel, 1.65);
        double mean = 0.0;
        for (double v : dailySales) {
            mean += v;
        }
        mean /= dailySales.length;
        double sumSq = 0.0;
        for (double v : dailySales) {
            sumSq += (v - mean) * (v - mean);
        }
        double stdDev = Math.sqrt(sumSq / (dailySales.length - 1));
        return z * stdDev * Math.sqrt(leadTime);
    }
}
