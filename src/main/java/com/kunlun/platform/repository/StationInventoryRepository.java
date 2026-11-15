package com.kunlun.platform.repository;

import com.kunlun.platform.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {
    List<StationInventory> findByStationId(Long stationId);
    Optional<StationInventory> findByStationIdAndMaterialId(Long stationId, Long materialId);
}
