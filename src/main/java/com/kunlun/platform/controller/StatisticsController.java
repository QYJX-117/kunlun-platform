package com.kunlun.platform.controller;

import com.kunlun.platform.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/inventory-health")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getInventoryHealth(
            @RequestParam(required = false) String area) {
        List<Map<String, Object>> data = statisticsService.getInventoryHealth(area);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending-orders-count")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getPendingOrdersCount() {
        long count = statisticsService.getPendingOrdersCount();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", count);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/failure-ranking")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getFailureRanking() {
        List<Map<String, Object>> data = statisticsService.getFailureRanking();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sales-trend")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getSalesTrend(
            @RequestParam Long stationId,
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> data = statisticsService.getSalesTrend(stationId, days);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'STATION_MASTER', 'MAINTAINER')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> data = statisticsService.getDashboard();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inventory-search")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'STATION_MASTER')")
    public ResponseEntity<Map<String, Object>> searchInventory(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long stationId) {
        List<Map<String, Object>> data = statisticsService.searchInventory(area, stationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stations")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER', 'STATION_MASTER', 'MAINTAINER')")
    public ResponseEntity<Map<String, Object>> getStations() {
        List<Map<String, Object>> data = statisticsService.getStations();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("message", "服务器内部错误: " + e.getMessage());
        return ResponseEntity.status(500).body(result);
    }
}
