package com.kunlun.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "sales_history")
public class SalesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "quantity")
    private Integer quantity;
}
