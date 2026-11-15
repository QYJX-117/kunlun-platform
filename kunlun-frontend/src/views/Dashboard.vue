<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" style="--gradient: linear-gradient(135deg, #005293, #0073c6);" @click="$router.push('/mrp')">
        <div class="stat-icon"><el-icon :size="32"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingOrders }}</div>
          <div class="stat-label">待处理计划订单</div>
        </div>
      </div>
      <div class="stat-card" style="--gradient: linear-gradient(135deg, #e6a23c, #f0ad4e);" @click="$router.push('/equipment')">
        <div class="stat-icon"><el-icon :size="32"><WarningFilled /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.uncompletedMaintenance }}</div>
          <div class="stat-label">未完成维修工单</div>
        </div>
      </div>
      <div class="stat-card" style="--gradient: linear-gradient(135deg, #67c23a, #85ce61);" @click="$router.push('/dispatch')">
        <div class="stat-icon"><el-icon :size="32"><Van /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.inTransitTrucks }}</div>
          <div class="stat-label">当前在途车辆</div>
        </div>
      </div>
      <div class="stat-card" style="--gradient: linear-gradient(135deg, #409eff, #66b1ff);">
        <div class="stat-icon"><el-icon :size="32"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todaySales }}</div>
          <div class="stat-label">今日销量概览 (L)</div>
        </div>
      </div>
    </div>

    <!-- 数据检索区 -->
    <el-card class="search-card">
      <template #header>
        <span class="card-title">
          <el-icon style="margin-right: 6px;"><Search /></el-icon>
          库存快速检索
        </span>
      </template>
      <div class="search-row">
        <el-select v-model="searchArea" placeholder="选择区域" clearable style="width: 180px;" @change="handleSearch">
          <el-option label="全部" value="" />
          <el-option label="兰州" value="兰州" />
          <el-option label="白银" value="白银" />
          <el-option label="定西" value="定西" />
        </el-select>
        <el-select v-model="searchStationId" placeholder="选择站点" clearable style="width: 220px;" @change="handleSearch">
          <el-option v-for="s in allStations" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px;"><Search /></el-icon>查询
        </el-button>
      </div>
      <el-table :data="inventoryData" border stripe v-loading="searchLoading" style="width: 100%; margin-top: 16px;" empty-text="请选择区域或站点进行查询">
        <el-table-column prop="stationName" label="站点" min-width="120" />
        <el-table-column prop="area" label="区域" width="80" />
        <el-table-column prop="materialName" label="油品" min-width="100" />
        <el-table-column prop="currentStock" label="当前库存" width="100" />
        <el-table-column prop="safetyStock" label="安全库存" width="100" />
        <el-table-column prop="maxStock" label="最大库存" width="100" />
        <el-table-column label="库存状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.currentStock >= row.safetyStock ? (row.currentStock >= row.maxStock ? 'warning' : 'success') : 'danger'"
              size="small"
            >
              {{ row.currentStock >= row.safetyStock ? (row.currentStock >= row.maxStock ? '超储' : '充足') : '不足' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const stats = reactive({
  pendingOrders: 0,
  uncompletedMaintenance: 0,
  inTransitTrucks: 0,
  todaySales: 0,
})

const allStations = ref<any[]>([])
const inventoryData = ref<any[]>([])
const searchLoading = ref(false)
const searchArea = ref('')
const searchStationId = ref<number | null>(null)

const fetchStats = async () => {
  try {
    const res = await request.get('/statistics/dashboard')
    const data = res.data.data || {}
    stats.pendingOrders = data.pendingOrders ?? 0
    stats.uncompletedMaintenance = data.uncompletedMaintenance ?? 0
    stats.inTransitTrucks = data.inTransitTrucks ?? 0
    stats.todaySales = data.todaySales ?? 0
  } catch {
    // 静默降级
  }
}

const fetchStations = async () => {
  try {
    const res = await request.get('/statistics/stations')
    allStations.value = res.data.data || []
  } catch {
    allStations.value = []
  }
}

const handleSearch = async () => {
  searchLoading.value = true
  try {
    const params: any = {}
    if (searchArea.value) params.area = searchArea.value
    if (searchStationId.value) params.stationId = searchStationId.value
    const res = await request.get('/statistics/inventory-search', { params })
    inventoryData.value = res.data.data || []
    if (inventoryData.value.length === 0) {
      ElMessage.info('暂无库存数据')
    }
  } catch {
    ElMessage.error('查询库存失败')
    inventoryData.value = []
  } finally {
    searchLoading.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchStations()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 20px;
  background: var(--gradient);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.stat-label {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 2px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.search-card {
  margin-top: 0;
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
