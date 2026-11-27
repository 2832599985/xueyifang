<template>
  <view class="demand-page">
    <!-- Obsidian Bounty Header -->
    <view class="hero-card">
      <view class="hero-eyebrow">
        <text class="eyebrow-pill">C2B 反向悬赏</text>
        <text class="eyebrow-sub">原生地图围栏核销</text>
      </view>
      <text class="hero-title">校内定制悬赏大厅</text>
      <text class="hero-sub">发布考前突击、联调或跟拍急单，认证创作者在线报价抢单</text>
      <view class="pub-btn" @tap="showPublishModal = true">+ 发起校内悬赏需求</view>
    </view>

    <!-- Urgency Filter Pills -->
    <view class="filter-pills">
      <view
        v-for="f in urgencyTabs"
        :key="String(f.value)"
        class="f-pill"
        :class="{ 'is-active': activeUrgency === f.value }"
        @tap="activeUrgency = f.value"
      >
        {{ f.label }}
      </view>
    </view>

    <!-- Escrow Bounty Tickets -->
    <view v-for="item in filteredDemands" :key="item.id" class="bounty-ticket">
      <view class="d-head">
        <view class="d-tags">
          <text class="u-tag" :class="`u-${item.urgencyLevel}`">
            {{ item.urgencyLevel === 3 ? '十万火急' : item.urgencyLevel === 2 ? '加急优先' : '常规悬赏' }}
          </text>
          <text class="mode-tag">{{ item.tradeType === 1 ? '校内当面' : '线上远程' }}</text>
        </view>
        <text class="d-status" :class="{ 'is-open': item.demandStatus === 0 }">
          {{ item.demandStatus === 0 ? '招募竞标中' : '已选标成单' }}
        </text>
      </view>

      <text class="d-title">{{ item.title }}</text>
      <text class="d-desc">{{ item.description }}</text>

      <view v-if="item.tradeType === 1 && item.meetupPoiName" class="d-spot">
        📍 {{ item.meetupPoiName }} · {{ item.meetupRadiusMeters || 80 }}m 围栏核销
      </view>

      <view class="d-foot">
        <view>
          <text class="d-budget-lbl">悬赏预算</text>
          <text class="d-budget">¥{{ item.budgetMin }} – ¥{{ item.budgetMax }}</text>
        </view>
        <text class="d-bids">{{ item.bidCount || 0 }} 位行家报价 →</text>
      </view>
    </view>

    <view v-if="filteredDemands.length === 0" class="empty-tip">
      <text>当前筛选下暂无招募中的悬赏需求</text>
    </view>

    <!-- Native Publish Bottom Sheet -->
    <view v-if="showPublishModal" class="sheet-mask" @tap.self="showPublishModal = false">
      <view class="sheet-box">
        <view class="sheet-handle" />
        <text class="sheet-title">发布校园反向悬赏</text>

        <input v-model="form.title" class="inp" placeholder="悬赏标题（如：周四晚高数突击 2 小时）" />
        <textarea v-model="form.description" class="area" placeholder="填写具体背景、需求内容与验收标准..." />

        <view class="row-2">
          <input v-model.number="form.budgetMin" type="number" class="inp" placeholder="最低预算(元)" />
          <input v-model.number="form.budgetMax" type="number" class="inp" placeholder="最高预算(元)" />
        </view>

        <view class="loc-btn" @tap="chooseMapSpot">
          {{ form.meetupPoiName ? `📍 已标定：${form.meetupPoiName} (${form.meetupRadiusMeters}m)` : '调用原生地图标定校内交接点' }}
        </view>

        <view class="row-2">
          <view class="cancel-btn" @tap="showPublishModal = false">取消</view>
          <view class="ok-btn" @tap="submitMobileDemand">确认发布悬赏</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'

declare const uni: any

const demands = ref<any[]>([])
const activeUrgency = ref<number | undefined>(undefined)
const showPublishModal = ref(false)

const urgencyTabs = [
  { label: '全部悬赏', value: undefined },
  { label: '十万火急', value: 3 },
  { label: '加急优先', value: 2 },
  { label: '常规', value: 1 }
]

