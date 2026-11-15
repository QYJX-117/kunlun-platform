<template>
  <div>
    <div class="page-header">
      <h1>设备管理</h1>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="设备台账" name="ledger">
        <el-card>
          <el-table :data="equipmentList" border stripe v-loading="equipLoading" style="width: 100%">
            <el-table-column prop="code" label="设备编号" width="120" />
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'NORMAL' ? '正常' : '故障' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="installDate" label="安装日期" width="120" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'FAULT' && (role === 'station_master' || role === 'manager')"
                  type="danger"
                  size="small"
                  @click="showReportDialog(row)"
                >
                  报修
                </el-button>
                <span v-else style="color: #999;">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="维修工单" name="orders">
        <el-card>
          <div class="filter-bar">
            <el-select v-model="orderStatusFilter" placeholder="筛选状态" clearable style="width: 160px" @change="fetchOrders">
              <el-option label="全部" value="" />
              <el-option label="已创建" value="CREATED" />
              <el-option label="已派单" value="ASSIGNED" />
              <el-option label="维修中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
            <el-button :loading="orderLoading" @click="fetchOrders">刷新</el-button>
          </div>

          <el-table :data="orderList" border stripe v-loading="orderLoading" style="width: 100%; margin-top: 12px;">
            <el-table-column prop="id" label="工单ID" width="80" />
            <el-table-column prop="equipmentName" label="设备名称" min-width="120" />
            <el-table-column prop="description" label="故障描述" min-width="140" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="orderStatusTag(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reporter" label="报修人" width="100" />
            <el-table-column prop="assignedTo" label="指派人" width="100">
              <template #default="{ row }">{{ row.assignedTo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="maintenanceRecord" label="维修记录" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.maintenanceRecord || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <template v-if="(role === 'manager' || role === 'dispatcher') && row.status === 'CREATED'">
                  <el-button type="warning" size="small" @click="showAssignDialog(row)">派单</el-button>
                </template>
                <template v-else-if="role === 'maintainer' && row.status === 'ASSIGNED'">
                  <el-button type="primary" size="small" :loading="actionLoading === row.id" @click="handleStartRepair(row)">开始维修</el-button>
                </template>
                <template v-else-if="role === 'maintainer' && row.status === 'IN_PROGRESS'">
                  <el-button type="success" size="small" @click="showCompleteDialog(row)">完成维修</el-button>
                </template>
                <span v-else style="color: #999;">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 报修对话框 -->
    <el-dialog v-model="reportVisible" title="设备报修" width="480px">
      <el-form :model="reportForm" label-width="80px">
        <el-form-item label="设备">
          <el-input :value="reportEquipment?.name" disabled />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="reportForm.description" type="textarea" :rows="3" placeholder="请描述故障现象" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="reportForm.priority" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交</el-button>
      </template>
    </el-dialog>

    <!-- 派单对话框 -->
    <el-dialog v-model="assignVisible" title="派单" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择维修工">
          <el-select v-model="assignWorkerId" placeholder="请选择维修工" style="width: 100%">
            <el-option v-for="w in maintainerList" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 完成维修对话框 -->
    <el-dialog v-model="completeVisible" title="完成维修" width="480px">
      <el-form :model="completeForm" label-width="80px">
        <el-form-item label="维修记录">
          <el-input v-model="completeForm.maintenanceRecord" type="textarea" :rows="3" placeholder="请填写维修记录" />
        </el-form-item>
        <el-form-item label="消耗备件">
          <el-input v-model="completeForm.consumedParts" placeholder="请填写消耗的备件" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" :loading="completeSubmitting" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const role = localStorage.getItem('role') || ''

const activeTab = ref('ledger')

const equipmentList = ref<any[]>([])
const equipLoading = ref(false)

const orderList = ref<any[]>([])
const orderLoading = ref(false)
const orderStatusFilter = ref('')

const actionLoading = ref<number | null>(null)

// 报修
const reportVisible = ref(false)
const reportEquipment = ref<any>(null)
const reportForm = ref({ description: '', priority: 'MEDIUM' })
const reportSubmitting = ref(false)

// 派单
const assignVisible = ref(false)
const assignOrderId = ref<number | null>(null)
const assignWorkerId = ref<number | null>(null)
const maintainerList = ref<{ id: number; name: string }[]>([])
const assignSubmitting = ref(false)

