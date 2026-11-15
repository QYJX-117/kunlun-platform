<template>
  <div class="bi-board">
    <div class="page-header">
      <h1>BI 看板</h1>
      <div class="actions">
        <el-select v-model="selectedArea" placeholder="选择区域" clearable style="width: 160px" @change="onAreaChange">
          <el-option label="全部" value="" />
          <el-option label="兰州" value="兰州" />
          <el-option label="白银" value="白银" />
          <el-option label="定西" value="定西" />
        </el-select>
        <el-button type="warning" plain @click="showDriverRoutePreview">
          <el-icon style="margin-right: 4px;"><Van /></el-icon>司机路线预览
        </el-button>
      </div>
    </div>

    <div class="top-row">
      <div class="map-container">
        <div id="amap" ref="mapRef" style="width: 100%; height: 100%"></div>
      </div>
      <div class="chart-container">
        <div ref="trendChartRef" style="width: 100%; height: 100%"></div>
      </div>
    </div>

    <div class="bottom-row">
      <div class="chart-container">
        <div ref="barChartRef" style="width: 100%; height: 100%"></div>
      </div>
      <div class="right-panel">
        <el-card class="stat-card">
          <div class="stat-label">待执行计划订单</div>
          <div class="stat-value">{{ pendingCount }}</div>
        </el-card>
        <div class="chart-container" style="flex: 1;">
          <div ref="failureChartRef" style="width: 100%; height: 100%"></div>
        </div>
      </div>
    </div>

    <!-- 路线预览对话框 -->
    <el-dialog v-model="routePreviewVisible" title="司机路线预览 - 实际行车路线" width="900px" :close-on-click-modal="false" @opened="initRoutePreviewMap">
      <div id="route-preview-map" style="width: 100%; height: 520px;"></div>
      <div class="route-summary" v-if="routeSummary">
        <div v-for="(rt, idx) in routeSummary" :key="idx" class="route-item">
          <span class="route-color" :style="{ background: routeColors[idx % routeColors.length] }"></span>
          <span>{{ rt.truck }} | {{ rt.driver }} | {{ rt.route }} | <strong>{{ rt.distance }}km</strong> | 约<strong>{{ rt.duration }}分钟</strong></span>
        </div>
      </div>
      <template #footer>
        <el-button @click="routePreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const selectedArea = ref('')
const pendingCount = ref(0)

const mapRef = ref<HTMLDivElement | null>(null)
const trendChartRef = ref<HTMLDivElement | null>(null)
const barChartRef = ref<HTMLDivElement | null>(null)
const failureChartRef = ref<HTMLDivElement | null>(null)

let mapInstance: any = null
let markers: any[] = []
let infoWindow: any = null
let trendChart: any = null
let barChart: any = null
let failureChart: any = null
const stationDataList = ref<any[]>([])
let selectedStationId: number | null = null

const routePreviewVisible = ref(false)
const routeSummary = ref<any[] | null>(null)
const routeColors = ['#005293', '#e6a23c', '#67c23a', '#409eff', '#f56c6c']
let routePreviewMap: any = null

const AMAP_KEY = '837446c34ab6975dec3092f6a05b58ce'

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

const healthColor = (health: string) => {
  const map: Record<string, string> = { GREEN: '#67C23A', YELLOW: '#E6A23C', RED: '#F56C6C' }
  return map[health] || '#909399'
}

async function fetchInventoryHealth() {
  try {
    const params: any = {}
    if (selectedArea.value) params.area = selectedArea.value
    const res = await request.get('/statistics/inventory-health', { params })
    stationDataList.value = res.data.data || []
  } catch {
    stationDataList.value = []
  }
}

async function fetchSalesTrend(stationId: number) {
  try {
    const res = await request.get('/statistics/sales-trend', { params: { stationId, days: 7 } })
    return res.data.data || []
  } catch {
    return []
  }
}

async function fetchPendingCount() {
  try {
    const res = await request.get('/statistics/pending-orders-count')
    pendingCount.value = res.data.data ?? 0
  } catch {
    pendingCount.value = 0
  }
}

async function fetchFailureRanking() {
  try {
    const res = await request.get('/statistics/failure-ranking')
    return res.data.data || []
  } catch {
    return []
  }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const echarts = (window as any).echarts
  if (!echarts) return
  trendChart = echarts.init(trendChartRef.value)
}

function initBarChart() {
  if (!barChartRef.value) return
  const echarts = (window as any).echarts
  if (!echarts) return
  barChart = echarts.init(barChartRef.value)
}

