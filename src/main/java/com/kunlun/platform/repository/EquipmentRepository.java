package com.kunlun.platform.repository;

import com.kunlun.platform.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByStationId(Long stationId);
    List<Equipment> findByStatus(String status);
}
