<template>
  <div class="main-shell">
    <!-- Top Frosted Studio Navigation -->
    <header class="main-header">
      <div class="xyf-container main-header__inner">
        <div class="main-header__left">
          <button
            type="button"
            class="mobile-menu-btn"
            aria-label="打开导航菜单"
            @click="mobileDrawerVisible = true"
          >
            <el-icon :size="20"><MenuIcon /></el-icon>
          </button>

          <router-link to="/" class="brand-link">
            <XyfBrandLogo compact subtitle="" />
          </router-link>

          <nav class="main-nav">
            <router-link to="/" class="main-nav__link" :class="{ 'is-active': route.path === '/' }">
              发现
            </router-link>
            <router-link
              to="/services"
              class="main-nav__link"
              :class="{ 'is-active': route.path.startsWith('/services') }"
            >
              服务大厅
            </router-link>
            <router-link
              to="/demands"
              class="main-nav__link"
              :class="{ 'is-active': route.path.startsWith('/demands') }"
            >
              悬赏大厅
            </router-link>
            <router-link
              to="/groups"
              class="main-nav__link"
              :class="{ 'is-active': route.path.startsWith('/groups') }"
            >
              拼班团课
            </router-link>
            <router-link
              to="/leaderboard"
              class="main-nav__link"
              :class="{ 'is-active': route.path.startsWith('/leaderboard') }"
            >
              技能风云榜
            </router-link>
            <router-link
              to="/about"
              class="main-nav__link"
              :class="{ 'is-active': route.path === '/about' }"
            >
              交易保障
            </router-link>
          </nav>
        </div>

        <div class="main-header__right">
          <div v-if="route.path !== '/'" class="header-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索校园技能服务..."
              clearable
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
            />
          </div>

          <button
            v-if="userStore.isAdmin"
            type="button"
            class="publish-cta is-admin-return"
            @click="router.push('/admin/dashboard')"
          >
            <el-icon><Setting /></el-icon>
            <span>返回管理后台</span>
          </button>

          <button v-else type="button" class="publish-cta" @click="goPublish">
            <el-icon><Plus /></el-icon>
            <span>发布服务</span>
          </button>

          <template v-if="userStore.userInfo">
            <template v-if="!userStore.isAdmin">
              <button type="button" class="icon-btn" title="我的收藏" @click="router.push('/favorites')">
                <el-icon :size="19"><Star /></el-icon>
              </button>

              <!-- Notifications Popover -->
              <el-popover
                v-model:visible="notifyPopoverVisible"
                placement="bottom-end"
                :width="352"
                trigger="click"
                popper-class="xyf-notify-popover"
                @show="handleNotifyPopoverShow"
              >
                <template #reference>
                  <button type="button" class="icon-btn" title="消息通知">
                    <el-badge
                      :value="notificationStore.unreadCount"
                      :hidden="notificationStore.unreadCount === 0"
                      :max="99"
                    >
                      <el-icon :size="19"><Bell /></el-icon>
                    </el-badge>
                  </button>
                </template>

                <div class="notify-panel">
                  <div class="notify-panel__head">
                    <span>站内通知</span>
                    <el-button
                      v-if="notificationStore.unreadCount > 0"
                      text
                      size="small"
                      @click="handleMarkAllNotificationsRead"
                    >
                      全部已读
                    </el-button>
                  </div>

                  <div class="notify-panel__list">
                    <template v-if="notificationStore.recentNotifications.length > 0">
                      <div
                        v-for="item in notificationStore.recentNotifications"
                        :key="item.id"
                        class="notify-row"
                        :class="{ 'is-unread': item.isRead === 0 }"
                        @click="handleClickNotificationItem(item)"
                      >
                        <div class="notify-row__meta">
                          <span class="notify-row__type">
                            {{ notificationTypeMap[item.notificationType] || '通知' }}
                          </span>
                          <span class="notify-row__time">{{ formatNotificationTime(item.createTime) }}</span>
                        </div>
                        <div class="notify-row__title ellipsis">{{ item.title }}</div>
                      </div>
                    </template>
                    <div v-else class="notify-panel__empty">暂无通知消息</div>
                  </div>

                  <div class="notify-panel__foot">
                    <el-button text class="view-all-btn" @click="goNotifications">
                      进入通知中心
                      <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </div>
              </el-popover>

              <!-- Chat Button -->
              <button type="button" class="icon-btn" title="私信聊天" @click="router.push('/chat')">
                <el-badge
                  :value="chatStore.totalUnreadCount"
                  :hidden="chatStore.totalUnreadCount === 0"
                  :max="99"
                >
                  <el-icon :size="19"><ChatDotRound /></el-icon>
                </el-badge>
              </button>
            </template>

            <!-- User Dropdown -->
            <el-dropdown trigger="click" @command="handleCommand">
              <button type="button" class="user-trigger">
                <XyfAvatar
                  :src="userStore.userInfo.avatar"
                  :name="userStore.userInfo.nickname || userStore.userInfo.realName"
                  :size="32"
                />
                <span class="user-trigger__name ellipsis">
                  {{ userStore.userInfo.nickname || userStore.userInfo.realName }}
                </span>
                <span v-if="userStore.isAdmin" class="admin-role-chip">管理员</span>
                <el-icon class="user-trigger__caret"><ArrowDown /></el-icon>
              </button>

              <template #dropdown>
                <el-dropdown-menu>
                  <template v-if="userStore.isAdmin">
                    <el-dropdown-item command="admin">
                      <el-icon><Setting /></el-icon>
                      <span>进入管理后台</span>
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </el-dropdown-item>
                  </template>
                  <template v-else>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      <span>个人中心</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="wallet">
                      <el-icon><Wallet /></el-icon>
                      <span>我的钱包</span>
                    </el-dropdown-item>
                    <el-dropdown-item divided command="my-services">
                      <el-icon><ShoppingBag /></el-icon>
                      <span>我的服务</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="my-buy-orders">
                      <el-icon><List /></el-icon>
                      <span>我的购买</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="my-sell-orders">
                      <el-icon><Ticket /></el-icon>
                      <span>我的销售</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="sales-stats">
                      <el-icon><TrendCharts /></el-icon>
                      <span>销售统计</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="disputes">
                      <el-icon><Warning /></el-icon>
                      <span>我的纠纷</span>
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button
              v-if="registerConfigStore.registerEnabled"
              type="primary"
              plain
              @click="router.push('/register')"
            >
              注册
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- Main View Content -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="main-footer">
      <div class="xyf-container main-footer__inner">
        <div class="main-footer__brand">
          <XyfBrandLogo compact subtitle="" />
          <p class="footer-note">
            连接高校内的专业技能与真实需求。实名认证 · 资金担保 · 满意再结算。
          </p>
        </div>

        <div class="main-footer__links">
          <div class="link-col">
            <h4>发现服务</h4>
            <router-link to="/">发现主页</router-link>
            <router-link to="/services">全部技能</router-link>
            <router-link to="/demands">悬赏大厅</router-link>
            <router-link to="/groups">拼班团课</router-link>
            <router-link to="/about">交易保障</router-link>
            <router-link v-if="!userStore.isAdmin" to="/service/publish">成为创作者</router-link>
          </div>
          <div class="link-col">
            <h4>{{ userStore.isAdmin ? '平台治理' : '个人工作台' }}</h4>
            <template v-if="userStore.isAdmin">
              <router-link to="/admin/dashboard">运营仪表盘</router-link>
              <router-link to="/admin/services">服务审核与巡检</router-link>
              <router-link to="/admin/disputes">订单纠纷仲裁</router-link>
            </template>
            <template v-else>
              <router-link to="/user/center">个人中心</router-link>
              <router-link to="/wallet">我的钱包</router-link>
              <router-link to="/orders">购买订单</router-link>
              <router-link to="/orders/sales">销售订单</router-link>
            </template>
          </div>
        </div>
      </div>
      <div class="xyf-container main-footer__bottom">
        <span>© {{ new Date().getFullYear() }} 学艺坊 Xueyifang · 校园专业技能服务平台</span>
        <router-link
          :to="userStore.isAdmin ? '/admin/dashboard' : '/admin/login'"
          class="admin-entry"
        >
          {{ userStore.isAdmin ? '返回管理控制台' : '管理后台' }}
        </router-link>
      </div>
    </footer>

    <!-- Mobile Navigation Drawer -->
    <el-drawer v-model="mobileDrawerVisible" direction="ltr" size="280px" :with-header="false">
      <div class="mobile-drawer">
        <div class="mobile-drawer__head">
          <XyfBrandLogo compact subtitle="校园技能工坊" />
        </div>

        <div class="mobile-drawer__search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索技能服务..."
            :prefix-icon="Search"
            @keyup.enter="handleMobileSearch"
          />
        </div>

        <nav class="mobile-drawer__nav">
          <router-link to="/" @click="mobileDrawerVisible = false">发现</router-link>
          <router-link to="/services" @click="mobileDrawerVisible = false">服务大厅</router-link>
          <router-link to="/about" @click="mobileDrawerVisible = false">交易保障</router-link>
          <router-link v-if="userStore.isAdmin" to="/admin/dashboard" @click="mobileDrawerVisible = false">
            返回管理后台
          </router-link>
          <router-link v-else to="/service/publish" @click="mobileDrawerVisible = false">
            发布服务
          </router-link>
          <template v-if="userStore.userInfo && !userStore.isAdmin">
            <div class="drawer-divider" />
            <router-link to="/user/center" @click="mobileDrawerVisible = false">个人中心</router-link>
            <router-link to="/wallet" @click="mobileDrawerVisible = false">我的钱包</router-link>
            <router-link to="/my-services" @click="mobileDrawerVisible = false">我的服务</router-link>
            <router-link to="/orders" @click="mobileDrawerVisible = false">我的购买</router-link>
            <router-link to="/orders/sales" @click="mobileDrawerVisible = false">我的销售</router-link>
            <router-link to="/statistics/sales" @click="mobileDrawerVisible = false">销售统计</router-link>
            <router-link to="/favorites" @click="mobileDrawerVisible = false">我的收藏</router-link>
            <router-link to="/chat" @click="mobileDrawerVisible = false">私信聊天</router-link>
            <router-link to="/notifications" @click="mobileDrawerVisible = false">通知中心</router-link>
            <router-link to="/disputes" @click="mobileDrawerVisible = false">我的纠纷</router-link>
          </template>
        </nav>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  Plus,
  Star,
  Bell,
  ChatDotRound,
  User,
  Wallet,
  ShoppingBag,
  List,
  Ticket,
  TrendCharts,
  Warning,
  Setting,
  SwitchButton,
  ArrowRight,
  ArrowDown,
  Menu as MenuIcon
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import { useRegisterConfigStore } from '@/stores/registerConfig'
import { markAllRead, markNotificationRead } from '@/api/notification'
import { notificationTypeMap, formatNotificationTime, getNotificationRoute } from '@/types/notification'
import type { NotificationItem } from '@/types/notification'
import XyfBrandLogo from '@/components/ui/XyfBrandLogo.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const chatStore = useChatStore()
const registerConfigStore = useRegisterConfigStore()

