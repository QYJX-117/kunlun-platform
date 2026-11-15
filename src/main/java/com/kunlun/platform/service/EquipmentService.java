package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final UserRepository userRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            MaintenanceOrderRepository maintenanceOrderRepository,
                            UserRepository userRepository) {
        this.equipmentRepository = equipmentRepository;
        this.maintenanceOrderRepository = maintenanceOrderRepository;
        this.userRepository = userRepository;
    }

    public MaintenanceOrder createMaintenanceOrder(Long equipmentId, String description,
                                                    String priority, String reporter) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("设备不存在: " + equipmentId));

        MaintenanceOrder order = new MaintenanceOrder();
        order.setEquipmentId(equipmentId);
        order.setDescription(description);
        order.setPriority(priority);
        order.setReporter(reporter);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        return maintenanceOrderRepository.save(order);
    }

    public MaintenanceOrder assignOrder(Long orderId, Long workerId) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + orderId));

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + workerId));

        if (!"MAINTAINER".equalsIgnoreCase(worker.getRole())) {
            throw new RuntimeException("该用户不是维修员，无法派单");
        }

        order.setAssignedTo(worker.getUsername());
        order.setStatus("ASSIGNED");
        return maintenanceOrderRepository.save(order);
    }

    public MaintenanceOrder startRepair(Long orderId) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + orderId));

        order.setStartTime(LocalDateTime.now());
        order.setStatus("IN_PROGRESS");
        return maintenanceOrderRepository.save(order);
    }

    @Transactional
    public MaintenanceOrder completeRepair(Long orderId, String maintenanceRecord, String consumedParts) {
        MaintenanceOrder order = maintenanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + orderId));

        order.setMaintenanceRecord(maintenanceRecord);
        order.setConsumedParts(consumedParts);
        order.setEndTime(LocalDateTime.now());
        order.setStatus("COMPLETED");
        maintenanceOrderRepository.save(order);

        Equipment equipment = equipmentRepository.findById(order.getEquipmentId()).orElse(null);
        if (equipment != null && "FAULT".equals(equipment.getStatus())) {
            equipment.setStatus("NORMAL");
            equipmentRepository.save(equipment);
        }

        return order;
    }

    public List<MaintenanceOrder> getOrders(String status, Long stationId) {
        List<MaintenanceOrder> orders;
        if (status != null && !status.isEmpty()) {
            orders = maintenanceOrderRepository.findByStatus(status);
        } else {
            orders = maintenanceOrderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (stationId != null) {
            orders = orders.stream()
                    .filter(o -> {
                        Equipment eq = equipmentRepository.findById(o.getEquipmentId()).orElse(null);
                        return eq != null && stationId.equals(eq.getStationId());
                    })
                    .toList();
        }

        return orders;
    }

    public List<User> getMaintainers() {
        return userRepository.findByRole("MAINTAINER");
    }
}
