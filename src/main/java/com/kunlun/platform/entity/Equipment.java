package com.kunlun.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;
}
