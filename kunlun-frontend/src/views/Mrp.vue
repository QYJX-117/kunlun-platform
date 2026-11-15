<template>
  <div class="mrp-container">
    <!-- 左侧导航 -->
    <div class="mrp-sidebar">
      <div class="mrp-nav-title">MRP 导航</div>
      <el-menu
        :default-active="activeNav"
        class="mrp-nav-menu"
        @select="handleNavSelect"
      >
        <el-menu-item index="orders">
          <el-icon><Document /></el-icon>
          <span>物料数据表管理</span>
        </el-menu-item>
        <el-menu-item index="demand">
          <el-icon><DataAnalysis /></el-icon>
          <span>主需求计划表管理</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 右侧内容 -->
    <div class="mrp-content">
      <!-- 运算结果视图 -->
      <div v-if="activeNav === 'orders'">
        <div class="page-header">
          <h1>MRP 运算结果</h1>
          <div class="actions">
            <el-button :type="viewMode === 'table' ? 'primary' : ''" @click="viewMode = 'table'">
              <el-icon style="margin-right: 4px;"><List /></el-icon>表格视图
            </el-button>
            <el-button :type="viewMode === 'matrix' ? 'primary' : ''" @click="viewMode = 'matrix'">
              <el-icon style="margin-right: 4px;"><Grid /></el-icon>矩阵视图
            </el-button>
            <el-button type="primary" :loading="running" :disabled="running" @click="handleRunMrp">
              {{ running ? '计算中...' : '运行 MRP' }}
            </el-button>
            <el-button :loading="loading" @click="fetchOrders">刷新列表</el-button>
          </div>
        </div>

        <el-card>
          <!-- 表格视图 -->
          <el-table
            v-if="viewMode === 'table'"
            :data="orders"
            border
            stripe
            v-loading="loading"
            style="width: 100%"
            :row-class-name="tableRowClassName"
          >
            <el-table-column prop="id" label="订单ID" width="80" />
            <el-table-column prop="stationName" label="站点名称" min-width="130" />
            <el-table-column prop="materialName" label="油品" min-width="100" />
            <el-table-column prop="suggestedQuantity" label="建议补货量" width="110" />
            <el-table-column prop="requiredDate" label="要求日期" width="120" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="库存状况" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.stockGap != null && row.stockGap < 0 ? 'danger' : 'success'"
                  size="small"
                >
                  {{ row.stockGap != null && row.stockGap < 0 ? '缺料' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>

          <!-- 矩阵视图 -->
          <div v-else class="matrix-view">
            <div class="matrix-toolbar">
              <span class="matrix-title">站点 × 物料 建议补货量矩阵</span>
            </div>
            <div class="matrix-scroll">
              <table class="matrix-table">
                <thead>
                  <tr>
                    <th class="matrix-corner">站点 \ 物料</th>
                    <th v-for="m in matrixMaterials" :key="m" class="matrix-col-header">{{ m }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in matrixStations" :key="s">
                    <td class="matrix-row-header">{{ s }}</td>
                    <td
                      v-for="m in matrixMaterials"
                      :key="m"
                      :class="['matrix-cell', (matrixData[s]?.[m] ?? 0) > 0 ? 'cell-needed' : 'cell-ok']"
                    >
                      {{ matrixData[s]?.[m] ?? '-' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <el-pagination
            v-if="total > 0 && viewMode === 'table'"
            style="margin-top: 16px;"
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            v-model:current-page="currentPage"
            @current-change="fetchOrders"
          />
        </el-card>
      </div>

      <!-- 主需求计划表管理 -->
      <div v-if="activeNav === 'demand'">
        <div class="page-header">
          <h1>主需求计划表管理</h1>
          <div class="actions">
            <el-button :loading="loading" @click="fetchDemandData">刷新</el-button>
          </div>
        </div>
        <el-card>
          <el-table :data="demandData" border stripe v-loading="loading" style="width: 100%">
            <el-table-column prop="stationName" label="站点" min-width="130" />
            <el-table-column prop="materialName" label="油品" min-width="100" />
            <el-table-column prop="forecastDaily" label="日均预测销量" width="120" />
            <el-table-column prop="currentStock" label="当前库存" width="100" />
            <el-table-column prop="safetyStock" label="安全库存" width="100" />
            <el-table-column prop="stockGap" label="库存缺口" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.stockGap < 0 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
                  {{ row.stockGap }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.stockGap < 0 ? 'danger' : 'success'" size="small">
                  {{ row.stockGap < 0 ? '需补货' : '充足' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const activeNav = ref('orders')
const viewMode = ref('table')

const orders = ref<any[]>([])
const loading = ref(false)
const running = ref(false)
const total = ref(0)
const pageSize = ref(20)
const currentPage = ref(1)

const demandData = ref<any[]>([])

const statusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    CONFIRMED: 'success',
    COMPLETED: '',
  }
  return map[status] || 'info'
}

const tableRowClassName = ({ row }: { row: any }) => {
  if (row.stockGap != null && row.stockGap < 0) return 'row-shortage'
  return ''
}

const matrixStations = computed(() => {
  const set = new Set<string>()
  orders.value.forEach(o => { if (o.stationName) set.add(o.stationName) })
  return Array.from(set).sort()
})

const matrixMaterials = computed(() => {
  const set = new Set<string>()
  orders.value.forEach(o => { if (o.materialName) set.add(o.materialName) })
  return Array.from(set).sort()
})

const matrixData = computed(() => {
  const map: Record<string, Record<string, number>> = {}
  orders.value.forEach(o => {
    const sn = o.stationName
    const mn = o.materialName
    if (!sn || !mn) return
    if (!map[sn]) map[sn] = {}
    const prev = map[sn][mn] || 0
    map[sn][mn] = prev + (o.suggestedQuantity || 0)
  })
  return map
})

const handleNavSelect = (index: string) => {
  activeNav.value = index
  if (index === 'orders') {
    fetchOrders()
  } else if (index === 'demand') {
    fetchDemandData()
  }
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/mrp/planned-orders', {
      params: { page: currentPage.value - 1, size: pageSize.value },
    })
    orders.value = res.data.data || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const fetchDemandData = async () => {
  loading.value = true
  try {
    const res = await request.get('/mrp/demand-summary')
    demandData.value = res.data.data || []
  } catch {
    ElMessage.error('获取需求计划数据失败')
  } finally {
    loading.value = false
  }
}

const handleRunMrp = async () => {
  running.value = true
  try {
    const res = await request.post('/mrp/run')
    const data = res.data.data || []
    orders.value = data
    total.value = data.length
    viewMode.value = 'table'
    ElMessage.success(`MRP 运行完成，共生成 ${data.length} 条计划订单`)
  } catch {
    ElMessage.error('MRP 运行失败')
  } finally {
    running.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.mrp-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 140px);
}

.mrp-sidebar {
  width: 220px;
  min-width: 220px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.mrp-nav-title {
  padding: 16px 20px 12px;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  border-bottom: 1px solid #f0f0f0;
}

.mrp-nav-menu {
  border-right: none;
}

.mrp-nav-menu :deep(.el-menu-item) {
  color: #606266 !important;
  font-size: 14px;
  height: 48px;
  line-height: 48px;
}

.mrp-nav-menu :deep(.el-menu-item:hover) {
  background-color: #f0f5fa !important;
  color: #005293 !important;
}

.mrp-nav-menu :deep(.el-menu-item.is-active) {
  background-color: #e8f2fb !important;
  color: #005293 !important;
  font-weight: 600;
}

.mrp-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

/* 缺料行高亮 */
:deep(.row-shortage td) {
  background-color: #fef0f0 !important;
}

/* 矩阵视图 */
.matrix-view {
  padding: 8px 0;
}

.matrix-toolbar {
  margin-bottom: 12px;
}

.matrix-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.matrix-scroll {
  overflow-x: auto;
  max-height: 500px;
  overflow-y: auto;
}

.matrix-table {
  border-collapse: collapse;
  min-width: 100%;
}

.matrix-table th,
.matrix-table td {
  border: 1px solid #e8edf2;
  padding: 10px 16px;
  text-align: center;
  font-size: 14px;
  white-space: nowrap;
}

.matrix-corner {
  background: #f0f5fa;
  font-weight: 700;
  position: sticky;
  left: 0;
  z-index: 2;
}

.matrix-col-header {
  background: #005293;
  color: #fff;
  font-weight: 600;
}

.matrix-row-header {
  background: #f0f5fa;
  font-weight: 600;
  position: sticky;
  left: 0;
  z-index: 1;
}

.cell-needed {
  background: #fef0f0;
  color: #f56c6c;
  font-weight: 600;
}

.cell-ok {
  color: #c0c4cc;
}
</style>
