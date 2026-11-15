package com.kunlun.platform.repository;

import com.kunlun.platform.entity.DispatchTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchTaskRepository extends JpaRepository<DispatchTask, Long> {
    List<DispatchTask> findByPlannedOrderId(Long plannedOrderId);
    List<DispatchTask> findByTruckId(Long truckId);
    List<DispatchTask> findByStatus(String status);
}