const filteredDemands = computed(() => {
  if (!activeUrgency.value) return demands.value
  return demands.value.filter(d => d.urgencyLevel === activeUrgency.value)
})

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
  background: linear-gradient(140deg, #18181b 0%, #111113 100%);
  color: #ffffff;
  border-radius: 30rpx;
  padding: 32rpx 30rpx;
  margin-bottom: 22rpx;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 14rpx;

  .eyebrow-pill {
    font-size: 21rpx;
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    background: rgba(200, 169, 126, 0.2);
    border: 1rpx solid rgba(200, 169, 126, 0.45);
    color: #e5c79e;
  }

  .eyebrow-sub {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.58);
  }
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
}

.hero-sub {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
  margin: 10rpx 0 26rpx;
}

.pub-btn {
  height: 78rpx;
  line-height: 78rpx;
  text-align: center;
  background: #ffffff;
  color: #18181b;
  font-size: 27rpx;
  font-weight: 700;
  border-radius: 999rpx;
}

.filter-pills {
  display: flex;
  gap: 12rpx;
  margin-bottom: 22rpx;
  overflow-x: auto;

  .f-pill {
    padding: 10rpx 24rpx;
    border-radius: 999rpx;
    background: #ffffff;
    border: 1rpx solid #e4e4e7;
    font-size: 24rpx;
    color: #52525b;
    white-space: nowrap;

    &.is-active {
      background: #18181b;
      border-color: #18181b;
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.bounty-ticket {
  background: #ffffff;
  border: 1rpx solid #e7e7ea;
  border-radius: 26rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.d-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}

.d-tags {
  display: flex;
  gap: 10rpx;
}

.u-tag {
  font-size: 21rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #f4f4f5;
  color: #52525b;

  &.u-3 {
    background: #fef2f2;
    color: #b91c1c;
  }

  &.u-2 {
    background: #faf6ef;
    color: #9a5b13;
  }
}

.mode-tag {
  font-size: 21rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #fafafa;
  border: 1rpx solid #e4e4e7;
  color: #52525b;
}

.d-status {
  font-size: 21rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #f4f4f5;
  color: #71717a;

  &.is-open {
    background: #18181b;
    color: #ffffff;
  }
}

.d-title {
  display: block;
  font-size: 31rpx;
  font-weight: 750;
  color: #111113;
  line-height: 1.4;
}

.d-desc {
  display: block;
  font-size: 25rpx;
  color: #52525b;
  margin-top: 8rpx;
  line-height: 1.5;
}

.d-spot {
  margin-top: 16rpx;
  padding: 12rpx 18rpx;
  background: #f7f7f8;
  border-radius: 14rpx;
  font-size: 22rpx;
  color: #52525b;
}

.d-foot {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx dashed #e4e4e7;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.d-budget-lbl {
  font-size: 22rpx;
  color: #8e8e98;
  margin-right: 10rpx;
}

.d-budget {
  font-size: 32rpx;
  font-weight: 800;
  color: #111113;
}

.d-bids {
  font-size: 23rpx;
  font-weight: 600;
  color: #52525b;
}

.empty-tip {
  padding: 72rpx 0;
  text-align: center;
  color: #8e8e98;
  font-size: 25rpx;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(9, 9, 11, 0.55);
  display: flex;
  align-items: flex-end;
}

.sheet-box {
  width: 100%;
  background: #ffffff;
  border-radius: 36rpx 36rpx 0 0;
  padding: 24rpx 32rpx 48rpx;
}

.sheet-handle {
  width: 72rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #e4e4e7;
  margin: 0 auto 24rpx;
}

.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 750;
  margin-bottom: 22rpx;
  color: #111113;
}

.inp,
.area {
  width: 100%;
  box-sizing: border-box;
  background: #f7f7f8;
  border: 1rpx solid #e4e4e7;
  border-radius: 18rpx;
  padding: 20rpx 22rpx;
  margin-bottom: 18rpx;
  font-size: 26rpx;
}

.area {
  height: 168rpx;
}

.row-2 {
  display: flex;
  gap: 16rpx;
}

.loc-btn {
  height: 78rpx;
  line-height: 78rpx;
  text-align: center;
  border-radius: 18rpx;
  margin-bottom: 24rpx;
  font-size: 25rpx;
  font-weight: 600;
  background: #faf6ef;
  border: 1rpx solid #e8dac4;
  color: #9a5b13;
}

.ok-btn,
.cancel-btn {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  text-align: center;
  border-radius: 999rpx;
  font-size: 27rpx;
  font-weight: 600;
}

.ok-btn {
  background: #18181b;
  color: #ffffff;
}

.cancel-btn {
  background: #f4f4f5;
  color: #52525b;
}
</style>