const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const notifyPopoverVisible = ref(false)
const mobileDrawerVisible = ref(false)

watch(
  () => route.query.keyword,
  (kw) => {
    searchKeyword.value = typeof kw === 'string' ? kw : ''
  }
)

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  router.push({
    path: '/services',
    query: kw ? { keyword: kw } : {}
  })
}

const handleMobileSearch = () => {
  mobileDrawerVisible.value = false
  handleSearch()
}

const goPublish = () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: '/service/publish' } })
    return
  }
  router.push('/service/publish')
}

const handleNotifyPopoverShow = () => {
  notificationStore.fetchRecentNotifications()
}

const goNotifications = () => {
  notifyPopoverVisible.value = false
  router.push('/notifications')
}

const handleMarkAllNotificationsRead = async () => {
  if (notificationStore.unreadCount === 0) return
  try {
    await markAllRead()
    notificationStore.markAllLocalRead()
    ElMessage.success('已标记全部通知为已读')
  } catch {
    // 拦截器处理
  }
}

const handleClickNotificationItem = async (item: NotificationItem) => {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id)
      notificationStore.markLocalRead(item.id)
    } catch {
      // 静默
    }
  }
  notifyPopoverVisible.value = false
  const routeConfig = getNotificationRoute(item)
  if (routeConfig) {
    router.push(routeConfig.path)
  }
}

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/user/center')
      break
    case 'wallet':
      router.push('/wallet')
      break
    case 'my-services':
      router.push('/my-services')
      break
    case 'my-buy-orders':
      router.push('/orders')
      break
    case 'my-sell-orders':
      router.push('/orders/sales')
      break
    case 'sales-stats':
      router.push('/statistics/sales')
      break
    case 'disputes':
      router.push('/disputes')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/')
      break
  }
}
</script>

