<template>
  <el-container class="main-container">
    <el-aside width="220px" class="main-sidebar">
      <div class="logo">
        <span class="logo-icon">⛽</span>
        <span class="logo-text">昆仑智联</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#fff"
      >
        <el-menu-item
          v-for="item in filteredMenus"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="main-header">
        <div class="header-left">
          <span class="welcome-text">欢迎回来</span>
        </div>
        <div class="header-right">
          <span class="role-tag">
            <el-icon style="margin-right: 4px;"><UserFilled /></el-icon>
            {{ roleName }}
          </span>
          <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const role = localStorage.getItem('role') || ''

const roleNameMap: Record<string, string> = {
  dispatcher: '调度员',
  station_master: '站长',
  maintainer: '维修员',
  manager: '管理员',
}

const roleName = computed(() => roleNameMap[role] || role)

interface MenuItem {
  path: string
  title: string
  icon: string
  roles: string[]
}

const allMenus: MenuItem[] = [
  { path: '/dashboard', title: '首页工作台', icon: 'HomeFilled', roles: ['dispatcher', 'station_master', 'maintainer', 'manager'] },
  { path: '/mrp', title: 'MRP 需求计划', icon: 'TrendCharts', roles: ['dispatcher', 'manager'] },
  { path: '/dispatch', title: '运输调度', icon: 'Van', roles: ['dispatcher', 'manager'] },
  { path: '/equipment', title: '设备管理', icon: 'Setting', roles: ['station_master', 'maintainer', 'manager'] },
  { path: '/bi', title: 'BI 看板', icon: 'DataAnalysis', roles: ['manager', 'dispatcher'] },
]

const filteredMenus = computed(() => {
  return allMenus.filter((item) => item.roles.includes(role))
})

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.main-container {
  height: 100vh;
}

.main-sidebar {
  background-color: #001529;
  overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: #001529;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.el-menu {
  border-right: none;
}

.main-header {
  background: #fff;
  border-bottom: 1px solid #e8edf2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.welcome-text {
  font-size: 14px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.role-tag {
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 14px;
}

.main-content {
  background: #f0f5fa;
  min-height: calc(100vh - 60px);
}
</style>
