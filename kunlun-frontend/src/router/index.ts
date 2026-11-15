import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'MainLayout',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { roles: ['DISPATCHER', 'STATION_MASTER', 'MAINTAINER', 'MANAGER'] },
        },
        {
          path: 'mrp',
          name: 'Mrp',
          component: () => import('@/views/Mrp.vue'),
          meta: { roles: ['DISPATCHER', 'MANAGER'] },
        },
        {
          path: 'dispatch',
          name: 'Dispatch',
          component: () => import('@/views/Dispatch.vue'),
          meta: { roles: ['DISPATCHER', 'MANAGER'] },
        },
        {
          path: 'equipment',
          name: 'Equipment',
          component: () => import('@/views/Equipment.vue'),
          meta: { roles: ['STATION_MASTER', 'MAINTAINER', 'MANAGER'] },
        },
        {
          path: 'bi',
          name: 'BiBoard',
          component: () => import('@/views/BiBoard.vue'),
          meta: { roles: ['MANAGER', 'DISPATCHER'] },
        },
        {
          path: '403',
          name: 'Forbidden',
          component: () => import('@/views/403.vue'),
          meta: { public: true },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.public) {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  const allowedRoles = to.meta.roles as string[] | undefined
  if (allowedRoles && allowedRoles.length > 0) {
    const role = (localStorage.getItem('role') || '').toUpperCase()
    if (!allowedRoles.includes(role)) {
      next('/403')
      return
    }
  }

  next()
})

export default router
