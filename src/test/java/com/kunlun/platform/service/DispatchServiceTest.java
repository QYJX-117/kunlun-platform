package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private PlannedOrderRepository plannedOrderRepository;

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private DispatchTaskRepository dispatchTaskRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private DispatchService dispatchService;

    private Station station;
    private Truck truck;
    private PlannedOrder order;

    @BeforeEach
    void setUp() {
        station = new Station();
        station.setId(1L);
        station.setName("兰州站A");
        station.setArea("兰州");
        station.setLongitude(new BigDecimal("103.830"));
        station.setLatitude(new BigDecimal("36.058"));

        truck = new Truck();
        truck.setId(1L);
        truck.setPlateNumber("甘A12345");
        truck.setDriverName("张三");
        truck.setStatus("IDLE");

        order = new PlannedOrder();
        order.setId(1L);
        order.setStationId(1L);
        order.setMaterialId(1L);
        order.setSuggestedQuantity(1000);
        order.setStatus("PENDING");
    }

    @Test
    void testGenerateDispatchTasks_withPendingOrders() {
        when(plannedOrderRepository.findByStatus("PENDING")).thenReturn(List.of(order));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(truckRepository.findByStatus("IDLE")).thenReturn(List.of(truck));
        when(dispatchTaskRepository.save(any(DispatchTask.class))).thenAnswer(inv -> {
            DispatchTask t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(plannedOrderRepository.save(any(PlannedOrder.class))).thenReturn(order);

        List<DispatchTask> tasks = dispatchService.generateDispatchTasks();

        assertEquals(1, tasks.size());
        assertEquals("ASSIGNED", tasks.get(0).getStatus());
        assertEquals("ON_WAY", truck.getStatus());
        assertEquals("CONFIRMED", order.getStatus());
        verify(truckRepository).save(truck);
        verify(plannedOrderRepository).save(order);
    }

    @Test
    void testGenerateDispatchTasks_noPendingOrders() {
        when(plannedOrderRepository.findByStatus("PENDING")).thenReturn(Collections.emptyList());

        List<DispatchTask> tasks = dispatchService.generateDispatchTasks();

        assertTrue(tasks.isEmpty());
        verify(dispatchTaskRepository, never()).save(any());
    }

    @Test
    void testGenerateDispatchTasks_noIdleTrucks() {
        when(plannedOrderRepository.findByStatus("PENDING")).thenReturn(List.of(order));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(truckRepository.findByStatus("IDLE")).thenReturn(Collections.emptyList());

        List<DispatchTask> tasks = dispatchService.generateDispatchTasks();

        assertTrue(tasks.isEmpty());
        verify(dispatchTaskRepository, never()).save(any());
    }
}
