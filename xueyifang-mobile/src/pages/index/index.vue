<template>
  <view class="page-wrap">
    <!-- Editorial Obsidian Campus Stage -->
    <view class="hero-stage">
      <view class="hero-top">
        <picker :range="campuses" range-key="campusName" @change="onCampusChange">
          <view class="campus-pill">
            <text class="campus-dot" />
            <text class="campus-title">{{ currentCampus?.campusName || '示范大学 · 主校区' }}</text>
            <text class="arrow">▾</text>
          </view>
        </picker>
        <text class="escrow-tag">担保托管</text>
      </view>

      <text class="hero-h1">找到身边的校园行家</text>

      <view class="search-capsule">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索课程辅导、约拍、硬件联调、清灰..."
          confirm-type="search"
          @confirm="loadFeed"
        />
        <view class="search-btn" @tap="loadFeed">探索</view>
      </view>
    </view>

    <!-- Mode Filter Pills -->
    <view class="mode-strip">
      <view
        class="m-pill"
        :class="{ 'is-active': activeMode === undefined }"
        @tap="selectMode(undefined)"
      >
        全部技能
      </view>
      <view
        class="m-pill"
        :class="{ 'is-active': activeMode === 1 }"
        @tap="selectMode(1)"
      >
        校内当面核销
      </view>
      <view
        class="m-pill"
        :class="{ 'is-active': activeMode === 2 }"
        @tap="selectMode(2)"
      >
        线上远程担保
      </view>
    </view>

    <!-- Service Feed -->
    <view class="feed-list">
      <view
        v-for="item in filteredServices"
        :key="item.id"
        class="xyf-card service-ticket"
        @tap="goDetail(item.id)"
      >
        <view class="item-top">
          <view class="badge-row">
            <text class="tag-badge">{{ item.tagName || '校园技能' }}</text>
            <text class="mode-badge" :class="{ 'is-offline': item.tradeType === 1 }">
              {{ item.tradeType === 1 ? '校内当面' : '线上担保' }}
            </text>
          </view>
          <text class="sales-txt">已完结 {{ item.salesCount || 0 }} 单</text>
        </view>

        <text class="item-title">{{ item.serviceTitle }}</text>

        <view class="item-bottom">
          <view class="seller-meta">
            <view class="avatar-dot">{{ (item.sellerName || '同').slice(0, 1) }}</view>
            <text class="seller-name">{{ item.sellerName || '认证同学' }}</text>
          </view>

          <view class="price-box">
            <text class="currency">¥</text>
            <text class="amount">{{ Number(item.price || 0).toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <view v-if="!loading && filteredServices.length === 0" class="empty-box">
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
const activeMode = ref<number | undefined>(undefined)
const loading = ref(false)

const currentCampus = computed(() =>
  campuses.value.find(c => c.id === userStore.activeCampusId) || campuses.value[0]
)

const filteredServices = computed(() => {
  if (!activeMode.value) return services.value
  return services.value.filter(s => s.tradeType === activeMode.value)
})

function selectMode(mode?: number) {
  activeMode.value = mode
}

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

.hero-stage {
  background: linear-gradient(140deg, #18181b 0%, #111113 100%);
  border-radius: 32rpx;
  padding: 32rpx 30rpx;
  color: #ffffff;
  margin-bottom: 22rpx;
  box-shadow: 0 20rpx 44rpx rgba(9, 9, 11, 0.16);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.campus-pill {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  background: rgba(255, 255, 255, 0.1);
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 23rpx;

  .campus-dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: #e5c79e;
  }
}

.escrow-tag {
  font-size: 21rpx;
  color: #e5c79e;
  background: rgba(200, 169, 126, 0.16);
  border: 1rpx solid rgba(200, 169, 126, 0.36);
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}

.hero-h1 {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 24rpx;
}

.search-capsule {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 999rpx;
  padding: 8rpx 8rpx 8rpx 28rpx;

  .search-input {
    flex: 1;
    height: 68rpx;
    color: #18181b;
    font-size: 26rpx;
  }

  .search-btn {
    height: 68rpx;
    line-height: 68rpx;
    padding: 0 34rpx;
    border-radius: 999rpx;
    background: #18181b;
    color: #ffffff;
    font-size: 25rpx;
    font-weight: 600;
  }
}

.mode-strip {
  display: flex;
  gap: 14rpx;
  margin-bottom: 22rpx;

  .m-pill {
    padding: 12rpx 26rpx;
    border-radius: 999rpx;
    background: #ffffff;
    border: 1rpx solid #e4e4e7;
    font-size: 24rpx;
    font-weight: 500;
    color: #52525b;

    &.is-active {
      background: #18181b;
      border-color: #18181b;
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.service-ticket {
  border: 1rpx solid #e7e7ea;

  .item-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14rpx;
  }

  .badge-row {
    display: flex;
    gap: 10rpx;
  }

  .tag-badge {
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    background: #f4f4f5;
    font-size: 22rpx;
    font-weight: 600;
    color: #18181b;
  }

  .mode-badge {
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    background: #fafafa;
    border: 1rpx solid #e4e4e7;
    font-size: 22rpx;
    color: #52525b;

    &.is-offline {
      background: #faf6ef;
      border-color: #e8dac4;
      color: #9a5b13;
    }
  }

  .sales-txt {
    font-size: 22rpx;
    color: #8e8e98;
  }

  .item-title {
    display: block;
    font-size: 31rpx;
    font-weight: 700;
    color: #111113;
    line-height: 1.4;
    margin-bottom: 20rpx;
  }

  .item-bottom {
    padding-top: 18rpx;
    border-top: 1rpx dashed #e4e4e7;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .seller-meta {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .avatar-dot {
      width: 40rpx;
      height: 40rpx;
      border-radius: 50%;
      background: #18181b;
      color: #ffffff;
      font-size: 20rpx;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .seller-name {
      font-size: 24rpx;
      color: #52525b;
    }
  }

  .price-box {
    color: #111113;

    .currency {
      font-size: 24rpx;
      font-weight: 700;
    }

    .amount {
      font-size: 34rpx;
      font-weight: 800;
    }
  }
}

.empty-box {
  padding: 80rpx 20rpx;
  text-align: center;
  color: #8e8e98;
  font-size: 26rpx;
}
</style>
