package com.kunlun.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "maintenance_order")
public class MaintenanceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reporter", length = 50)
    private String reporter;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "maintenance_record", length = 1000)
    private String maintenanceRecord;

    @Column(name = "consumed_parts", length = 500)
    private String consumedParts;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
