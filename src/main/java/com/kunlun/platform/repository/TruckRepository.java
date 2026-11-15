package com.kunlun.platform.repository;

import com.kunlun.platform.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {
    Optional<Truck> findByPlateNumber(String plateNumber);
    List<Truck> findByStatus(String status);
}