// 完成维修
const completeVisible = ref(false)
const completeOrderId = ref<number | null>(null)
const completeForm = ref({ maintenanceRecord: '', consumedParts: '' })
const completeSubmitting = ref(false)

const orderStatusTag = (status: string) => {
  const map: Record<string, string> = {
    CREATED: 'info',
    ASSIGNED: 'warning',
    IN_PROGRESS: '',
    COMPLETED: 'success',
  }
  return map[status] || 'info'
}

const orderStatusText = (status: string) => {
  const map: Record<string, string> = {
    CREATED: '已创建',
    ASSIGNED: '已派单',
    IN_PROGRESS: '维修中',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

const fetchEquipment = async () => {
  equipLoading.value = true
  try {
    const res = await request.get('/equipment/list')
    equipmentList.value = res.data.data || []
  } catch {
    ElMessage.error('获取设备列表失败')
  } finally {
    equipLoading.value = false
  }
}

const fetchOrders = async () => {
  orderLoading.value = true
  try {
    const params: any = {}
    if (orderStatusFilter.value) params.status = orderStatusFilter.value
    const res = await request.get('/equipment/orders', { params })
    orderList.value = res.data.data || []
  } catch {
    ElMessage.error('获取工单列表失败')
  } finally {
    orderLoading.value = false
  }
}

const fetchMaintainers = async () => {
  try {
    const res = await request.get('/equipment/users/maintainers')
    maintainerList.value = res.data.data || []
  } catch {
    // fallback mock data
    maintainerList.value = [
      { id: 1, name: '维修工张三' },
      { id: 2, name: '维修工李四' },
    ]
  }
}

const showReportDialog = (row: any) => {
  reportEquipment.value = row
  reportForm.value = { description: '', priority: 'MEDIUM' }
  reportVisible.value = true
}

const submitReport = async () => {
  if (!reportForm.value.description.trim()) {
    ElMessage.warning('请填写故障描述')
    return
  }
  reportSubmitting.value = true
  try {
    await request.post('/equipment/orders', {
      equipmentId: reportEquipment.value.id,
      description: reportForm.value.description,
      priority: reportForm.value.priority,
      reporter: localStorage.getItem('role') || 'unknown',
    })
    ElMessage.success('报修工单已创建')
    reportVisible.value = false
    activeTab.value = 'orders'
    await fetchOrders()
  } catch {
    ElMessage.error('提交报修失败')
  } finally {
    reportSubmitting.value = false
  }
}

const showAssignDialog = (row: any) => {
  assignOrderId.value = row.id
  assignWorkerId.value = null
  assignVisible.value = true
  fetchMaintainers()
}

const submitAssign = async () => {
  if (!assignWorkerId.value) {
    ElMessage.warning('请选择维修工')
    return
  }
  assignSubmitting.value = true
  try {
    await request.put(`/equipment/orders/${assignOrderId.value}/assign`, {
      workerId: assignWorkerId.value,
    })
    ElMessage.success('派单成功')
    assignVisible.value = false
    await fetchOrders()
  } catch {
    ElMessage.error('派单失败')
  } finally {
    assignSubmitting.value = false
  }
}

const handleStartRepair = async (row: any) => {
  actionLoading.value = row.id
  try {
    await request.put(`/equipment/orders/${row.id}/start`)
    ElMessage.success('维修已开始')
    await fetchOrders()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

const showCompleteDialog = (row: any) => {
  completeOrderId.value = row.id
  completeForm.value = { maintenanceRecord: '', consumedParts: '' }
  completeVisible.value = true
}

const submitComplete = async () => {
  if (!completeForm.value.maintenanceRecord.trim()) {
    ElMessage.warning('请填写维修记录')
    return
  }
  completeSubmitting.value = true
  try {
    await request.put(`/equipment/orders/${completeOrderId.value}/complete`, {
      maintenanceRecord: completeForm.value.maintenanceRecord,
      consumedParts: completeForm.value.consumedParts,
    })
    ElMessage.success('维修已完成')
    completeVisible.value = false
    await Promise.all([fetchOrders(), fetchEquipment()])
  } catch {
    ElMessage.error('操作失败')
  } finally {
    completeSubmitting.value = false
  }
}

const handleTabChange = (tab: string) => {
  if (tab === 'ledger') {
    fetchEquipment()
  } else if (tab === 'orders') {
    fetchOrders()
  }
}

onMounted(() => {
  fetchEquipment()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
