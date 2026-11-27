<template>
  <view class="page-wrap" v-if="detail">
    <view class="xyf-card">
      <view class="price-head">
        <text class="price">¥{{ Number(detail.price || 0).toFixed(2) }}</text>
        <text class="mode-pill">{{ detail.tradeType === 1 ? '校园线下当面履约' : '线上担保交付' }}</text>
      </view>
      <text class="title">{{ detail.serviceTitle }}</text>
      <text class="desc">{{ detail.serviceDescription }}</text>
    </view>

    <!-- Real Alumni Edu-Verified & Credit Score Trust Card -->
    <view class="xyf-card trust-card">
      <view class="seller-head">
        <view>
          <text class="seller-name">{{ detail.sellerName || '校园服务同学' }}</text>
          <text class="seller-sub">{{ detail.sellerGrade || '在校生' }} · {{ detail.sellerProfessional || '校内专业' }}</text>
        </view>
        <view class="verify-pill" :class="{ 'is-verified': detail.sellerEduVerified === 1 }">
          {{ detail.sellerEduVerified === 1 ? '教育邮箱已核验' : '学籍待核验' }}
        </view>
      </view>

      <view class="metrics-row">
        <view class="metric">
          <text class="val">{{ detail.sellerCreditScore ?? 100 }}</text>
          <text class="lbl">履约信用分</text>
        </view>
        <view class="metric">
          <text class="val">{{ detail.sellerCompletedOrders ?? 0 }}</text>
          <text class="lbl">累计完结单</text>
        </view>
        <view class="metric">
          <text class="val">{{ detail.reviewCount ? Number(detail.averageRating || 5).toFixed(1) : '暂无' }}</text>
          <text class="lbl">真实评分</text>
        </view>
      </view>
    </view>

    <!-- Optional Appointment Time Slot Selector -->
    <view class="xyf-card" v-if="detail.availableSlots && detail.availableSlots.length > 0">
      <text class="sec-title">选择可预约时间档期</text>
      <view class="slot-list">
        <view
          v-for="slot in detail.availableSlots"
          :key="slot.id"
          class="slot-item"
          :class="{ 'is-picked': pickedSlotId === slot.id }"
          @tap="pickedSlotId = slot.id"
        >
          <text class="slot-time">{{ slot.slotStart }} - {{ slot.slotEnd.slice(11, 16) }}</text>
          <text class="slot-rem">余 {{ slot.remainingCapacity }} 名额</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="xyf-btn-primary full-btn" @tap="goCheckout">
        立即担保预约（支持地图选点与当面核销）
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'

const serviceId = ref(0)
const detail = ref<any>(null)
const pickedSlotId = ref<number | undefined>(undefined)

async function fetchDetail() {
  detail.value = await mobileApi.getServiceDetail(serviceId.value)
  if (detail.value?.availableSlots?.length) {
    pickedSlotId.value = detail.value.availableSlots[0].id
  }
}

function goCheckout() {
  const slotQuery = pickedSlotId.value ? `&slotId=${pickedSlotId.value}` : ''
  uni.navigateTo({
    url: `/pages/order/checkout?serviceId=${serviceId.value}${slotQuery}`
  })
}

onLoad((query) => {
  serviceId.value = Number(query?.id || 0)
  if (serviceId.value) fetchDetail()
})
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx 24rpx 160rpx;
}
.price-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
  .price {
    font-size: 44rpx;
    font-weight: 800;
    color: #18181b;
  }
  .mode-pill {
    font-size: 22rpx;
    background: #f4f4f5;
    padding: 6rpx 16rpx;
    border-radius: 999rpx;
  }
}
.title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.desc {
  display: block;
  font-size: 27rpx;
  color: #52525b;
  line-height: 1.7;
}
.seller-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22rpx;
  .seller-name {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
  }
  .seller-sub {
    font-size: 23rpx;
    color: #71717a;
  }
  .verify-pill {
    font-size: 22rpx;
    padding: 6rpx 16rpx;
    border-radius: 999rpx;
    background: #f4f4f5;
    color: #52525b;
    &.is-verified {
      background: #faf6ef;
      color: #9a5b13;
      font-weight: 600;
    }
  }
}
.metrics-row {
  display: flex;
  justify-content: space-around;
  background: #fafafa;
  border-radius: 18rpx;
  padding: 20rpx 0;
  .metric {
    text-align: center;
    .val {
      display: block;
      font-size: 32rpx;
      font-weight: 800;
    }
    .lbl {
      font-size: 22rpx;
      color: #71717a;
    }
  }
}
.sec-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.slot-item {
  padding: 18rpx 22rpx;
  border: 2rpx solid #e4e4e7;
  border-radius: 16rpx;
  margin-bottom: 12rpx;
  display: flex;
  justify-content: space-between;
  font-size: 25rpx;
  &.is-picked {
    border-color: #18181b;
    background: #18181b;
    color: #ffffff;
  }
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e4e4e7;
}
</style>
