package com.kunlun.platform.service;

import com.kunlun.platform.entity.*;
import com.kunlun.platform.repository.*;
import com.kunlun.platform.util.ExponentialSmoothing;
import com.kunlun.platform.util.SafetyStockCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MrpService {

    private static final Logger log = LoggerFactory.getLogger(MrpService.class);

    private final StationRepository stationRepository;
    private final MaterialRepository materialRepository;
    private final SalesHistoryRepository salesHistoryRepository;
    private final StationInventoryRepository stationInventoryRepository;
    private final PlannedOrderRepository plannedOrderRepository;

    public MrpService(StationRepository stationRepository,
                      MaterialRepository materialRepository,
                      SalesHistoryRepository salesHistoryRepository,
                      StationInventoryRepository stationInventoryRepository,
                      PlannedOrderRepository plannedOrderRepository) {
        this.stationRepository = stationRepository;
        this.materialRepository = materialRepository;
        this.salesHistoryRepository = salesHistoryRepository;
        this.stationInventoryRepository = stationInventoryRepository;
        this.plannedOrderRepository = plannedOrderRepository;
    }

    @Transactional
    public List<PlannedOrder> runMrp() {
        List<PlannedOrder> generatedOrders = new ArrayList<>();
        List<Station> stations = stationRepository.findAll();
        List<Material> materials = materialRepository.findAll();

        for (Station station : stations) {
            for (Material material : materials) {
                // 1) 查询最近 7 天销量历史
                List<SalesHistory> salesList = salesHistoryRepository
                        .findByStationIdAndMaterialId(station.getId(), material.getId());
                if (salesList == null || salesList.size() < 3) {
                    log.info("跳过站点[{}]油品[{}]: 历史销量数据不足3条", station.getName(), material.getName());
                    continue;
                }
                // 按日期降序取最近 7 条
                List<SalesHistory> recent = salesList.stream()
                        .sorted(Comparator.comparing(SalesHistory::getSaleDate).reversed())
                        .limit(7)
                        .collect(Collectors.toList());
                if (recent.size() < 3) {
                    log.info("跳过站点[{}]油品[{}]: 近期销量不足3条", station.getName(), material.getName());
                    continue;
                }
                // 转换为 double 列表（按时间升序用于预测）
                List<Double> dailySales = recent.stream()
                        .sorted(Comparator.comparing(SalesHistory::getSaleDate))
                        .map(s -> s.getQuantity().doubleValue())
                        .collect(Collectors.toList());

                // 2) 指数平滑预测日均销量
                double bestAlpha = ExponentialSmoothing.compareAlpha(dailySales,
                        new double[]{0.1, 0.2, 0.3, 0.4, 0.5});
                double dailyForecast = ExponentialSmoothing.forecast(dailySales, bestAlpha);

                // 3) 未来 3 天总需求
                long forecast3Days = (long) Math.ceil(dailyForecast * 3);

                // 4) 当前库存 & 提前期
                StationInventory inventory = stationInventoryRepository
                        .findByStationIdAndMaterialId(station.getId(), material.getId())
                        .orElse(null);
                double currentStock = inventory != null && inventory.getCurrentStock() != null
                        ? inventory.getCurrentStock().doubleValue() : 0.0;
                int leadTime = material.getLeadTime() != null ? material.getLeadTime() : 3;

                // 5) 安全库存
                double[] salesArray = dailySales.stream().mapToDouble(Double::doubleValue).toArray();
                double safetyStock = SafetyStockCalculator.calculate(salesArray, leadTime, 0.95);

                // 6) 净需求
                double net = forecast3Days - currentStock + safetyStock;
                if (net <= 0) {
                    continue;
                }

                // 7) 创建计划订单
                PlannedOrder order = new PlannedOrder();
                order.setStationId(station.getId());
                order.setMaterialId(material.getId());
                order.setSuggestedQuantity((int) Math.ceil(net));
                order.setRequiredDate(LocalDate.now().plusDays(leadTime));
                order.setStatus("PENDING");
                order.setCreatedAt(LocalDateTime.now());
                plannedOrderRepository.save(order);
                generatedOrders.add(order);

                log.info("生成计划订单: 站点[{}] 油品[{}] 建议数量[{}] 需求日期[{}]",
                        station.getName(), material.getName(),
                        order.getSuggestedQuantity(), order.getRequiredDate());
            }
        }
        return generatedOrders;
    }
}
