package com.kunlun.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "station_inventory")
public class StationInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "current_stock")
    private Integer currentStock;

    @Column(name = "safety_stock")
    private Integer safetyStock;

    @Column(name = "max_stock")
    private Integer maxStock;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
