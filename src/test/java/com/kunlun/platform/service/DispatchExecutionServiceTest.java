package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchExecutionServiceTest {

    @Mock
    private DispatchTaskRepository dispatchTaskRepository;
    @Mock
    private TruckRepository truckRepository;
    @Mock
    private PlannedOrderRepository plannedOrderRepository;
    @Mock
    private StationInventoryRepository stationInventoryRepository;

    private DispatchExecutionService service;

    @BeforeEach
    void setUp() {
        service = new DispatchExecutionService(
                dispatchTaskRepository, truckRepository,
                plannedOrderRepository, stationInventoryRepository);
    }

    @Test
    void arrive_shouldUpdateInventoryAndSetTruckIdle_whenNoOtherActiveTasks() {
        DispatchTask task = new DispatchTask();
        task.setId(1L);
        task.setTruckId(10L);
        task.setPlannedOrderId(100L);
        task.setStatus("ON_WAY");

        PlannedOrder order = new PlannedOrder();
        order.setId(100L);
        order.setStationId(200L);
        order.setMaterialId(300L);
        order.setSuggestedQuantity(500);
        order.setStatus("CONFIRMED");

        StationInventory inventory = new StationInventory();
        inventory.setId(1L);
        inventory.setStationId(200L);
        inventory.setMaterialId(300L);
        inventory.setCurrentStock(1000);
        inventory.setSafetyStock(200);

        Truck truck = new Truck();
        truck.setId(10L);
        truck.setStatus("ON_WAY");

        when(dispatchTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(plannedOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stationInventoryRepository.findByStationIdAndMaterialId(200L, 300L))
                .thenReturn(Optional.of(inventory));
        when(dispatchTaskRepository.findByTruckId(10L)).thenReturn(List.of(task));
        when(truckRepository.findById(10L)).thenReturn(Optional.of(truck));

        DispatchTask result = service.arrive(1L);

        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getActualArrival());
        verify(dispatchTaskRepository).save(task);
        verify(plannedOrderRepository).save(order);
        assertEquals("COMPLETED", order.getStatus());
        assertEquals(1500, inventory.getCurrentStock());
        assertEquals("IDLE", truck.getStatus());
        verify(truckRepository).save(truck);
    }

    @Test
    void arrive_shouldNotSetTruckIdle_whenOtherActiveTasksExist() {
        DispatchTask task = new DispatchTask();
        task.setId(1L);
        task.setTruckId(10L);
        task.setPlannedOrderId(100L);
        task.setStatus("ON_WAY");

        DispatchTask otherTask = new DispatchTask();
        otherTask.setId(2L);
        otherTask.setTruckId(10L);
        otherTask.setPlannedOrderId(101L);
        otherTask.setStatus("ASSIGNED");

        PlannedOrder order = new PlannedOrder();
        order.setId(100L);
        order.setStationId(200L);
        order.setMaterialId(300L);
        order.setSuggestedQuantity(300);

        StationInventory inventory = new StationInventory();
        inventory.setId(1L);
        inventory.setStationId(200L);
        inventory.setMaterialId(300L);
        inventory.setCurrentStock(1000);

        when(dispatchTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(plannedOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stationInventoryRepository.findByStationIdAndMaterialId(200L, 300L))
                .thenReturn(Optional.of(inventory));
        when(dispatchTaskRepository.findByTruckId(10L)).thenReturn(List.of(task, otherTask));

        service.arrive(1L);

        assertEquals("COMPLETED", task.getStatus());
        assertEquals(1300, inventory.getCurrentStock());
        verify(truckRepository, never()).findById(any());
        verify(truckRepository, never()).save(any());
    }

    @Test
    void arrive_shouldThrowException_whenTaskNotFound() {
        when(dispatchTaskRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.arrive(999L));
        assertTrue(ex.getMessage().contains("配送任务不存在"));
    }

    @Test
    void arrive_shouldThrowException_whenInventoryNotFound() {
        DispatchTask task = new DispatchTask();
        task.setId(1L);
        task.setPlannedOrderId(100L);

        PlannedOrder order = new PlannedOrder();
        order.setId(100L);
        order.setStationId(200L);
        order.setMaterialId(300L);

        when(dispatchTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(plannedOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stationInventoryRepository.findByStationIdAndMaterialId(200L, 300L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.arrive(1L));
        assertTrue(ex.getMessage().contains("站点库存记录不存在"));
    }

    @Test
    void depart_shouldUpdateTaskAndTruckStatus() {
        DispatchTask task = new DispatchTask();
        task.setId(1L);
        task.setTruckId(10L);
        task.setStatus("ASSIGNED");

        Truck truck = new Truck();
        truck.setId(10L);
        truck.setStatus("IDLE");

        when(dispatchTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(truckRepository.findById(10L)).thenReturn(Optional.of(truck));

        DispatchTask result = service.depart(1L);

        assertEquals("ON_WAY", result.getStatus());
        assertNotNull(result.getActualDeparture());
        assertEquals("ON_WAY", truck.getStatus());
        verify(dispatchTaskRepository).save(task);
        verify(truckRepository).save(truck);
    }
}
