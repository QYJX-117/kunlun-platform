package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final StationRepository stationRepository;
    private final StationInventoryRepository stationInventoryRepository;
    private final PlannedOrderRepository plannedOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final SalesHistoryRepository salesHistoryRepository;
    private final MaterialRepository materialRepository;
    private final TruckRepository truckRepository;
    private final DispatchTaskRepository dispatchTaskRepository;

    public StatisticsService(StationRepository stationRepository,
                             StationInventoryRepository stationInventoryRepository,
                             PlannedOrderRepository plannedOrderRepository,
                             EquipmentRepository equipmentRepository,
                             MaintenanceOrderRepository maintenanceOrderRepository,
                             SalesHistoryRepository salesHistoryRepository,
                             MaterialRepository materialRepository,
                             TruckRepository truckRepository,
                             DispatchTaskRepository dispatchTaskRepository) {
        this.stationRepository = stationRepository;
        this.stationInventoryRepository = stationInventoryRepository;
        this.plannedOrderRepository = plannedOrderRepository;
        this.equipmentRepository = equipmentRepository;
        this.maintenanceOrderRepository = maintenanceOrderRepository;
        this.salesHistoryRepository = salesHistoryRepository;
        this.materialRepository = materialRepository;
        this.truckRepository = truckRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
    }

    /**
     * 获取各站点库存健康度，坐标严格取自数据库 station 表。
     */
    public List<Map<String, Object>> getInventoryHealth(String area) {
        List<Station> stations;
        if (area != null && !area.isEmpty()) {
            stations = stationRepository.findByArea(area);
        } else {
            stations = stationRepository.findAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Station station : stations) {
            List<StationInventory> inventories = stationInventoryRepository.findByStationId(station.getId());
            int totalStock = 0;
            int totalSafety = 0;
            for (StationInventory inv : inventories) {
                totalStock += inv.getCurrentStock() != null ? inv.getCurrentStock() : 0;
                totalSafety += inv.getSafetyStock() != null ? inv.getSafetyStock() : 0;
            }

            String health;
            if (totalSafety == 0) {
                health = "GREEN";
            } else if (totalStock >= totalSafety * 2) {
                health = "GREEN";
            } else if (totalStock >= totalSafety) {
                health = "YELLOW";
            } else {
                health = "RED";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stationId", station.getId());
            item.put("name", station.getName());
            item.put("area", station.getArea());
            item.put("longitude", station.getLongitude());
            item.put("latitude", station.getLatitude());
            item.put("currentStock", totalStock);
            item.put("safetyStock", totalSafety);
            item.put("health", health);
            result.add(item);
        }
        return result;
    }

    public long getPendingOrdersCount() {
        return plannedOrderRepository.findByStatus("PENDING").size();
    }

    /**
     * 设备故障率排行，按维修工单数降序取前5。
     */
    public List<Map<String, Object>> getFailureRanking() {
        List<MaintenanceOrder> allOrders = maintenanceOrderRepository.findAll();
        Map<Long, Long> equipmentIdCount = allOrders.stream()
                .collect(Collectors.groupingBy(MaintenanceOrder::getEquipmentId, Collectors.counting()));

        Map<Long, String> equipmentNames = equipmentRepository.findAll().stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName, (a, b) -> a));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : equipmentIdCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("equipmentId", entry.getKey());
            item.put("name", equipmentNames.getOrDefault(entry.getKey(), "未知设备"));
            item.put("count", entry.getValue());
            ranking.add(item);
        }

        ranking.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return ranking.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 获取指定站点最近 N 天的销量趋势。
     */
    public List<Map<String, Object>> getSalesTrend(Long stationId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        List<SalesHistory> records = salesHistoryRepository
                .findByStationIdAndSaleDateBetween(stationId, start, end);

        Map<Long, String> materialNames = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        return records.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", r.getSaleDate().toString());
            item.put("materialName", materialNames.getOrDefault(r.getMaterialId(), "未知"));
            item.put("quantity", r.getQuantity());
            return item;
        }).sorted(Comparator.comparing(m -> (String) m.get("date"))).collect(Collectors.toList());
    }

    /**
     * 仪表盘概览数据。
     */
    public Map<String, Object> getDashboard() {
        long pendingOrders = plannedOrderRepository.findByStatus("PENDING").size();
        long uncompletedMaintenance = maintenanceOrderRepository.findAll().stream()
                .filter(o -> !"COMPLETED".equals(o.getStatus()))
                .count();
        long inTransitTrucks = truckRepository.findByStatus("ON_WAY").size();
        long todaySales = salesHistoryRepository.findAll().stream()
                .filter(s -> s.getSaleDate().equals(LocalDate.now()))
                .mapToLong(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingOrders", pendingOrders);
        result.put("uncompletedMaintenance", uncompletedMaintenance);
        result.put("inTransitTrucks", inTransitTrucks);
        result.put("todaySales", todaySales);
        return result;
    }

    /**
     * 库存明细搜索，按区域或站点筛选，包含每种油品的当前库存/安全库存/最大库存。
     */
    public List<Map<String, Object>> searchInventory(String area, Long stationId) {
        List<Station> stations;
        if (stationId != null) {
            Station s = stationRepository.findById(stationId).orElse(null);
            stations = s != null ? Collections.singletonList(s) : Collections.emptyList();
        } else if (area != null && !area.isEmpty()) {
            stations = stationRepository.findByArea(area);
        } else {
            stations = stationRepository.findAll();
        }

        Map<Long, String> materialNames = materialRepository.findAll().stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Station station : stations) {
            List<StationInventory> inventories = stationInventoryRepository.findByStationId(station.getId());
            for (StationInventory inv : inventories) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("stationId", station.getId());
                item.put("stationName", station.getName());
                item.put("area", station.getArea());
                item.put("materialId", inv.getMaterialId());
                item.put("materialName", materialNames.getOrDefault(inv.getMaterialId(), "未知"));
                item.put("currentStock", inv.getCurrentStock());
                item.put("safetyStock", inv.getSafetyStock());
                item.put("maxStock", inv.getMaxStock());
                result.add(item);
            }
        }
        return result;
    }

    public List<Map<String, Object>> getStations() {
        return stationRepository.findAll().stream().map(s -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getName());
            item.put("area", s.getArea());
            return item;
        }).collect(Collectors.toList());
    }
}
