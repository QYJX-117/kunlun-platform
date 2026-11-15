package com.kunlun.platform.repository;

import com.kunlun.platform.entity.MaintenanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {
    List<MaintenanceOrder> findByStatus(String status);
    List<MaintenanceOrder> findByEquipmentId(Long equipmentId);
}
