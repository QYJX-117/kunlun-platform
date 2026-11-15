package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private MaintenanceOrderRepository maintenanceOrderRepository;
    @Mock
    private UserRepository userRepository;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepository, maintenanceOrderRepository, userRepository);
    }

    @Test
    void completeRepair_shouldRestoreEquipmentStatus_whenEquipmentIsFault() {
        Equipment equipment = new Equipment();
        equipment.setId(1L);
        equipment.setName("加油机A");
        equipment.setStatus("FAULT");

        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(10L);
        order.setEquipmentId(1L);
        order.setStatus("IN_PROGRESS");

        when(maintenanceOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));

        MaintenanceOrder result = service.completeRepair(10L, "更换滤芯", "滤芯x2");

        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getEndTime());
        assertEquals("更换滤芯", result.getMaintenanceRecord());
        assertEquals("滤芯x2", result.getConsumedParts());
        assertEquals("NORMAL", equipment.getStatus());
        verify(equipmentRepository).save(equipment);
        verify(maintenanceOrderRepository).save(order);
    }

    @Test
    void completeRepair_shouldNotChangeEquipmentStatus_whenEquipmentNotFault() {
        Equipment equipment = new Equipment();
        equipment.setId(1L);
        equipment.setStatus("NORMAL");

        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(10L);
        order.setEquipmentId(1L);
        order.setStatus("IN_PROGRESS");

        when(maintenanceOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));

        service.completeRepair(10L, "定期保养", "无");

        assertEquals("NORMAL", equipment.getStatus());
        verify(equipmentRepository).save(equipment);
    }

    @Test
    void completeRepair_shouldThrowException_whenOrderNotFound() {
        when(maintenanceOrderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.completeRepair(999L, "test", "test"));
        assertTrue(ex.getMessage().contains("工单不存在"));
    }

    @Test
    void assignOrder_shouldAssignToMaintainer() {
        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(10L);
        order.setStatus("CREATED");

        User worker = new User();
        worker.setId(5L);
        worker.setUsername("维修工张三");
        worker.setRole("MAINTAINER");

        when(maintenanceOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findById(5L)).thenReturn(Optional.of(worker));

        MaintenanceOrder result = service.assignOrder(10L, 5L);

        assertEquals("ASSIGNED", result.getStatus());
        assertEquals("维修工张三", result.getAssignedTo());
        verify(maintenanceOrderRepository).save(order);
    }

    @Test
    void assignOrder_shouldThrowException_whenWorkerNotMaintainer() {
        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(10L);
        order.setStatus("CREATED");

        User worker = new User();
        worker.setId(5L);
        worker.setUsername("管理员");
        worker.setRole("MANAGER");

        when(maintenanceOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findById(5L)).thenReturn(Optional.of(worker));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.assignOrder(10L, 5L));
        assertTrue(ex.getMessage().contains("不是维修员"));
    }

    @Test
    void startRepair_shouldSetStartTime() {
        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(10L);
        order.setStatus("ASSIGNED");

        when(maintenanceOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        MaintenanceOrder result = service.startRepair(10L);

        assertEquals("IN_PROGRESS", result.getStatus());
        assertNotNull(result.getStartTime());
        verify(maintenanceOrderRepository).save(order);
    }
}
