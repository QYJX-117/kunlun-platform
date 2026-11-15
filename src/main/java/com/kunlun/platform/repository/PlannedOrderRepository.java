package com.kunlun.platform.repository;

import com.kunlun.platform.entity.PlannedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlannedOrderRepository extends JpaRepository<PlannedOrder, Long> {
    List<PlannedOrder> findByStationId(Long stationId);
    List<PlannedOrder> findByStatus(String status);
}
