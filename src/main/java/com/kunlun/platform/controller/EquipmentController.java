package com.kunlun.platform.controller;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.EquipmentRepository;
import com.kunlun.platform.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;

    public EquipmentController(EquipmentService equipmentService,
                               EquipmentRepository equipmentRepository) {
        this.equipmentService = equipmentService;
        this.equipmentRepository = equipmentRepository;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('STATION_MASTER', 'MAINTAINER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long stationId) {
        List<Equipment> equipment;
        if (stationId != null) {
            equipment = equipmentRepository.findByStationId(stationId);
        } else {
            equipment = equipmentRepository.findAll();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", equipment);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('STATION_MASTER')")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        Long equipmentId = Long.valueOf(body.get("equipmentId").toString());
        String description = (String) body.get("description");
        String priority = (String) body.getOrDefault("priority", "MEDIUM");
        String reporter = (String) body.get("reporter");
        MaintenanceOrder order = equipmentService.createMaintenanceOrder(equipmentId, description, priority, reporter);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", order);
        result.put("message", "工单已创建");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/orders/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> assignOrder(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> body) {
        Long workerId = Long.valueOf(body.get("workerId").toString());
        MaintenanceOrder order = equipmentService.assignOrder(id, workerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", order);
        result.put("message", "工单已派发");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/orders/{id}/start")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ResponseEntity<Map<String, Object>> startRepair(@PathVariable Long id) {
        MaintenanceOrder order = equipmentService.startRepair(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", order);
        result.put("message", "维修已开始");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/orders/{id}/complete")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ResponseEntity<Map<String, Object>> completeRepair(@PathVariable Long id,
                                                               @RequestBody Map<String, Object> body) {
        String maintenanceRecord = (String) body.get("maintenanceRecord");
        String consumedParts = (String) body.getOrDefault("consumedParts", "");
        MaintenanceOrder order = equipmentService.completeRepair(id, maintenanceRecord, consumedParts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", order);
        result.put("message", "维修已完成");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('STATION_MASTER', 'MAINTAINER', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long stationId) {
        List<MaintenanceOrder> orders = equipmentService.getOrders(status, stationId);
        List<Map<String, Object>> enriched = orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("equipmentId", o.getEquipmentId());
            Equipment eq = equipmentRepository.findById(o.getEquipmentId()).orElse(null);
            m.put("equipmentName", eq != null ? eq.getName() : "未知");
            m.put("description", o.getDescription());
            m.put("priority", o.getPriority());
            m.put("status", o.getStatus());
            m.put("reporter", o.getReporter());
            m.put("assignedTo", o.getAssignedTo());
            m.put("maintenanceRecord", o.getMaintenanceRecord());
            m.put("consumedParts", o.getConsumedParts());
            m.put("startTime", o.getStartTime());
            m.put("endTime", o.getEndTime());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", enriched);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/maintainers")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getMaintainers() {
        List<User> maintainers = equipmentService.getMaintainers();
        List<Map<String, Object>> list = maintainers.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getUsername());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", list);
        result.put("message", "success");
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 400);
        result.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("message", "服务器内部错误");
        return ResponseEntity.status(500).body(result);
    }
}
