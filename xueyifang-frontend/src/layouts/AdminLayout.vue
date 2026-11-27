<template>
  <div class="admin-shell">
    <!-- Desktop Obsidian SaaS Sidebar -->
    <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed }">
      <div class="admin-sidebar__logo">
        <XyfBrandLogo
          theme="light"
          compact
          :icon-only="collapsed"
          title="学艺坊 Admin"
          subtitle="管理后台"
        />
      </div>

      <nav class="admin-sidebar__nav">
        <div
          v-for="group in menuGroups"
          :key="group.title"
          class="nav-group"
        >
          <div v-show="!collapsed" class="nav-group__label">
            {{ group.title }}
          </div>
          <router-link
            v-for="item in group.children"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': isActive(item.path) }"
            :title="collapsed ? item.title : ''"
          >
            <el-icon class="nav-item__icon"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-item__text">{{ item.title }}</span>
          </router-link>
        </div>
      </nav>

      <div class="admin-sidebar__foot">
        <router-link to="/" class="foot-btn" title="进入前台市场浏览">
          <el-icon><Shop /></el-icon>
          <span v-show="!collapsed">打开前台市场</span>
        </router-link>
        <button type="button" class="foot-btn is-danger" @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!collapsed">安全退出</span>
        </button>
      </div>
    </aside>

    <!-- Main Container -->
    <div class="admin-main" :class="{ 'is-collapsed': collapsed }">
      <header class="admin-header">
        <div class="admin-header__left">
          <button
            type="button"
            class="icon-toggle mobile-only"
            aria-label="打开后台菜单"
            @click="mobileDrawerVisible = true"
          >
            <el-icon :size="19"><MenuIcon /></el-icon>
          </button>
          <button
            type="button"
            class="icon-toggle desktop-only"
            :title="collapsed ? '展开侧栏' : '收起侧栏'"
            @click="collapsed = !collapsed"
          >
            <el-icon :size="17">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </button>

          <div class="admin-crumb">
            <span v-if="currentGroupTitle" class="admin-crumb__group">{{ currentGroupTitle }}</span>
            <span v-if="currentGroupTitle" class="admin-crumb__sep">/</span>
            <span class="admin-header__title">{{ currentTitle }}</span>
          </div>
        </div>

        <div class="admin-header__right">
          <router-link to="/" class="market-jump-btn">
            <el-icon><Shop /></el-icon>
            <span>前台预览</span>
          </router-link>

          <div class="admin-pill">
            <XyfAvatar :src="userStore.userInfo?.avatar" :name="adminName" :size="26" />
            <span class="admin-pill__name">{{ adminName }}</span>
            <span class="admin-pill__role">管理员</span>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>

    <!-- Mobile Drawer -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="268px"
      :with-header="false"
      class="admin-mobile-drawer"
    >
      <div class="mobile-admin-nav">
        <div class="mobile-admin-nav__brand">
          <XyfBrandLogo compact title="学艺坊 Admin" subtitle="管理后台" />
        </div>
        <template v-for="group in menuGroups" :key="group.title">
          <div class="mobile-nav-group">{{ group.title }}</div>
          <router-link
            v-for="child in group.children"
            :key="child.path"
            :to="child.path"
            class="mobile-nav-item"
            :class="{ 'is-active': isActive(child.path) }"
            @click="mobileDrawerVisible = false"
          >
            <el-icon><component :is="child.icon" /></el-icon>
            <span>{{ child.title }}</span>
          </router-link>
        </template>

        <div class="mobile-admin-nav__foot">
          <el-button type="danger" plain style="width: 100%" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer,
  User,
  Stamp,
  Checked,
  Aim,
  Compass,
  Wallet,
  Monitor,
  ScaleToOriginal,
  CollectionTag,
  School,
  MapLocation,
  Location,
  Setting,
  Shop,
  SwitchButton,
  Fold,
  Expand,
  Menu as MenuIcon
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import XyfBrandLogo from '@/components/ui/XyfBrandLogo.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const mobileDrawerVisible = ref(false)

type MenuItem = {
  path: string
  title: string
  icon: Component
}

type MenuGroup = {
  title: string
  children: MenuItem[]
}

const menuGroups: MenuGroup[] = [
  {
    title: '运营总览',
    children: [
      { path: '/admin/dashboard', title: '数据概览', icon: Odometer },
      { path: '/admin/system-health', title: '系统检查', icon: Monitor }
    ]
  },
  {
    title: '业务管理',
    children: [
      { path: '/admin/users', title: '用户管理', icon: User },
      { path: '/admin/permissions', title: '权限审核', icon: Stamp },
      { path: '/admin/services', title: '服务审核', icon: Checked },
      { path: '/admin/demands', title: '悬赏管理', icon: Aim }
    ]
  },
  {
    title: '财务与风控',
    children: [
      { path: '/admin/finance', title: '财务对账', icon: Wallet },
      { path: '/admin/risk-radar', title: '异常订单监测', icon: Compass },
      { path: '/admin/disputes', title: '纠纷处理', icon: ScaleToOriginal }
    ]
  },
  {
    title: '基础配置',
    children: [
      { path: '/admin/resource/campuses', title: '校区管理', icon: MapLocation },
      { path: '/admin/resource/trade-locations', title: '交易地点', icon: Location },
      { path: '/admin/resource/service-tags', title: '服务分类', icon: CollectionTag },
      { path: '/admin/resource/professionals', title: '专业目录', icon: School }
    ]
  },
  {
    title: '系统设置',
    children: [
      { path: '/admin/settings', title: '参数与导入', icon: Setting }
    ]
  }
]

