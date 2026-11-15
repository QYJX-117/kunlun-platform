package com.kunlun.platform.controller;

import com.kunlun.platform.entity.Material;
import com.kunlun.platform.entity.PlannedOrder;
import com.kunlun.platform.entity.Station;
import com.kunlun.platform.entity.StationInventory;
import com.kunlun.platform.repository.MaterialRepository;
import com.kunlun.platform.repository.StationRepository;
import com.kunlun.platform.service.MrpService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mrp")
public class MrpController {

    private final MrpService mrpService;
    private final StationRepository stationRepository;
    private final MaterialRepository materialRepository;
    private final com.kunlun.platform.repository.PlannedOrderRepository plannedOrderRepository;
    private final com.kunlun.platform.repository.StationInventoryRepository stationInventoryRepository;

    public MrpController(MrpService mrpService,
                         StationRepository stationRepository,
                         MaterialRepository materialRepository,
                         com.kunlun.platform.repository.PlannedOrderRepository plannedOrderRepository,
                         com.kunlun.platform.repository.StationInventoryRepository stationInventoryRepository) {
        this.mrpService = mrpService;
        this.stationRepository = stationRepository;
        this.materialRepository = materialRepository;
        this.plannedOrderRepository = plannedOrderRepository;
        this.stationInventoryRepository = stationInventoryRepository;
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<Map<String, Object>> runMrp() {
        List<PlannedOrder> orders = mrpService.runMrp();
        List<Map<String, Object>> enriched = enrichOrders(orders);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", enriched);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/planned-orders")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getPlannedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PlannedOrder> pageResult = plannedOrderRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> enriched = enrichOrders(pageResult.getContent());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", enriched);
        result.put("total", pageResult.getTotalElements());
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/demand-summary")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getDemandSummary() {
        List<Station> stations = stationRepository.findAll();
        List<Material> materials = materialRepository.findAll();
        List<PlannedOrder> pendingOrders = plannedOrderRepository.findByStatus("PENDING");
        List<StationInventory> allInventories =
                stationInventoryRepository.findAll();

        Map<Long, String> materialNames = materials.stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        Map<Long, String> stationNames = stations.stream()
                .collect(Collectors.toMap(Station::getId, Station::getName));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Station station : stations) {
            for (Material material : materials) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("stationId", station.getId());
                item.put("stationName", station.getName());
                item.put("materialId", material.getId());
                item.put("materialName", material.getName());

                // current stock
                StationInventory inv = allInventories.stream()
                        .filter(i -> i.getStationId().equals(station.getId())
                                && i.getMaterialId().equals(material.getId()))
                        .findFirst().orElse(null);
                int currentStock = inv != null && inv.getCurrentStock() != null ? inv.getCurrentStock() : 0;
                int safetyStock = inv != null && inv.getSafetyStock() != null ? inv.getSafetyStock() : 0;

                item.put("currentStock", currentStock);
                item.put("safetyStock", safetyStock);
                item.put("stockGap", currentStock - safetyStock);
                item.put("forecastDaily", "-");

                result.add(item);
            }
        }

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("code", 200);
        wrapper.put("data", result);
        wrapper.put("message", "success");
        return ResponseEntity.ok(wrapper);
    }

    private List<Map<String, Object>> enrichOrders(List<PlannedOrder> orders) {
        Set<Long> stationIds = orders.stream().map(PlannedOrder::getStationId).collect(Collectors.toSet());
        Set<Long> materialIds = orders.stream().map(PlannedOrder::getMaterialId).collect(Collectors.toSet());

        Map<Long, String> stationNames = stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, Station::getName));
        Map<Long, String> materialNames = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        return orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("stationId", o.getStationId());
            m.put("stationName", stationNames.getOrDefault(o.getStationId(), "未知"));
            m.put("materialId", o.getMaterialId());
            m.put("materialName", materialNames.getOrDefault(o.getMaterialId(), "未知"));
            m.put("suggestedQuantity", o.getSuggestedQuantity());
            m.put("requiredDate", o.getRequiredDate());
            m.put("status", o.getStatus());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }
}
