package com.kunlun.platform.util;

import java.util.List;

public class ExponentialSmoothing {

    /**
     * 指数平滑预测下一期日均销量。
     * 公式: S_t = α * y_t + (1-α) * S_{t-1}, 初始值 S_0 = y_0
     *
     * @param sales 历史日销量序列
     * @param alpha 平滑系数 (0 < alpha < 1)
     * @return 下一期预测值；空或null返回0.0
     */
    public static double forecast(List<Double> sales, double alpha) {
        if (sales == null || sales.isEmpty()) {
            return 0.0;
        }
        double smoothed = sales.get(0);
        for (int i = 1; i < sales.size(); i++) {
            smoothed = alpha * sales.get(i) + (1 - alpha) * smoothed;
        }
        return smoothed;
    }

    /**
     * 比较不同 alpha 的预测效果，返回最优 alpha。
     * 对每个 alpha，用前 n-1 天数据预测第 n 天，计算误差平方和，取误差最小的 alpha。
     *
     * @param sales  历史日销量序列（长度 >= 2）
     * @param alphas 待比较的 alpha 数组
     * @return 最优 alpha；sales 不足 2 条返回 0.3
     */
    public static double compareAlpha(List<Double> sales, double[] alphas) {
        if (sales == null || sales.size() < 2 || alphas == null || alphas.length == 0) {
            return 0.3;
        }
        double bestAlpha = alphas[0];
        double minError = Double.MAX_VALUE;
        for (double alpha : alphas) {
            double sse = 0.0;
            double smoothed = sales.get(0);
            // 逐步递推，每一步预测下一天，累加误差平方
            for (int i = 1; i < sales.size(); i++) {
                smoothed = alpha * sales.get(i - 1) + (1 - alpha) * smoothed;
                double error = sales.get(i) - smoothed;
                sse += error * error;
            }
            if (sse < minError) {
                minError = sse;
                bestAlpha = alpha;
            }
        }
        return bestAlpha;
    }
}
