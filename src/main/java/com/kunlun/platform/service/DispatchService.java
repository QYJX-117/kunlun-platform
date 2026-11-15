package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import com.kunlun.platform.util.RouteOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private static final Station DEPOT = new Station();

    static {
        DEPOT.setId(0L);
        DEPOT.setName("兰州西固油库");
        DEPOT.setLongitude(new BigDecimal("103.718"));
        DEPOT.setLatitude(new BigDecimal("36.099"));
    }

    private final PlannedOrderRepository plannedOrderRepository;
    private final TruckRepository truckRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final StationRepository stationRepository;

    public DispatchService(PlannedOrderRepository plannedOrderRepository,
                           TruckRepository truckRepository,
                           DispatchTaskRepository dispatchTaskRepository,
                           StationRepository stationRepository) {
        this.plannedOrderRepository = plannedOrderRepository;
        this.truckRepository = truckRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional
    public List<DispatchTask> generateDispatchTasks() {
        List<PlannedOrder> pendingOrders = plannedOrderRepository.findByStatus("PENDING");
        if (pendingOrders.isEmpty()) {
            log.info("没有待处理的计划订单");
            return Collections.emptyList();
        }

        // 按区域分组订单，过滤掉站点/区域缺失的订单
        Map<String, List<PlannedOrder>> areaOrderGroups = new LinkedHashMap<>();
        Map<Long, Station> allStationMap = new HashMap<>();

        for (PlannedOrder order : pendingOrders) {
            Station station = stationRepository.findById(order.getStationId()).orElse(null);
            if (station == null || station.getArea() == null) {
                log.warn("订单[{}]站点[{}]或区域信息缺失，跳过", order.getId(), order.getStationId());
                continue;
            }
            String area = station.getArea();
            areaOrderGroups.computeIfAbsent(area, k -> new ArrayList<>()).add(order);
            allStationMap.putIfAbsent(station.getId(), station);
        }

        List<DispatchTask> allTasks = new ArrayList<>();

        // 各区域独立规划，禁止跨区
        for (Map.Entry<String, List<PlannedOrder>> entry : areaOrderGroups.entrySet()) {
            String area = entry.getKey();
            List<PlannedOrder> areaOrders = entry.getValue();

            // 该区域内唯一站点去重（同一站点多个油品订单合并为一个停靠点）
            Set<Long> uniqueStationIds = areaOrders.stream()
                    .map(PlannedOrder::getStationId)
                    .collect(Collectors.toSet());

            List<Station> areaStations = new ArrayList<>();
            for (Long sid : uniqueStationIds) {
                Station s = allStationMap.get(sid);
                if (s != null) areaStations.add(s);
            }

            // 路线优化（区域内独立规划）
            List<Station> optimizedRoute = RouteOptimizer.optimizeRoute(areaStations, DEPOT);

            // 建立 站点ID → 停靠序号 映射（同一站点多个订单共享同一 routeSequence）
            Map<Long, Integer> stationSequenceMap = new LinkedHashMap<>();
            for (int i = 0; i < optimizedRoute.size(); i++) {
                stationSequenceMap.put(optimizedRoute.get(i).getId(), i + 1);
            }

            // 分配空闲车辆
            List<Truck> idleTrucks = truckRepository.findByStatus("IDLE");
            if (idleTrucks.isEmpty()) {
                log.warn("区域[{}]没有空闲车辆，跳过该区域所有订单", area);
                continue;
            }
            Truck truck = idleTrucks.get(0);
            truck.setStatus("ON_WAY");
            truckRepository.save(truck);

            // 为每个订单创建配送任务，同一站点多个订单共享 routeSequence
            for (PlannedOrder order : areaOrders) {
                Integer seq = stationSequenceMap.get(order.getStationId());
                if (seq == null) {
                    log.warn("订单[{}]站点[{}]不在优化路线中，跳过", order.getId(), order.getStationId());
                    continue;
                }

                DispatchTask task = new DispatchTask();
                task.setPlannedOrderId(order.getId());
                task.setTruckId(truck.getId());
                task.setRouteSequence(seq);
                task.setStatus("ASSIGNED");
                dispatchTaskRepository.save(task);
                allTasks.add(task);

                order.setStatus("CONFIRMED");
                plannedOrderRepository.save(order);
            }

            log.info("区域[{}]: {}个停靠点, {}个配送任务, 分配车辆[{}]车牌[{}]",
                    area, optimizedRoute.size(), areaOrders.size(), truck.getId(), truck.getPlateNumber());
        }

        log.info("配送任务生成完成，共{}个区域，{}条任务", areaOrderGroups.size(), allTasks.size());
        return allTasks;
    }
}
