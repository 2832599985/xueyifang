<template>
  <div class="admin-shell">
    <!-- Desktop Sidebar -->
    <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed }">
      <div class="admin-sidebar__logo">
        <XyfBrandLogo
          theme="light"
          compact
          :icon-only="collapsed"
          title="学艺坊 Admin"
          subtitle="运营治理控制台"
        />
      </div>

      <nav class="admin-sidebar__nav">
        <template v-for="item in menuItems" :key="item.title">
          <router-link
            v-if="!isMenuGroup(item)"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': isActive(item.path) }"
            :title="collapsed ? item.title : ''"
          >
            <el-icon class="nav-item__icon"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-item__text">{{ item.title }}</span>
          </router-link>

          <div v-else class="nav-group" :class="{ 'is-active': isGroupActive(item.children) }">
            <div v-show="!collapsed" class="nav-group__label">
              {{ item.title }}
            </div>
            <router-link
              v-for="child in item.children"
              :key="child.path"
              :to="child.path"
              class="nav-item"
              :class="{ 'is-active': isActive(child.path) }"
              :title="collapsed ? child.title : ''"
            >
              <el-icon class="nav-item__icon"><component :is="child.icon" /></el-icon>
              <span v-show="!collapsed" class="nav-item__text">{{ child.title }}</span>
            </router-link>
          </div>
        </template>
      </nav>

      <div class="admin-sidebar__foot">
        <router-link to="/" class="foot-btn" title="进入前台市场浏览">
          <el-icon><Shop /></el-icon>
          <span v-show="!collapsed">查看前台市场</span>
        </router-link>
        <button type="button" class="foot-btn is-danger" @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!collapsed">退出登录</span>
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
            <el-icon :size="18">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </button>
          <h1 class="admin-header__title">{{ currentTitle }}</h1>
        </div>

        <div class="admin-header__right">
          <div class="admin-pill">
            <XyfAvatar :src="userStore.userInfo?.avatar" :name="adminName" :size="28" />
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
    <el-drawer v-model="mobileDrawerVisible" direction="ltr" size="256px" :with-header="false" class="admin-mobile-drawer">
      <div class="mobile-admin-nav">
        <div class="mobile-admin-nav__brand">
          <XyfBrandLogo compact title="学艺坊 Admin" />
        </div>
        <template v-for="item in menuItems" :key="item.title">
          <router-link
            v-if="!isMenuGroup(item)"
            :to="item.path"
            class="mobile-nav-item"
            :class="{ 'is-active': isActive(item.path) }"
            @click="mobileDrawerVisible = false"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </router-link>
          <template v-else>
            <div class="mobile-nav-group">{{ item.title }}</div>
            <router-link
              v-for="child in item.children"
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
  WarnTriangleFilled,
  CollectionTag,
  School,
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

const menuItems: Array<MenuItem | MenuGroup> = [
  { path: '/admin/dashboard', title: '运营仪表盘', icon: Odometer },
  { path: '/admin/users', title: '用户与账号治理', icon: User },
  { path: '/admin/permissions', title: '发布权限审核', icon: Stamp },
  { path: '/admin/services', title: '服务审核与巡检', icon: Checked },
  { path: '/admin/demands', title: '悬赏需求巡检', icon: Checked },
  { path: '/admin/risk-radar', title: '异常互刷风控雷达', icon: WarnTriangleFilled },
  { path: '/admin/finance', title: '财务对账与爽约赔付', icon: Stamp },
  { path: '/admin/system-health', title: '安全与索引健康巡检', icon: Odometer },
  { path: '/admin/disputes', title: '订单纠纷仲裁', icon: WarnTriangleFilled },
  {
    title: '平台字典管理',
    children: [
      { path: '/admin/resource/service-tags', title: '服务分类标签', icon: CollectionTag },
      { path: '/admin/resource/professionals', title: '专业目录管理', icon: School },
      { path: '/admin/resource/campuses', title: '校区围栏与分账', icon: Location },
      { path: '/admin/resource/trade-locations', title: '预设交接点管理', icon: Location }
    ]
  },
  { path: '/admin/settings', title: '系统配置与导入', icon: Setting }
]

const currentTitle = computed(() => (route.meta.title as string) || '管理后台')
const adminName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.nickname || '管理员')

const isMenuGroup = (item: MenuItem | MenuGroup): item is MenuGroup => {
  return 'children' in item
}

const isActive = (path: string): boolean => {
  if (path === '/admin/disputes') {
    return route.path.startsWith('/admin/disputes') || route.path.startsWith('/admin/orders')
  }
  if (path === '/admin/services') {
    return route.path.startsWith('/admin/services')
  }
  return route.path === path
}

const isGroupActive = (children: MenuItem[]): boolean => {
  return children.some(child => isActive(child.path))
}

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
  background: var(--xyf-bg);
}

.admin-sidebar {
  width: var(--xyf-admin-sidebar-width);
  background: var(--xyf-surface-ink);
  color: var(--xyf-ink-invert);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 200;
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
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  &__nav {
    flex: 1;
    padding: 14px 12px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__foot {
    padding: 12px;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
}

.nav-group {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  &__label {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.06em;
    color: rgba(248, 247, 243, 0.44);
    padding: 6px 12px 4px;
    text-transform: uppercase;
  }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  border-radius: var(--xyf-radius-sm);
  color: rgba(248, 247, 243, 0.72);
  font-size: 13.5px;
  font-weight: 500;
  transition: all var(--xyf-transition);

  &__icon {
    font-size: 17px;
    flex-shrink: 0;
  }

  &:hover {
    color: #ffffff;
    background: rgba(255, 255, 255, 0.08);
  }

  &.is-active {
    color: #ffffff;
    background: var(--xyf-primary);
    box-shadow: 0 4px 12px rgba(24, 24, 27, 0.35);
  }
}

.foot-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--xyf-radius-sm);
  background: transparent;
  border: none;
  color: rgba(248, 247, 243, 0.72);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: rgba(255, 255, 255, 0.08);
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
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
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

  &__title {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.icon-toggle {
  width: 34px;
  height: 34px;
  border-radius: var(--xyf-radius-sm);
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
  padding: 4px 12px 4px 4px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface-soft);

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  &__role {
    font-size: 11px;
    font-weight: 600;
    color: var(--xyf-primary);
    background: var(--xyf-primary-soft);
    padding: 1px 7px;
    border-radius: var(--xyf-radius-pill);
  }
}

.admin-content {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;

  @media (max-width: 768px) {
    padding: 16px;
  }
}

.mobile-admin-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  height: 100%;

  &__brand {
    padding-bottom: 14px;
    border-bottom: 1px solid var(--xyf-border);
    margin-bottom: 8px;
  }

  &__foot {
    margin-top: auto;
    padding-top: 16px;
  }
}

.mobile-nav-group {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--xyf-ink-3);
  padding: 10px 8px 4px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--xyf-radius-sm);
  color: var(--xyf-ink-2);
  font-weight: 500;

  &.is-active {
    background: var(--xyf-primary-soft);
    color: var(--xyf-primary);
    font-weight: 600;
  }
}
</style>