<style scoped lang="scss">
.main-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--xyf-bg);
}

.main-header {
  position: sticky;
  top: 0;
  z-index: 900;
  height: var(--xyf-header-height);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--xyf-border-subtle);

  &__inner {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 32px;
    min-width: 0;

    @media (max-width: 640px) {
      gap: 10px;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    @media (max-width: 640px) {
      gap: 4px;
    }
  }
}

.mobile-menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border-radius: var(--xyf-radius-sm);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  color: var(--xyf-ink);
  cursor: pointer;
  align-items: center;
  justify-content: center;

  @media (max-width: 820px) {
    display: inline-flex;
  }
}

.brand-link {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 24px;

  @media (max-width: 820px) {
    display: none;
  }

  &__link {
    position: relative;
    padding: 6px 2px;
    font-size: 14.5px;
    font-weight: 500;
    color: var(--xyf-ink-2);
    transition: color var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
    }

    &.is-active {
      color: var(--xyf-ink);
      font-weight: 600;
    }
  }
}

.header-search {
  width: 240px;

  @media (max-width: 1080px) {
    width: 180px;
  }

  @media (max-width: 820px) {
    display: none;
  }

  :deep(.el-input__wrapper) {
    border-radius: var(--xyf-radius-pill);
    background: var(--xyf-surface-soft);
    box-shadow: none;
    border: 1px solid transparent;
    transition: all var(--xyf-transition);

    &:hover,
    &.is-focus {
      background: #ffffff;
      border-color: var(--xyf-border-strong);
    }
  }
}

