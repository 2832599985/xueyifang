import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuthState } from '@/utils/auth'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: Array<RouteRecordRaw> = [
  // ==================== 独立全屏认证路由 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', hideLayout: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', hideLayout: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/login/AdminLogin.vue'),
    meta: { title: '管理员登录', hideLayout: true }
  },

  // ==================== 前台与用户中心路由 ====================
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'services',
        name: 'ServiceList',
        component: () => import('@/views/service/ServiceList.vue'),
        meta: { title: '服务大厅' }
      },
      {
        path: 'services/:id',
        name: 'ServiceDetail',
        component: () => import('@/views/service/ServiceDetail.vue'),
        meta: { title: '服务详情' }
      },
      {
        path: 'demands',
        name: 'DemandHall',
        component: () => import('@/views/demand/DemandHall.vue'),
        meta: { title: '悬赏大厅' }
      },
      {
        path: 'demands/:id',
        name: 'DemandDetail',
        component: () => import('@/views/demand/DemandDetail.vue'),
        meta: { title: '悬赏详情' }
      },
      {
        path: 'groups',
        name: 'GroupSessionHall',
        component: () => import('@/views/group/GroupSessionHall.vue'),
        meta: { title: '拼班团课' }
      },
      {
        path: 'groups/:id',
        name: 'GroupSessionDetail',
        component: () => import('@/views/group/GroupSessionDetail.vue'),
        meta: { title: '拼班详情' }
      },
      {
        path: 'leaderboard',
        name: 'CreatorLeaderboard',
        component: () => import('@/views/user/CreatorLeaderboard.vue'),
        meta: { title: '行家榜' }
      },
      {
        path: 'creators/:id',
        name: 'CreatorProfile',
        component: () => import('@/views/user/CreatorProfile.vue'),
        meta: { title: '创作者主页' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/about/About.vue'),
        meta: { title: '担保规则' }
      },
      {
        path: 'service/publish',
        name: 'ServicePublish',
        component: () => import('@/views/service/ServicePublish.vue'),
        meta: { title: '发布服务', requiresAuth: true, requiresPublishPermission: true }
      },
      {
        path: 'service/edit/:id',
        name: 'ServiceEdit',
        component: () => import('@/views/service/ServiceEdit.vue'),
        meta: { title: '编辑服务', requiresAuth: true }
      },
      {
        path: 'my-services',
        name: 'MyServices',
        component: () => import('@/views/service/MyServices.vue'),
        meta: { title: '我的服务', requiresAuth: true }
      },
      {
        path: 'order/confirm/:serviceId',
        name: 'OrderConfirm',
        component: () => import('@/views/order/OrderConfirm.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'BuyerOrderList',
        component: () => import('@/views/order/BuyerOrderList.vue'),
        meta: { title: '我的购买', requiresAuth: true }
      },
      {
        path: 'orders/sales',
        name: 'SellerOrderList',
        component: () => import('@/views/order/SellerOrderList.vue'),
        meta: { title: '我的销售', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'MyFavorites',
        component: () => import('@/views/favorite/MyFavorites.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notification/NotificationList.vue'),
        meta: { title: '通知中心', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatPage.vue'),
        meta: { title: '私信消息', requiresAuth: true }
      },
      {
        path: 'chat/:userId',
        name: 'ChatWithUser',
        component: () => import('@/views/chat/ChatPage.vue'),
        meta: { title: '私信消息', requiresAuth: true }
      },
      {
        path: 'user/center',
        name: 'UserCenter',
        component: () => import('@/views/user/UserCenter.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'wallet',
        name: 'WalletOverview',
        component: () => import('@/views/wallet/WalletOverview.vue'),
        meta: { title: '我的钱包', requiresAuth: true }
      },
      {
        path: 'wallet/transactions',
        name: 'WalletTransactions',
        component: () => import('@/views/wallet/WalletTransactions.vue'),
        meta: { title: '资金流水', requiresAuth: true }
      },
      {
        path: 'statistics/sales',
        name: 'SalesStatistics',
        component: () => import('@/views/statistics/SalesDashboard.vue'),
        meta: { title: '销售统计', requiresAuth: true }
      },
      {
        path: 'disputes',
        name: 'MyDisputes',
        component: () => import('@/views/dispute/MyDisputes.vue'),
        meta: { title: '我的纠纷', requiresAuth: true }
      },
      {
        path: 'disputes/:id',
        name: 'DisputeDetail',
        component: () => import('@/views/dispute/DisputeDetail.vue'),
        meta: { title: '纠纷详情', requiresAuth: true }
      }
    ]
  },

  // ==================== 管理后台嵌套路由 ====================
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard/AdminDashboard.vue'),
        meta: { title: '运营仪表盘', requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUserList',
        component: () => import('@/views/admin/user/AdminUserList.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'permissions',
        name: 'AdminPermissionReview',
        component: () => import('@/views/admin/permission/AdminPermissionReview.vue'),
        meta: { title: '权限审核', requiresAdmin: true }
      },
      {
        path: 'services',
        name: 'AdminServiceReview',
        component: () => import('@/views/admin/service/AdminServiceReview.vue'),
        meta: { title: '服务审核', requiresAdmin: true }
      },
      {
        path: 'services/:id',
        name: 'AdminServiceDetail',
        component: () => import('@/views/admin/service/AdminServiceDetail.vue'),
        meta: { title: '服务审核详情', requiresAdmin: true, hideInMenu: true }
      },
      {
        path: 'demands',
        name: 'AdminDemandList',
        component: () => import('@/views/admin/demand/AdminDemandList.vue'),
        meta: { title: '悬赏管理', requiresAdmin: true }
      },
      {
        path: 'risk-radar',
        name: 'AdminRiskRadar',
        component: () => import('@/views/admin/risk/AdminRiskRadar.vue'),
        meta: { title: '异常订单监测', requiresAdmin: true }
      },
      {
        path: 'finance',
        name: 'AdminFinanceReconciliation',
        component: () => import('@/views/admin/finance/AdminFinanceReconciliation.vue'),
        meta: { title: '财务对账', requiresAdmin: true }
      },
      {
        path: 'system-health',
        name: 'AdminSystemHealth',
        component: () => import('@/views/admin/system/AdminSystemHealth.vue'),
        meta: { title: '系统检查', requiresAdmin: true }
      },
      {
        path: 'disputes',
        name: 'AdminDisputeList',
        component: () => import('@/views/admin/dispute/AdminDisputeList.vue'),
        meta: { title: '纠纷处理', requiresAdmin: true }
      },
      {
        path: 'disputes/:id',
        name: 'AdminDisputeDetail',
        component: () => import('@/views/admin/dispute/AdminDisputeDetail.vue'),
        meta: { title: '纠纷仲裁详情', requiresAdmin: true, hideInMenu: true }
      },
      {
        path: 'orders/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/order/AdminOrderDetail.vue'),
        meta: { title: '后台订单详情', requiresAdmin: true, hideInMenu: true }
      },
      {
        path: 'resource/service-tags',
        name: 'AdminServiceTagList',
        component: () => import('@/views/admin/resource/ServiceTagList.vue'),
        meta: { title: '服务分类标签', requiresAdmin: true }
      },
      {
        path: 'resource/professionals',
        name: 'AdminProfessionalList',
        component: () => import('@/views/admin/resource/ProfessionalList.vue'),
        meta: { title: '专业目录管理', requiresAdmin: true }
      },
      {
        path: 'resource/campuses',
        name: 'AdminCampusGeofenceList',
        component: () => import('@/views/admin/resource/CampusGeofenceList.vue'),
        meta: { title: '校区管理', requiresAdmin: true }
      },
      {
        path: 'resource/trade-locations',
        name: 'AdminTradeLocationList',
        component: () => import('@/views/admin/resource/TradeLocationList.vue'),
        meta: { title: '交易地点管理', requiresAdmin: true }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/settings/AdminSysConfig.vue'),
        meta: { title: '系统配置与导入', requiresAdmin: true }
      }
    ]
  },

  // ==================== 404 ====================
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面未找到', hideLayout: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

function isJwtExpired(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return true
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const payload = JSON.parse(atob(padded)) as { exp?: number }
    if (typeof payload.exp !== 'number') return false
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

async function ensureUserInfo() {
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  if (userStore.userInfo) return userStore.userInfo
  try {
    return (await userStore.fetchCurrentUser()) ?? null
  } catch {
    await clearAuthState()
    return null
  }
}

router.beforeEach(async (to) => {
  const title = (to.meta.title as string) || '学艺坊'
  document.title = `${title} - 学艺坊`

  const token = localStorage.getItem('token')

  if (to.meta.requiresAdmin) {
    if (!token) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    const userInfo = await ensureUserInfo()
    if (!userInfo) {
      ElMessage.warning('登录已过期，请重新登录')
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    if (userInfo.role !== 2) {
      ElMessage.error('当前账号不是管理员')
      await clearAuthState()
      return '/admin/login'
    }
    return true
  }

  if (to.meta.requiresAuth) {
    if (!token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    const userInfo = await ensureUserInfo()
    if (!userInfo) {
      ElMessage.warning('登录已过期，请重新登录')
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (userInfo.role === 2) {
      ElMessage.info('管理员账号仅支持前台巡检浏览，请使用管理控制台')
      return '/admin/dashboard'
    }
    if (to.meta.requiresPublishPermission && userInfo.publishPermission !== 1) {
      ElMessage.warning('您暂未获得发布权限，可在个人中心提交申请')
      return '/user/center'
    }
    return true
  }

  if (token && (to.path === '/login' || to.path === '/register')) {
    if (isJwtExpired(token)) {
      await clearAuthState()
    } else {
      const userInfo = await ensureUserInfo()
      if (userInfo) {
        return userInfo.role === 2 ? '/admin/dashboard' : '/'
      }
    }
  }

  if (!localStorage.getItem('token') && to.path === '/register') {
    try {
      const { getRegisterStatus } = await import('@/api/sysConfig')
      const res = await getRegisterStatus()
      if (!res.registerEnabled) {
        ElMessage.warning('注册功能已关闭，请联系管理员')
        return '/login'
      }
    } catch {
      // fail open
    }
  }

  return true
})

export default router
