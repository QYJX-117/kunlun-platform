<template>
  <div>
    <div class="page-header">
      <h1>运输调度</h1>
      <div class="actions">
        <el-button type="primary" :loading="generating" :disabled="generating" @click="handleGenerate">
          {{ generating ? '生成中...' : '生成配送任务' }}
        </el-button>
        <el-button :loading="loading" @click="fetchTasks">刷新列表</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tasks" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column prop="driverName" label="司机" width="100" />
        <el-table-column prop="stationName" label="站点名称" min-width="120" />
        <el-table-column prop="materialName" label="油品" min-width="100" />
        <el-table-column prop="routeSequence" label="配送顺序" width="100">
          <template #default="{ row }">
            <el-tooltip content="数字代表在该车次中的送达次序，1为第一站" placement="top">
              <span class="route-seq">{{ row.routeSequence }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="途经站点" min-width="200">
          <template #default="{ row }">
            <span class="route-stations">{{ getRouteStations(row.truckId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ASSIGNED'"
              type="primary"
              size="small"
              :loading="actionLoading === row.id"
              @click="handleDepart(row)"
            >
              出发
            </el-button>
            <el-button
              v-else-if="row.status === 'ON_WAY'"
              type="success"
              size="small"
              :loading="actionLoading === row.id"
              @click="handleArrive(row)"
            >
              到达
            </el-button>
            <el-tag v-else-if="row.status === 'COMPLETED'" type="success" size="small">已完成</el-tag>
            <span v-else style="color: #999;">-</span>
            <el-button
              v-if="row.truckId"
              type="warning"
              size="small"
              plain
              style="margin-left: 4px;"
              @click="showRouteMap()"
            >
              <el-icon style="margin-right: 2px;"><MapLocation /></el-icon>查看路线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 路线地图对话框 -->
    <el-dialog v-model="mapVisible" title="配送路线预览（高德驾车路线）" width="800px" :close-on-click-modal="false" @opened="initRouteMap">
      <div id="dispatch-map" style="width: 100%; height: 500px;"></div>
      <div class="route-legend" v-if="routeInfo">
        <span>预计总距离：<strong>{{ routeInfo.distance }}</strong></span>
        <span style="margin-left: 24px;">预计行驶时间：<strong>{{ routeInfo.duration }}</strong></span>
      </div>
      <template #footer>
        <el-button @click="mapVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tasks = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)
const actionLoading = ref<number | null>(null)
const mapVisible = ref(false)
const routeInfo = ref<{ distance: string; duration: string } | null>(null)

let mapInstance: any = null
const AMAP_KEY = '837446c34ab6975dec3092f6a05b58ce'

const statusType = (status: string) => {
  const map: Record<string, string> = {
    ASSIGNED: 'warning',
    ON_WAY: '',
    COMPLETED: 'success',
  }
  return map[status] || 'info'
}

const statusText = (status: string) => {
  const map: Record<string, string> = {
    ASSIGNED: '已分配',
    ON_WAY: '在途',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

const getRouteStations = (truckId: number) => {
  if (!truckId) return '-'
  const truckTasks = tasks.value
    .filter(t => t.truckId === truckId)
    .sort((a, b) => (a.routeSequence || 0) - (b.routeSequence || 0))
  if (truckTasks.length === 0) return '-'
  const names: string[] = []
  for (const t of truckTasks) {
    const name = t.stationName
    if (name && names[names.length - 1] !== name) {
      names.push(name)
    }
  }
  return '油库 → ' + names.join(' → ')
}

const fetchTasks = async () => {
  loading.value = true
  try {
    const res = await request.get('/dispatch/tasks')
    tasks.value = res.data.data || []
  } catch {
    ElMessage.error('获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleGenerate = async () => {
  generating.value = true
  try {
    const res = await request.post('/dispatch/generate')
    const data = res.data.data || []
    tasks.value = data
    ElMessage.success(`配送任务生成完成，共 ${data.length} 条任务`)
  } catch {
    ElMessage.error('生成配送任务失败')
  } finally {
    generating.value = false
  }
}

const handleDepart = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认车辆【${row.plateNumber}】出发？`, '确认出发', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }

  actionLoading.value = row.id
  try {
    await request.put(`/dispatch/tasks/${row.id}/depart`)
    ElMessage.success('车辆已出发')
    await fetchTasks()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

const handleArrive = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认车辆【${row.plateNumber}】已到达站点【${row.stationName}】？`, '确认到达', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }

  actionLoading.value = row.id
  try {
    await request.put(`/dispatch/tasks/${row.id}/arrive`)
    ElMessage.success('任务已完成，库存已更新')
    await fetchTasks()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = null
  }
}

function loadAMap(): Promise<any> {
  return new Promise((resolve, reject) => {
    const w = window as any
    if (w.AMap) {
      resolve(w.AMap)
      return
    }
    if (document.getElementById('amap-script')) {
      const check = setInterval(() => {
        if (w.AMap) {
          clearInterval(check)
          resolve(w.AMap)
        }
      }, 200)
      return
    }
    const script = document.createElement('script')
    script.id = 'amap-script'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.Driving`
    script.onload = () => resolve(w.AMap)
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
}

const showRouteMap = async () => {
  mapVisible.value = true
  routeInfo.value = null
}

const initRouteMap = async () => {
  try {
    const AMap = await loadAMap()
    const container = document.getElementById('dispatch-map')
    if (!container) return

    if (mapInstance) {
      mapInstance.destroy()
      mapInstance = null
    }

    mapInstance = new AMap.Map(container, {
      zoom: 9,
      center: [103.718, 36.099],
    })

    const res = await request.get('/dispatch/route-plan')
    const routes = res.data.data || []

    if (routes.length === 0) {
      ElMessage.info('暂无路线数据')
      return
    }

    const colors = ['#005293', '#e6a23c', '#67c23a', '#409eff', '#f56c6c']
    let totalDist = 0
    let totalDur = 0
    let hasFailure = false

    for (let idx = 0; idx < routes.length; idx++) {
      const route = routes[idx]
      const color = colors[idx % colors.length]
      const waypoints = route.waypoints || []
      if (waypoints.length === 0) continue

      const fullPath: [number, number][] = []
      if (route.depotLng != null && route.depotLat != null) {
        fullPath.push([route.depotLng, route.depotLat])
      }
      for (const wp of waypoints) {
        fullPath.push([wp.lng, wp.lat])
      }

      if (fullPath.length < 2) continue

      // 使用高德驾车路线规划 API 获取真实路线
      const origin = fullPath[0]
      const destination = fullPath[fullPath.length - 1]
      const midPoints = fullPath.slice(1, -1)

      await new Promise<void>((resolveRoute) => {
        try {
          const driving = new AMap.Driving({
            policy: AMap.DrivingPolicy.LEAST_TIME,
          })
          driving.search(
            new AMap.LngLat(origin[0], origin[1]),
            new AMap.LngLat(destination[0], destination[1]),
            { waypoints: midPoints.map((p) => new AMap.LngLat(p[0], p[1])) },
            (status: string, result: any) => {
              if (status === 'complete' && result.routes && result.routes.length > 0) {
                const r = result.routes[0]
                const polyPath: [number, number][] = []
                for (const step of r.steps) {
                  for (const pt of step.path) {
                    polyPath.push([pt.lng, pt.lat])
                  }
                }
                new AMap.Polyline({
                  path: polyPath,
                  strokeColor: color,
                  strokeWeight: 5,
                  strokeOpacity: 0.8,
                  showDir: true,
                }).setMap(mapInstance)

                totalDist += r.distance / 1000
                totalDur += r.time / 60
              } else {
                hasFailure = true
              }
              resolveRoute()
            }
          )
        } catch {
          hasFailure = true
          resolveRoute()
        }
      })

      // 放置油库和站点标记
      if (route.depotLng != null && route.depotLat != null) {
        new AMap.Marker({
          position: [route.depotLng, route.depotLat],
          title: '油库',
          label: { content: '油库', direction: 'bottom' },
        }).setMap(mapInstance)
      }
      for (const wp of waypoints) {
        new AMap.Marker({
          position: [wp.lng, wp.lat],
          title: wp.name,
          label: { content: wp.name, direction: 'bottom' },
        }).setMap(mapInstance)
      }
    }

    routeInfo.value = {
      distance: totalDist.toFixed(1) + ' 公里',
      duration: Math.round(totalDur) + ' 分钟',
    }

    if (hasFailure) {
      ElMessage.warning('部分路线规划失败')
    }

    mapInstance.setFitView()
  } catch {
    ElMessage.error('路线规划失败')
  }
}

onMounted(() => {
  fetchTasks()
})

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.route-seq {
  font-weight: 600;
  color: #005293;
  cursor: help;
}

.route-stations {
  color: #606266;
  font-size: 13px;
}

.route-legend {
  margin-top: 12px;
  padding: 12px 16px;
  background: #f0f5fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

.route-legend strong {
  color: #005293;
}

#dispatch-map {
  border-radius: 8px;
  overflow: hidden;
}
</style>
