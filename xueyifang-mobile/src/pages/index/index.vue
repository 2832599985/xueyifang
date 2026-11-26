<template>
  <view class="page-wrap">
    <!-- Campus Switcher & Search Header -->
    <view class="hero-bar">
      <picker :range="campuses" range-key="campusName" @change="onCampusChange">
        <view class="campus-pill">
          <text class="campus-dot" />
          <text class="campus-title">{{ currentCampus?.campusName || '示范大学 · 主校区' }}</text>
          <text class="arrow">▾</text>
        </view>
      </picker>

      <view class="search-row">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索课程辅导、约拍、清灰维修、跑腿代办..."
          confirm-type="search"
          @confirm="loadFeed"
        />
        <view class="search-btn" @tap="loadFeed">搜索</view>
      </view>
    </view>

    <!-- Trust Value Banner -->
    <view class="trust-strip">
      <text>校园教育邮箱实名</text>
      <text>·</text>
      <text>电子围栏选点</text>
      <text>·</text>
      <text>蓝牙加密碰面核销</text>
    </view>

    <!-- Service Feed -->
    <view class="feed-list">
      <view
        v-for="item in services"
        :key="item.id"
        class="xyf-card service-item"
        @tap="goDetail(item.id)"
      >
        <view class="item-top">
          <text class="tag-badge">{{ item.tagName || '校园技能' }}</text>
          <text class="mode-badge" :class="{ 'is-offline': item.tradeType === 1 }">
            {{ item.tradeType === 1 ? '校内当面履约' : '线上担保交付' }}
          </text>
        </view>

        <text class="item-title">{{ item.serviceTitle }}</text>

        <view class="item-bottom">
          <view class="price-box">
            <text class="currency">¥</text>
            <text class="amount">{{ Number(item.price || 0).toFixed(2) }}</text>
          </view>
          <view class="seller-meta">
            <text>{{ item.sellerName || '认证同学' }}</text>
            <text>· 已完结 {{ item.salesCount || 0 }} 单</text>
          </view>
        </view>
      </view>

      <view v-if="!loading && services.length === 0" class="empty-box">
        <text>当前筛选下暂无在架技能服务</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { mobileApi, type CampusItem } from '@/api'
import { useMobileUserStore } from '@/stores/user'

const userStore = useMobileUserStore()
const campuses = ref<CampusItem[]>([])
const services = ref<any[]>([])
const keyword = ref('')
const loading = ref(false)

const currentCampus = computed(() =>
  campuses.value.find(c => c.id === userStore.activeCampusId) || campuses.value[0]
)

async function loadCampuses() {
  try {
    campuses.value = await mobileApi.listCampuses()
  } catch {
    campuses.value = []
  }
}

async function loadFeed() {
  loading.value = true
  try {
    const res = await mobileApi.listServices({ keyword: keyword.value.trim() || undefined })
    services.value = res.records || []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function onCampusChange(e: any) {
  const idx = Number(e.detail.value)
  const picked = campuses.value[idx]
  if (picked) {
    userStore.activeCampusId = picked.id
    uni.setStorageSync('XYF_CAMPUS_ID', picked.id)
    loadFeed()
  }
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/service/detail?id=${id}` })
}

onLoad(() => {
  loadCampuses()
  loadFeed()
})

onPullDownRefresh(() => {
  loadFeed()
})
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx;
}

.hero-bar {
  background: #18181b;
  border-radius: 28rpx;
  padding: 28rpx;
  color: #ffffff;
  margin-bottom: 18rpx;
}

.campus-pill {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  background: rgba(255, 255, 255, 0.12);
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  margin-bottom: 20rpx;

  .campus-dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: #10b981;
  }
}

.search-row {
  display: flex;
  gap: 14rpx;

  .search-input {
    flex: 1;
    height: 72rpx;
    background: #ffffff;
    color: #18181b;
    border-radius: 999rpx;
    padding: 0 28rpx;
    font-size: 26rpx;
  }

  .search-btn {
    height: 72rpx;
    line-height: 72rpx;
    padding: 0 32rpx;
    border-radius: 999rpx;
    background: #10b981;
    color: #ffffff;
    font-size: 26rpx;
    font-weight: 600;
  }
}

.trust-strip {
  display: flex;
  justify-content: center;
  gap: 12rpx;
  font-size: 22rpx;
  color: #52525b;
  margin-bottom: 20rpx;
}

.service-item {
  .item-top {
    display: flex;
    justify-content: space-between;
    margin-bottom: 14rpx;
    font-size: 22rpx;
  }

  .tag-badge {
    background: #f4f4f5;
    color: #18181b;
    padding: 4rpx 14rpx;
    border-radius: 8rpx;
    font-weight: 600;
  }

  .mode-badge {
    color: #0f766e;
    font-weight: 600;
    &.is-offline {
      color: #b45309;
    }
  }

  .item-title {
    display: block;
    font-size: 31rpx;
    font-weight: 700;
    color: #18181b;
    margin-bottom: 18rpx;
  }

  .item-bottom {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }

  .price-box {
    color: #18181b;
    font-weight: 800;
    .currency {
      font-size: 24rpx;
    }
    .amount {
      font-size: 38rpx;
    }
  }

  .seller-meta {
    font-size: 23rpx;
    color: #71717a;
  }
}

.empty-box {
  padding: 80rpx 0;
  text-align: center;
  color: #71717a;
}
</style>