function initFailureChart() {
  if (!failureChartRef.value) return
  const echarts = (window as any).echarts
  if (!echarts) return
  failureChart = echarts.init(failureChartRef.value)
}

async function renderTrendChart(stationId: number, stationName: string) {
  if (!trendChart) return
  const data = await fetchSalesTrend(stationId)

  const dateMap = new Map<string, Map<string, number>>()
  for (const item of data) {
    const d = item.date
    const mn = item.materialName
    const qty = Number(item.quantity) || 0
    if (!dateMap.has(d)) dateMap.set(d, new Map())
    dateMap.get(d)!.set(mn, (dateMap.get(d)!.get(mn) || 0) + qty)
  }

  const dates = Array.from(dateMap.keys()).sort()
  const materialSet = new Set<string>()
  for (const [, mm] of dateMap) {
    for (const mn of mm.keys()) materialSet.add(mn)
  }
  const materialNames = Array.from(materialSet)

  const series = materialNames.map(mn => ({
    name: mn,
    type: 'line',
    smooth: true,
    data: dates.map(d => dateMap.get(d)?.get(mn) || 0),
  }))

  trendChart.setOption({
    title: { text: `${stationName} 销量趋势`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: materialNames },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '销量' },
    series,
  }, true)
}

async function renderBarChart() {
  if (!barChart) return
  const data = stationDataList.value
  const names = data.map((s: any) => s.name)
  const currentStocks = data.map((s: any) => Number(s.currentStock) || 0)
  const safetyStocks = data.map((s: any) => Number(s.safetyStock) || 0)

  barChart.setOption({
    title: { text: '站点库存对比（当前 vs 安全）', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['当前库存', '安全库存'] },
    grid: { left: 60, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '库存量' },
    series: [
      { name: '当前库存', type: 'bar', data: currentStocks, itemStyle: { color: '#005293' } },
      { name: '安全库存', type: 'bar', data: safetyStocks, itemStyle: { color: '#E6A23C' } },
    ],
  }, true)
}

async function renderFailureChart() {
  if (!failureChart) return
  const data = await fetchFailureRanking()
  const names = data.map((d: any) => d.name).reverse()
  const counts = data.map((d: any) => Number(d.count)).reverse()

  failureChart.setOption({
    title: { text: '设备故障率排行', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 40, top: 40, bottom: 20 },
    xAxis: { type: 'value', name: '次数' },
    yAxis: { type: 'category', data: names },
    series: [{ type: 'bar', data: counts, itemStyle: { color: '#F56C6C' } }],
  }, true)
}

async function initMap() {
  try {
    const AMap = await loadAMap()
    await nextTick()
    if (!mapRef.value) return

    mapInstance = new AMap.Map(mapRef.value, {
      zoom: 8,
      center: [104.0, 36.5],
    })

    infoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -20),
    })

    await renderMapMarkers()
  } catch (e) {
    console.error('地图初始化失败:', e)
  }
}

async function renderMapMarkers() {
  if (!mapInstance) return
  markers.forEach(m => mapInstance.remove(m))
  markers = []

  for (const s of stationDataList.value) {
    const lon = Number(s.longitude) || 0
    const lat = Number(s.latitude) || 0
    if (lon === 0 && lat === 0) continue

    const color = healthColor(s.health)
    const content = `<div style="width:16px;height:16px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 6px rgba(0,0,0,0.3);cursor:pointer;"></div>`

    const marker = new (window as any).AMap.Marker({
      position: [lon, lat],
      content,
      offset: new (window as any).AMap.Pixel(-8, -8),
      title: s.name,
    })

    const stockStatus = Number(s.currentStock) >= Number(s.safetyStock) ? '充足' : '不足'
    const healthText = s.health === 'GREEN' ? '健康' : s.health === 'YELLOW' ? '警告' : '危险'

    marker.on('click', () => {
      selectedStationId = s.stationId
      renderTrendChart(s.stationId, s.name)

      const infoContent = `
        <div style="padding:8px 12px;min-width:180px;">
          <h4 style="margin:0 0 8px;color:#303133;">${s.name}</h4>
          <p style="margin:4px 0;font-size:13px;color:#606266;">区域: ${s.area || '-'}</p>
          <p style="margin:4px 0;font-size:13px;color:#606266;">当前库存: <b style="color:${color}">${s.currentStock}</b></p>
          <p style="margin:4px 0;font-size:13px;color:#606266;">安全库存: ${s.safetyStock}</p>
          <p style="margin:4px 0;font-size:13px;">库存状态: <span style="color:${color};font-weight:600;">${stockStatus} (${healthText})</span></p>
        </div>
      `
      infoWindow.setContent(infoContent)
      infoWindow.open(mapInstance, marker.getPosition())
    })

    mapInstance.add(marker)
    markers.push(marker)
  }

  if (stationDataList.value.length > 0 && !selectedStationId) {
    const first = stationDataList.value[0]
    selectedStationId = first.stationId
    await renderTrendChart(first.stationId, first.name)
  }
}