const isActive = (path: string): boolean => {
  if (path === '/admin/disputes') {
    return route.path.startsWith('/admin/disputes') || route.path.startsWith('/admin/orders')
  }
  if (path === '/admin/services') {
    return route.path.startsWith('/admin/services')
  }
  return route.path === path
}

const currentGroupTitle = computed(() => {
  const matched = menuGroups.find(g => g.children.some(c => isActive(c.path)))
  return matched?.title || ''
})

const currentTitle = computed(() => {
  for (const g of menuGroups) {
    const item = g.children.find(c => isActive(c.path))
    if (item) return item.title
  }
  return (route.meta.title as string) || '管理后台'
})

const adminName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.nickname || '管理员')

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出管理员登录吗？', '退出登录', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    mobileDrawerVisible.value = false
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/admin/login')
  } catch {
    // 取消
  }
}
</script>

<style scoped lang="scss">
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #fafafa;
}

.admin-sidebar {
  width: var(--xyf-admin-sidebar-width);
  background: #0f0f12;
  color: var(--xyf-ink-invert);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 200;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  transition: width 0.22s ease;

  &.is-collapsed {
    width: 72px;

    .admin-sidebar__logo {
      padding: 0 12px;
      justify-content: center;
    }

    .nav-item {
      justify-content: center;
      padding: 10px;
    }

    .foot-btn {
      justify-content: center;
      padding: 10px;
    }
  }

  @media (max-width: 960px) {
    display: none;
  }

  &__logo {
    height: 64px;
    padding: 0 18px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  }

  &__nav {
    flex: 1;
    padding: 12px 12px 18px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__foot {
    padding: 10px 12px 14px;
    border-top: 1px solid rgba(255, 255, 255, 0.07);
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &__label {
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.02em;
    color: rgba(255, 255, 255, 0.42);
    padding: 4px 12px 5px;
  }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--xyf-radius-sm);
  color: rgba(244, 244, 245, 0.68);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--xyf-transition);

  &__icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  &:hover {
    color: #ffffff;
    background: rgba(255, 255, 255, 0.06);
  }

  &.is-active {
    color: #111113;
    background: #ffffff;
    font-weight: 600;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.28);
  }
}

.foot-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 9px;
  background: transparent;
  border: none;
  font-family: inherit;
  color: rgba(244, 244, 245, 0.68);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: rgba(255, 255, 255, 0.07);
    color: #ffffff;
  }

  &.is-danger:hover {
    color: #fca5a5;
  }
}

.admin-main {
  flex: 1;
  min-width: 0;
  margin-left: var(--xyf-admin-sidebar-width);
  display: flex;
  flex-direction: column;
  transition: margin-left 0.22s ease;

  &.is-collapsed {
    margin-left: 72px;
  }

  @media (max-width: 960px) {
    margin-left: 0 !important;
  }
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--xyf-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__title {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }
}

.admin-crumb {
  display: flex;
  align-items: center;
  gap: 8px;

  &__group {
    font-size: 13px;
    color: var(--xyf-ink-3);
    font-weight: 500;

    @media (max-width: 640px) {
      display: none;
    }
  }

  &__sep {
    color: var(--xyf-border-strong);
    font-size: 13px;

    @media (max-width: 640px) {
      display: none;
    }
  }
}

.market-jump-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-weight: 500;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    color: var(--xyf-ink);
  }

  @media (max-width: 640px) {
    display: none;
  }
}

.icon-toggle {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  color: var(--xyf-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    color: var(--xyf-primary);
    border-color: var(--xyf-primary-border);
  }

  &.mobile-only {
    display: none;
    @media (max-width: 960px) {
      display: inline-flex;
    }
  }

  &.desktop-only {
    @media (max-width: 960px) {
      display: none;
    }
  }
}

.admin-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 11px 3px 4px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid var(--xyf-border);
  background: #ffffff;

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  &__role {
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.04em;
    color: #ffffff;
    background: #18181b;
    padding: 1px 7px;
    border-radius: var(--xyf-radius-pill);
  }
}

.admin-content {
  flex: 1;
  padding: 28px;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;

  @media (max-width: 768px) {
    padding: 16px;
  }
}

.mobile-admin-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 100%;

  &__brand {
    padding-bottom: 14px;
    border-bottom: 1px solid var(--xyf-border);
    margin-bottom: 6px;
  }

  &__foot {
    margin-top: auto;
    padding-top: 16px;
  }
}

.mobile-nav-group {
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink-3);
  padding: 10px 8px 4px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 9px;
  color: var(--xyf-ink-2);
  font-weight: 500;

  &.is-active {
    background: #18181b;
    color: #ffffff;
    font-weight: 600;
  }
}
</style>
