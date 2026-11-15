package com.kunlun.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_task")
public class DispatchTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "planned_order_id", nullable = false)
    private Long plannedOrderId;

    @Column(name = "truck_id", nullable = false)
    private Long truckId;

    @Column(name = "route_sequence")
    private Integer routeSequence;

    @Column(name = "estimated_departure")
    private LocalDateTime estimatedDeparture;

    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "actual_departure")
    private LocalDateTime actualDeparture;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Column(name = "status", length = 20)
    private String status;
}