async function onAreaChange() {
  await fetchInventoryHealth()
  await renderMapMarkers()
  await renderBarChart()
}

function handleResize() {
  trendChart?.resize()
  barChart?.resize()
  failureChart?.resize()
}

// 司机路线预览
const showDriverRoutePreview = () => {
  routePreviewVisible.value = true
  routeSummary.value = null
}

const initRoutePreviewMap = async () => {
  try {
    const AMap = await loadAMap()
    const container = document.getElementById('route-preview-map')
    if (!container) return

    if (routePreviewMap) {
      routePreviewMap.destroy()
      routePreviewMap = null
    }

    routePreviewMap = new AMap.Map(container, {
      zoom: 9,
      center: [103.718, 36.099],
    })

    const res = await request.get('/dispatch/route-plan')
    const routes = res.data.data || []

    if (routes.length === 0) {
      ElMessage.info('暂无路线数据')
      return
    }

    const summaryArr: any[] = []

    for (let idx = 0; idx < routes.length; idx++) {
      const route = routes[idx]
      const color = routeColors[idx % routeColors.length]

      const waypoints = route.waypoints || []
      if (waypoints.length === 0) continue

      const fullPath: [number, number][] = []
      if (route.depotLng != null && route.depotLat != null) {
        fullPath.push([route.depotLng, route.depotLat])
      }
      for (const wp of waypoints) {
        fullPath.push([wp.lng, wp.lat])
      }

      if (fullPath.length >= 2) {
        const origin = fullPath[0]
        const destination = fullPath[fullPath.length - 1]
        const midPoints = fullPath.slice(1, -1)

        await new Promise<void>((resolveSearch) => {
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
                }).setMap(routePreviewMap)

                const routeStations = ['油库', ...waypoints.map((wp: any) => wp.name)]
                summaryArr.push({
                  truck: route.truck || `车辆${idx + 1}`,
                  driver: route.driver || '-',
                  route: routeStations.join(' → '),
                  distance: (r.distance / 1000).toFixed(1),
                  duration: Math.round(r.time / 60),
                })
              } else {
                ElMessage.warning('部分路线规划失败')
                const routeStations = ['油库', ...waypoints.map((wp: any) => wp.name)]
                summaryArr.push({
                  truck: route.truck || `车辆${idx + 1}`,
                  driver: route.driver || '-',
                  route: routeStations.join(' → '),
                  distance: '-',
                  duration: '-',
                })
              }
              resolveSearch()
            }
          )
        })
      }

      // 标记油库
      if (route.depotLng != null && route.depotLat != null) {
        new AMap.Marker({
          position: [route.depotLng, route.depotLat],
          title: '油库',
          label: { content: '油库', direction: 'bottom' },
        }).setMap(routePreviewMap)
      }

      // 标记站点（坐标严格取自数据库 station 表）
      for (const wp of waypoints) {
        new AMap.Marker({
          position: [wp.lng, wp.lat],
          title: wp.name,
          label: { content: wp.name, direction: 'bottom' },
        }).setMap(routePreviewMap)
      }
    }

    routeSummary.value = summaryArr
    routePreviewMap.setFitView()
  } catch {
    ElMessage.error('路线规划失败')
  }
}

onMounted(async () => {
  await fetchInventoryHealth()

  initTrendChart()
  initBarChart()
  initFailureChart()
  await initMap()

  await fetchPendingCount()
  await renderBarChart()
  await renderFailureChart()

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  barChart?.dispose()
  failureChart?.dispose()
  if (mapInstance) {
    mapInstance.destroy()
  }
  if (routePreviewMap) {
    routePreviewMap.destroy()
  }
})
</script>

<style scoped>
.bi-board {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.top-row {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.bottom-row {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.map-container {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.chart-container {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 8px;
  min-height: 0;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #005293;
  margin-top: 8px;
}

#route-preview-map {
  border-radius: 8px;
  overflow: hidden;
}

.route-summary {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.route-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f7faff;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.route-color {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  flex-shrink: 0;
}

.route-item strong {
  color: #303133;
}
</style>
