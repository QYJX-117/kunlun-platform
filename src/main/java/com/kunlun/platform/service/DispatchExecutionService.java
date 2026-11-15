package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DispatchExecutionService {

    private final DispatchTaskRepository dispatchTaskRepository;
    private final TruckRepository truckRepository;
    private final PlannedOrderRepository plannedOrderRepository;
    private final StationInventoryRepository stationInventoryRepository;

    public DispatchExecutionService(DispatchTaskRepository dispatchTaskRepository,
                                    TruckRepository truckRepository,
                                    PlannedOrderRepository plannedOrderRepository,
                                    StationInventoryRepository stationInventoryRepository) {
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.truckRepository = truckRepository;
        this.plannedOrderRepository = plannedOrderRepository;
        this.stationInventoryRepository = stationInventoryRepository;
    }

    public DispatchTask depart(Long taskId) {
        DispatchTask task = dispatchTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("配送任务不存在: " + taskId));

        task.setActualDeparture(LocalDateTime.now());
        task.setStatus("ON_WAY");
        dispatchTaskRepository.save(task);

        Truck truck = truckRepository.findById(task.getTruckId())
                .orElseThrow(() -> new RuntimeException("关联车辆不存在: " + task.getTruckId()));
        truck.setStatus("ON_WAY");
        truckRepository.save(truck);

        return task;
    }

    @Transactional
    public DispatchTask arrive(Long taskId) {
        DispatchTask task = dispatchTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("配送任务不存在: " + taskId));

        task.setActualArrival(LocalDateTime.now());
        task.setStatus("COMPLETED");
        dispatchTaskRepository.save(task);

        PlannedOrder order = plannedOrderRepository.findById(task.getPlannedOrderId())
                .orElseThrow(() -> new RuntimeException("关联计划订单不存在: " + task.getPlannedOrderId()));

        StationInventory inventory = stationInventoryRepository
                .findByStationIdAndMaterialId(order.getStationId(), order.getMaterialId())
                .orElseThrow(() -> new RuntimeException("站点库存记录不存在"));

        inventory.setCurrentStock(inventory.getCurrentStock() + order.getSuggestedQuantity());
        inventory.setLastUpdated(LocalDateTime.now());
        stationInventoryRepository.save(inventory);

        order.setStatus("COMPLETED");
        plannedOrderRepository.save(order);

        List<DispatchTask> truckTasks = dispatchTaskRepository.findByTruckId(task.getTruckId());
        boolean hasOtherActive = truckTasks.stream()
                .anyMatch(t -> !t.getId().equals(task.getId()) && !"COMPLETED".equals(t.getStatus()));
        if (!hasOtherActive) {
            Truck truck = truckRepository.findById(task.getTruckId())
                    .orElseThrow(() -> new RuntimeException("关联车辆不存在: " + task.getTruckId()));
            truck.setStatus("IDLE");
            truckRepository.save(truck);
        }

        return task;
    }
}
