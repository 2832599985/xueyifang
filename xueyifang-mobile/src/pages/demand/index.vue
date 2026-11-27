<template>
  <view class="demand-page">
    <view class="hero-card">
      <text class="hero-title">校内反向悬赏池</text>
      <text class="hero-sub">支持原生地图选点与米级电子围栏核销，认证创作者在线抢单报价</text>
      <button class="pub-btn" @click="showPublishModal = true">+ 发布悬赏需求</button>
    </view>

    <view v-for="item in demands" :key="item.id" class="d-card">
      <view class="d-head">
        <text class="u-tag" :class="`u-${item.urgencyLevel}`">
          {{ item.urgencyLevel === 3 ? '十万火急' : item.urgencyLevel === 2 ? '加急' : '常规' }}
        </text>
        <text class="d-status">{{ item.demandStatus === 0 ? '招募竞标中' : '已成单' }}</text>
      </view>
      <text class="d-title">{{ item.title }}</text>
      <text class="d-desc">{{ item.description }}</text>
      <view v-if="item.tradeType === 1 && item.meetupPoiName" class="d-spot">
        地标：{{ item.meetupPoiName }}（{{ item.meetupRadiusMeters || 80 }}m 核销围栏）
      </view>
      <view class="d-foot">
        <text class="d-budget">预算 ¥{{ item.budgetMin }} - ¥{{ item.budgetMax }}</text>
        <text class="d-bids">{{ item.bidCount || 0 }} 人报价</text>
      </view>
    </view>

    <!-- Native Publish Sheet -->
    <view v-if="showPublishModal" class="sheet-mask" @click.self="showPublishModal = false">
      <view class="sheet-box">
        <text class="sheet-title">发布校园悬赏</text>
        <input v-model="form.title" class="inp" placeholder="悬赏标题（如：周四晚高数突击2小时）" />
        <textarea v-model="form.description" class="area" placeholder="填写具体需求与验收标准..." />
        <view class="row-2">
          <input v-model.number="form.budgetMin" type="number" class="inp" placeholder="最低预算(元)" />
          <input v-model.number="form.budgetMax" type="number" class="inp" placeholder="最高预算(元)" />
        </view>
        <button class="loc-btn" @click="chooseMapSpot">
          {{ form.meetupPoiName ? `已选点：${form.meetupPoiName} (${form.meetupRadiusMeters}m)` : '调用原生地图标记校内交接点' }}
        </button>
        <view class="row-2">
          <button class="cancel-btn" @click="showPublishModal = false">取消</button>
          <button class="ok-btn" @click="submitMobileDemand">发布</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'

declare const uni: any

const demands = ref<any[]>([])
const showPublishModal = ref(false)
const form = reactive({
  title: '',
  description: '',
  budgetMin: 50,
  budgetMax: 120,
  tradeType: 1,
  urgencyLevel: 2,
  meetupLat: 30.514,
  meetupLng: 114.3544,
  meetupPoiName: '',
  meetupAddress: '',
  meetupRadiusMeters: 80
})

async function loadList() {
  try {
    const res = await mobileApi.listDemands({ pageNum: 1, pageSize: 20 })
    demands.value = res.records || []
  } catch {
    demands.value = []
  } finally {
    uni.stopPullDownRefresh()
  }
}

function chooseMapSpot() {
  uni.chooseLocation({
    success: (res: any) => {
      form.meetupLat = Number(Number(res.latitude).toFixed(6))
      form.meetupLng = Number(Number(res.longitude).toFixed(6))
      form.meetupPoiName = res.name || '校内地图选点'
      form.meetupAddress = res.address || ''
    }
  })
}

async function submitMobileDemand() {
  if (!form.title || !form.description) {
    uni.showToast({ title: '请填写标题与说明', icon: 'none' })
    return
  }
  if (!form.meetupPoiName) {
    form.meetupPoiName = '主图书馆监控区'
  }
  try {
    await mobileApi.publishDemand({ ...form, campusId: 1 })
    uni.showToast({ title: '悬赏发布成功', icon: 'success' })
    showPublishModal.value = false
    loadList()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发布失败', icon: 'none' })
  }
}

onShow(loadList)
onPullDownRefresh(loadList)
</script>

<style scoped lang="scss">
.demand-page {
  padding: 24rpx;
}
.hero-card {
  background: #18181b;
  color: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.hero-title {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
}
.hero-sub {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.75);
  margin: 10rpx 0 22rpx;
}
.pub-btn {
  background: #10b981;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  border-radius: 999rpx;
}
.d-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.d-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.u-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #fef3c7;
  color: #b45309;
}
.d-status {
  font-size: 22rpx;
  color: #059669;
}
.d-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #18181b;
}
.d-desc {
  display: block;
  font-size: 25rpx;
  color: #52525b;
  margin-top: 8rpx;
}
.d-spot {
  margin-top: 14rpx;
  padding: 10rpx 16rpx;
  background: #f4f4f5;
  border-radius: 12rpx;
  font-size: 22rpx;
  color: #52525b;
}
.d-foot {
  margin-top: 18rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.d-budget {
  font-size: 30rpx;
  font-weight: 800;
  color: #18181b;
}
.d-bids {
  font-size: 24rpx;
  color: #059669;
}
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
}
.sheet-box {
  width: 100%;
  background: #ffffff;
  border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx;
}
.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}
.inp,
.area {
  width: 100%;
  background: #f4f4f5;
  border-radius: 14rpx;
  padding: 18rpx;
  margin-bottom: 16rpx;
  font-size: 26rpx;
}
.row-2 {
  display: flex;
  gap: 16rpx;
}
.loc-btn {
  margin-bottom: 20rpx;
  font-size: 25rpx;
  background: #ecfdf5;
  color: #047857;
}
.ok-btn {
  flex: 1;
  background: #18181b;
  color: #ffffff;
}
.cancel-btn {
  flex: 1;
}
</style>
