package com.kunlun.platform.repository;

import com.kunlun.platform.entity.SalesHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, Long> {
    List<SalesHistory> findByStationId(Long stationId);
    List<SalesHistory> findByStationIdAndMaterialId(Long stationId, Long materialId);
    List<SalesHistory> findByStationIdAndSaleDateBetween(Long stationId, LocalDate start, LocalDate end);
}
