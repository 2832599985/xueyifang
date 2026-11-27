<template>
  <view class="page-wrap">
    <view class="role-tabs">
      <view class="tab" :class="{ active: role === 'BUYER' }" @tap="switchRole('BUYER')">我购买的</view>
      <view class="tab" :class="{ active: role === 'SELLER' }" @tap="switchRole('SELLER')">我售出的</view>
      <view v-if="role === 'SELLER'" class="scan-pill" @tap="scanBuyerDynamicQr">扫码极速核销</view>
    </view>

    <view
      v-for="item in orders"
      :key="item.id"
      class="xyf-card order-card"
      @tap="goOrderDetail(item.id)"
    >
      <view class="card-head">
        <text class="order-no">{{ item.orderNumber }}</text>
        <text class="status-txt">{{ statusLabel(item.orderStatus) }}</text>
      </view>
      <text class="svc-title">{{ item.serviceTitle }}</text>
      <view class="card-foot">
        <text class="party">{{ role === 'BUYER' ? `卖家：${item.sellerName || ''}` : `买家：${item.buyerName || ''}` }}</text>
        <text class="amt">¥{{ Number(item.totalAmount || 0).toFixed(2) }}</text>
      </view>
    </view>

    <view v-if="orders.length === 0" class="empty">暂无相关担保订单</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'

const role = ref<'BUYER' | 'SELLER'>('BUYER')
const orders = ref<any[]>([])

function statusLabel(s: number): string {
  const map: Record<number, string> = {
    1: '待支付',
    2: '担保托管中 · 待履约',
    3: '待验收确认',
    4: '已完结分账',
    5: '已取消',
    6: '已退款关闭'
  }
  return map[s] || '处理中'
}

async function loadOrders() {
  try {
    const res = await mobileApi.listMyOrders(role.value)
    orders.value = res.records || []
  } finally {
    uni.stopPullDownRefresh()
  }
}

function switchRole(next: 'BUYER' | 'SELLER') {
  role.value = next
  loadOrders()
}

function scanBuyerDynamicQr() {
  uni.scanCode({
    onlyFromCamera: true,
    scanType: ['qrCode'],
    success: async (res) => {
      const verifiedId = await mobileApi.verifyQrToken(res.result)
      uni.showToast({ title: '10s动态码核销成功', icon: 'success' })
      uni.navigateTo({ url: `/pages/order/detail?id=${verifiedId}` })
    }
  })
}

function goOrderDetail(id: number) {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

onShow(loadOrders)
onPullDownRefresh(loadOrders)
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx;
}
.role-tabs {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 22rpx;
  .tab {
    padding: 12rpx 28rpx;
    border-radius: 999rpx;
    background: #ffffff;
    font-size: 26rpx;
    font-weight: 600;
    &.active {
      background: #18181b;
      color: #ffffff;
    }
  }
  .scan-pill {
    margin-left: auto;
    padding: 12rpx 24rpx;
    border-radius: 999rpx;
    background: #18181b;
    color: #ffffff;
    font-size: 24rpx;
    font-weight: 600;
  }
}
.card-head,
.card-foot {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #71717a;
}
.status-txt {
  color: #18181b;
  font-weight: 700;
}
.svc-title {
  display: block;
  margin: 16rpx 0;
  font-size: 30rpx;
  font-weight: 700;
}
.amt {
  font-size: 32rpx;
  font-weight: 800;
  color: #18181b;
}
.empty {
  padding: 90rpx 0;
  text-align: center;
  color: #71717a;
}
</style>