.publish-cta {
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border-strong);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    background: var(--xyf-ink);
    color: #ffffff;
  }

  &.is-admin-return {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;

    &:hover {
      background: #2e2e34;
      border-color: #2e2e34;
    }
  }

  @media (max-width: 560px) {
    display: none;
  }
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--xyf-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--xyf-transition);

  @media (max-width: 480px) {
    width: 32px;
    height: 32px;
  }

  &:hover {
    color: var(--xyf-ink);
    background: var(--xyf-surface-soft);
  }

  :deep(.el-badge__content) {
    background: #ef4444;
    border: none;
  }
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 12px 3px 4px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    box-shadow: var(--xyf-shadow-xs);
  }

  &__name {
    max-width: 96px;
    font-size: 13px;
    font-weight: 500;
    color: var(--xyf-ink);

    @media (max-width: 640px) {
      display: none;
    }
  }

  &__caret {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.admin-role-chip {
  font-size: 11px;
  font-weight: 600;
  color: #18181b;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
  padding: 1px 7px;
  border-radius: 5px;
}

.main-content {
  flex: 1;
}

.main-footer {
  background: var(--xyf-surface-soft);
  border-top: 1px solid var(--xyf-border);
  margin-top: 0;
  padding: 56px 0 32px;

  &__inner {
    display: flex;
    justify-content: space-between;
    gap: 32px;
    flex-wrap: wrap;
    padding-bottom: 28px;
    border-bottom: 1px solid var(--xyf-border-subtle);
  }

  &__brand {
    max-width: 380px;

    .footer-note {
      margin-top: 12px;
      font-size: 13px;
      color: var(--xyf-ink-3);
      line-height: 1.65;
    }
  }

  &__links {
    display: flex;
    gap: 56px;
    flex-wrap: wrap;

    .link-col {
      display: flex;
      flex-direction: column;
      gap: 8px;

      h4 {
        font-size: 13px;
        font-weight: 600;
        color: var(--xyf-ink);
        margin-bottom: 4px;
      }

      a {
        font-size: 13px;
        color: var(--xyf-ink-2);

        &:hover {
          color: var(--xyf-primary);
        }
      }
    }
  }

  &__bottom {
    padding-top: 18px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 12.5px;
    color: var(--xyf-ink-3);

    .admin-entry {
      color: var(--xyf-ink-2);

      &:hover {
        color: var(--xyf-primary);
      }
    }
  }
}

.mobile-drawer {
  display: flex;
  flex-direction: column;
  gap: 18px;

  &__search {
    margin-top: 4px;
  }

  &__nav {
    display: flex;
    flex-direction: column;
    gap: 4px;

    a {
      padding: 10px 12px;
      border-radius: var(--xyf-radius-sm);
      font-size: 14.5px;
      font-weight: 500;
      color: var(--xyf-ink-2);

      &.router-link-exact-active {
        background: var(--xyf-primary-soft);
        color: var(--xyf-primary);
        font-weight: 600;
      }
    }

    .drawer-divider {
      height: 1px;
      background: var(--xyf-border);
      margin: 8px 0;
    }
  }
}

/* Global Popover Content */
.notify-panel {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid var(--xyf-border-subtle);
    font-weight: 600;
    font-size: 14.5px;
    color: var(--xyf-ink);
  }

  &__list {
    max-height: 300px;
    overflow-y: auto;
  }

  &__empty {
    padding: 36px 16px;
    text-align: center;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__foot {
    padding: 8px 12px;
    border-top: 1px solid var(--xyf-border-subtle);

    .view-all-btn {
      width: 100%;
      justify-content: center;
      color: var(--xyf-primary);
    }
  }
}

.notify-row {
  padding: 11px 16px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  cursor: pointer;
  transition: background var(--xyf-transition);

  &:hover {
    background: var(--xyf-surface-soft);
  }

  &.is-unread {
    background: var(--xyf-primary-soft);

    .notify-row__title {
      font-weight: 600;
    }
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
    font-size: 11.5px;
  }

  &__type {
    color: var(--xyf-primary);
    font-weight: 600;
  }

  &__time {
    color: var(--xyf-ink-3);
  }

  &__title {
    font-size: 13.5px;
    color: var(--xyf-ink);
  }
}
</style>

<style lang="scss">
.xyf-notify-popover {
  padding: 0 !important;
  border-radius: var(--xyf-radius-md) !important;
  border: 1px solid var(--xyf-border) !important;
  overflow: hidden;
}
</style>
