package com.kunlun.platform.controller;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import com.kunlun.platform.service.DispatchService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final TruckRepository truckRepository;
    private final StationRepository stationRepository;
    private final MaterialRepository materialRepository;
    private final PlannedOrderRepository plannedOrderRepository;

    public DispatchController(DispatchService dispatchService,
                              DispatchTaskRepository dispatchTaskRepository,
                              TruckRepository truckRepository,
                              StationRepository stationRepository,
                              MaterialRepository materialRepository,
                              PlannedOrderRepository plannedOrderRepository) {
        this.dispatchService = dispatchService;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.truckRepository = truckRepository;
        this.stationRepository = stationRepository;
        this.materialRepository = materialRepository;
        this.plannedOrderRepository = plannedOrderRepository;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<Map<String, Object>> generate() {
        List<DispatchTask> tasks = dispatchService.generateDispatchTasks();
        List<Map<String, Object>> enriched = enrichTasks(tasks);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", enriched);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getTasks(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String date) {
        List<DispatchTask> tasks = dispatchTaskRepository.findAll(
                Sort.by(Sort.Direction.DESC, "id"));
        List<Map<String, Object>> enriched = enrichTasks(tasks);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", enriched);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/route-plan")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getRoutePlan() {
        List<DispatchTask> tasks = dispatchTaskRepository.findAll(
                Sort.by(Sort.Direction.ASC, "truckId", "routeSequence"));

        if (tasks.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", 200);
            result.put("data", Collections.emptyList());
            result.put("message", "success");
            return ResponseEntity.ok(result);
        }

        Map<Long, List<DispatchTask>> truckGroups = tasks.stream()
                .collect(Collectors.groupingBy(DispatchTask::getTruckId, LinkedHashMap::new, Collectors.toList()));

        Set<Long> orderIds = tasks.stream().map(DispatchTask::getPlannedOrderId).collect(Collectors.toSet());
        Map<Long, PlannedOrder> orderMap = plannedOrderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(PlannedOrder::getId, o -> o));
        Set<Long> stationIds = orderMap.values().stream().map(PlannedOrder::getStationId).collect(Collectors.toSet());
        Map<Long, Station> stationMap = stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, s -> s));
        Map<Long, Truck> truckMap = truckRepository.findAllById(truckGroups.keySet()).stream()
                .collect(Collectors.toMap(Truck::getId, t -> t));

        // Depot: 兰州西固油库
        final double depotLng = 103.718;
        final double depotLat = 36.099;

        List<Map<String, Object>> routes = new ArrayList<>();
        for (Map.Entry<Long, List<DispatchTask>> entry : truckGroups.entrySet()) {
            Long truckId = entry.getKey();
            List<DispatchTask> truckTasks = entry.getValue();
            Truck truck = truckMap.get(truckId);

            List<Map<String, Object>> waypoints = new ArrayList<>();
            for (DispatchTask task : truckTasks) {
                PlannedOrder order = orderMap.get(task.getPlannedOrderId());
                if (order != null) {
                    Station station = stationMap.get(order.getStationId());
                    if (station != null && station.getLongitude() != null && station.getLatitude() != null) {
                        Map<String, Object> wp = new LinkedHashMap<>();
                        wp.put("name", station.getName());
                        wp.put("lng", station.getLongitude().doubleValue());
                        wp.put("lat", station.getLatitude().doubleValue());
                        wp.put("sequence", task.getRouteSequence());
                        waypoints.add(wp);
                    }
                }
            }

            waypoints.sort(Comparator.comparingInt(w -> (Integer) w.get("sequence")));

            double totalDistance = 0;
            if (!waypoints.isEmpty()) {
                Map<String, Object> firstWp = waypoints.get(0);
                totalDistance += com.kunlun.platform.util.DistanceUtil.haversine(
                        depotLat, depotLng,
                        (Double) firstWp.get("lat"), (Double) firstWp.get("lng"));
                for (int i = 1; i < waypoints.size(); i++) {
                    Map<String, Object> prev = waypoints.get(i - 1);
                    Map<String, Object> curr = waypoints.get(i);
                    totalDistance += com.kunlun.platform.util.DistanceUtil.haversine(
                            (Double) prev.get("lat"), (Double) prev.get("lng"),
                            (Double) curr.get("lat"), (Double) curr.get("lng"));
                }
            }

            Map<String, Object> route = new LinkedHashMap<>();
            route.put("truckId", truckId);
            route.put("truck", truck != null ? truck.getPlateNumber() : "未知");
            route.put("driver", truck != null ? truck.getDriverName() : "未知");
            route.put("depotLng", depotLng);
            route.put("depotLat", depotLat);
            route.put("waypoints", waypoints);
            route.put("distance", String.format("%.1f", totalDistance));
            route.put("duration", String.format("%.0f", totalDistance / 40.0 * 60));
            routes.add(route);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", routes);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    private List<Map<String, Object>> enrichTasks(List<DispatchTask> tasks) {
        Set<Long> truckIds = tasks.stream().map(DispatchTask::getTruckId).collect(Collectors.toSet());
        Set<Long> orderIds = tasks.stream().map(DispatchTask::getPlannedOrderId).collect(Collectors.toSet());

        Map<Long, Truck> truckMap = truckRepository.findAllById(truckIds).stream()
                .collect(Collectors.toMap(Truck::getId, t -> t));

        Map<Long, PlannedOrder> orderMap = plannedOrderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(PlannedOrder::getId, o -> o));

        Set<Long> stationIds = orderMap.values().stream().map(PlannedOrder::getStationId).collect(Collectors.toSet());
        Set<Long> materialIds = orderMap.values().stream().map(PlannedOrder::getMaterialId).collect(Collectors.toSet());

        Map<Long, String> stationNames = stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, Station::getName));
        Map<Long, String> materialNames = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        return tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("plannedOrderId", t.getPlannedOrderId());
            m.put("truckId", t.getTruckId());
            m.put("routeSequence", t.getRouteSequence());
            m.put("status", t.getStatus());

            Truck truck = truckMap.get(t.getTruckId());
            m.put("plateNumber", truck != null ? truck.getPlateNumber() : "未知");
            m.put("driverName", truck != null ? truck.getDriverName() : "未知");

            PlannedOrder order = orderMap.get(t.getPlannedOrderId());
            if (order != null) {
                m.put("stationName", stationNames.getOrDefault(order.getStationId(), "未知"));
                m.put("materialName", materialNames.getOrDefault(order.getMaterialId(), "未知"));
            }
            return m;
        }).collect(Collectors.toList());
    }
}
