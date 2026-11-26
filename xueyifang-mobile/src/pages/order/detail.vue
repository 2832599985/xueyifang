<template>
  <view class="page-wrap" v-if="order">
    <!-- Status & Escrow Settlement Card -->
    <view class="xyf-card hero-status">
      <text class="status-big">{{ statusName }}</text>
      <text class="amount-big">¥{{ Number(order.totalAmount || 0).toFixed(2) }}</text>
      <text v-if="order.sellerSettleAmount && order.orderStatus === 4" class="fee-note">
        卖方实得 ¥{{ Number(order.sellerSettleAmount).toFixed(2) }}（平台技术服务费 ¥{{ Number(order.platformFee || 0).toFixed(2) }}）
      </text>
      <text v-else-if="order.verifyChannel" class="fee-note">
        核销方式：{{ order.verifyChannel }}
      </text>
    </view>

    <!-- Bidirectional Campus Map Meetup Negotiation Card -->
    <view class="xyf-card" v-if="order.tradeType === 1">
      <view class="sec-head">
        <text class="sec-title">线下会面位置双向确认</text>
        <text class="lock-badge" :class="{ locked: order.meetupConfirmed === 1 }">
          {{ order.meetupConfirmed === 1 ? '双方已锁定' : '可协商改约' }}
        </text>
      </view>

      <view class="spot-box" @tap="openMapNavigation">
        <text class="poi">{{ order.meetupPoiName || order.tradeLocationName || '待选定校内会面点' }}</text>
        <text class="addr">{{ order.meetupAddress || '点击可调起手机地图步行导航' }}</text>
      </view>

      <!-- Pending Proposal from Counterparty -->
      <view v-if="pendingProposal" class="proposal-banner">
        <text class="prop-title">
          {{ pendingProposal.proposerRole === 1 ? '买家' : '卖家' }}提议改至「{{ pendingProposal.poiName }}」
        </text>
        <view v-if="pendingProposal.proposerId !== userStore.userInfo?.id" class="prop-btns">
          <view class="btn-reject" @tap="respondProposal(pendingProposal.id, false)">婉拒</view>
          <view class="btn-accept" @tap="respondProposal(pendingProposal.id, true)">同意并锁定</view>
        </view>
        <text v-else class="wait-hint">等待对方点击确认...</text>
      </view>

      <view
        v-if="order.orderStatus === 2 || order.orderStatus === 3"
        class="xyf-btn-outline"
        style="margin-top: 18rpx"
        @tap="proposeNewMapSpot"
      >
        调起地图发起新会面点提案
      </view>
    </view>

    <!-- Offline Dual Verification Entry: BLE Proximity Crypto + 10s Dynamic Code -->
    <view class="xyf-card" v-if="order.tradeType === 1 && (order.orderStatus === 2 || order.orderStatus === 3)">
      <text class="sec-title">线下碰面防伪核销</text>
      <text class="ble-desc">
        两部手机靠近 3 米内开启蓝牙感应，自动完成双向挑战-应答加密签名与定位核验，彻底杜绝远程代扫或截图造假。
      </text>
      <view class="xyf-btn-primary" style="margin-top: 20rpx" @tap="goBleMeetup">
        开启蓝牙近场加密碰面核销
      </view>

      <view v-if="credential?.verifyCode" class="totp-box">
        <text class="totp-lbl">30秒动态备用核销码（{{ credential.codeRemainingSeconds }}s 后刷新）</text>
        <text class="totp-code">{{ credential.verifyCode }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'
import { useMobileUserStore } from '@/stores/user'

const userStore = useMobileUserStore()
const orderId = ref(0)
const order = ref<any>(null)
const proposals = ref<any[]>([])
const credential = ref<any>(null)

const pendingProposal = computed(() => proposals.value.find(p => p.proposalStatus === 0) || null)

const statusName = computed(() => {
  const s = order.value?.orderStatus
  if (s === 1) return '待买方担保支付'
  if (s === 2) return '资金已托管 · 待当面履约'
  if (s === 3) return '待验收确认'
  if (s === 4) return '已完成担保结算'
  if (s === 5) return '订单已取消'
  return '已退款关闭'
})

async function refreshAll() {
  if (!orderId.value) return
  order.value = await mobileApi.getOrderDetail(orderId.value)
  if (order.value?.tradeType === 1) {
    proposals.value = await mobileApi.listMeetupProposals(orderId.value).catch(() => [])
    if (order.value.buyerId === userStore.userInfo?.id && (order.value.orderStatus === 2 || order.value.orderStatus === 3)) {
      credential.value = await mobileApi.getOfflineCredential(orderId.value).catch(() => null)
    }
  }
}

function openMapNavigation() {
  if (!order.value?.meetupLat || !order.value?.meetupLng) return
  uni.openLocation({
    latitude: Number(order.value.meetupLat),
    longitude: Number(order.value.meetupLng),
    name: order.value.meetupPoiName || '约定校内交接点',
    address: order.value.meetupAddress || ''
  })
}

function proposeNewMapSpot() {
  uni.chooseLocation({
    success: async (res) => {
      await mobileApi.proposeMeetup(orderId.value, {
        latitude: Number(res.latitude.toFixed(6)),
        longitude: Number(res.longitude.toFixed(6)),
        poiName: res.name || '校内地图选点',
        detailAddress: res.address || ''
      })
      uni.showToast({ title: '选点提案已发送给对方', icon: 'success' })
      refreshAll()
    }
  })
}

async function respondProposal(proposalId: number, accept: boolean) {
  await mobileApi.respondMeetup(proposalId, accept, accept ? undefined : '距离较远，希望重新在地图选点')
  uni.showToast({ title: accept ? '已锁定会面位置' : '已婉拒提案', icon: 'success' })
  refreshAll()
}

function goBleMeetup() {
  uni.navigateTo({ url: `/pages/verify/ble-meetup?orderId=${orderId.value}` })
}

onLoad((q) => {
  orderId.value = Number(q?.id || 0)
})

onShow(refreshAll)
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx;
}
.hero-status {
  background: #18181b;
  color: #ffffff;
  .status-big {
    display: block;
    font-size: 26rpx;
    opacity: 0.82;
  }
  .amount-big {
    display: block;
    font-size: 48rpx;
    font-weight: 800;
    margin: 8rpx 0;
  }
  .fee-note {
    font-size: 22rpx;
    color: #a1a1aa;
  }
}
.sec-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.sec-title {
  font-size: 29rpx;
  font-weight: 700;
}
.lock-badge {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #fef3c7;
  color: #b45309;
  &.locked {
    background: #ecfdf5;
    color: #047857;
  }
}
.spot-box {
  background: #fafafa;
  border-radius: 16rpx;
  padding: 20rpx;
  .poi {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
  }
  .addr {
    font-size: 23rpx;
    color: #71717a;
  }
}
.proposal-banner {
  margin-top: 16rpx;
  padding: 18rpx;
  background: #fffbeb;
  border: 1rpx solid #fde68a;
  border-radius: 16rpx;
  .prop-title {
    display: block;
    font-size: 25rpx;
    font-weight: 600;
    color: #78350f;
    margin-bottom: 12rpx;
  }
  .prop-btns {
    display: flex;
    gap: 16rpx;
    .btn-reject,
    .btn-accept {
      flex: 1;
      text-align: center;
      padding: 12rpx 0;
      border-radius: 999rpx;
      font-size: 24rpx;
      font-weight: 600;
    }
    .btn-reject {
      background: #ffffff;
      color: #b91c1c;
    }
    .btn-accept {
      background: #18181b;
      color: #ffffff;
    }
  }
}
.ble-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #52525b;
}
.totp-box {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx dashed #e4e4e7;
  text-align: center;
  .totp-lbl {
    display: block;
    font-size: 22rpx;
    color: #71717a;
  }
  .totp-code {
    font-size: 46rpx;
    font-weight: 800;
    letter-spacing: 8rpx;
  }
}
</style>
