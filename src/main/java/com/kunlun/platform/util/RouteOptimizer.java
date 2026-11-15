package com.kunlun.platform.util;

import com.kunlun.platform.entity.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RouteOptimizer {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizer.class);

    /**
     * 优化配送路线：贪心最近邻构造初始解 + 2-opt 局部优化。
     *
     * @param stations 待配送站点列表（无序）
     * @param depot    油库（起始点）
     * @return 优化后的站点顺序（不含 depot）
     */
    public static List<Station> optimizeRoute(List<Station> stations, Station depot) {
        if (stations == null || stations.isEmpty()) {
            return new ArrayList<>();
        }
        if (stations.size() == 1) {
            return new ArrayList<>(stations);
        }

        // 阶段一：贪心最近邻，使用 Set 跟踪已访问站点 ID，避免依赖 equals
        List<Station> route = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Station current = depot;

        while (visited.size() < stations.size()) {
            Station nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Station s : stations) {
                if (visited.contains(s.getId())) continue;
                double d = DistanceUtil.haversine(current, s);
                if (d < minDist) {
                    minDist = d;
                    nearest = s;
                }
            }
            visited.add(nearest.getId());
            route.add(nearest);
            current = nearest;
        }

        double greedyDist = totalDistanceWithDepot(route, depot);
        log.info("路线优化: 贪心初始距离={}km, 站点数={}", greedyDist, route.size());

        // 阶段二：2-opt 局部优化（开放路径，不包含回环）
        boolean improved = true;
        int iterations = 0;
        while (improved) {
            improved = false;
            iterations++;
            for (int i = 0; i < route.size() - 2; i++) {
                for (int j = i + 2; j < route.size() - 1; j++) {
                    Station a = route.get(i);
                    Station b = route.get(i + 1);
                    Station c = route.get(j);
                    Station d = route.get(j + 1);

                    double oldDist = DistanceUtil.haversine(a, b) + DistanceUtil.haversine(c, d);
                    double newDist = DistanceUtil.haversine(a, c) + DistanceUtil.haversine(b, d);

                    if (newDist < oldDist) {
                        reverse(route, i + 1, j);
                        improved = true;
                    }
                }
            }
        }

        double optimizedDist = totalDistanceWithDepot(route, depot);
        log.info("路线优化完成: 2-opt迭代{}次, 优化后距离={}km, 缩短={}km",
                iterations, optimizedDist, Math.round((greedyDist - optimizedDist) * 100.0) / 100.0);

        return route;
    }

    /**
     * 计算路径总长度（公里），含 depot→首站 + 站间距离。
     */
    public static double totalDistanceWithDepot(List<Station> route, Station depot) {
        if (route == null || route.isEmpty()) return 0.0;
        double total = DistanceUtil.haversine(depot, route.get(0));
        for (int i = 0; i < route.size() - 1; i++) {
            total += DistanceUtil.haversine(route.get(i), route.get(i + 1));
        }
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * 计算路径总长度（公里），不含 depot 的距离。
     */
    public static double totalDistance(List<Station> route) {
        if (route == null || route.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            total += DistanceUtil.haversine(route.get(i), route.get(i + 1));
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private static void reverse(List<Station> list, int from, int to) {
        while (from < to) {
            Collections.swap(list, from, to);
            from++;
            to--;
        }
    }
}
