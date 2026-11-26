<template>
  <view class="page-wrap" v-if="service">
    <view class="xyf-card">
      <text class="sec-title">{{ service.serviceTitle }}</text>
      <view class="row-line">
        <text>单价</text>
        <text class="bold">¥{{ Number(service.price || 0).toFixed(2) }}</text>
      </view>
      <view class="row-line">
        <text>购买数量</text>
        <view class="stepper">
          <view class="step-btn" @tap="quantity = Math.max(1, quantity - 1)">-</view>
          <text class="qty">{{ quantity }}</text>
          <view class="step-btn" @tap="quantity++">+</view>
        </view>
      </view>
    </view>

    <!-- Native Map Location Chooser + Campus Polygon Geofence Verification -->
    <view class="xyf-card" v-if="service.tradeType === 1">
      <text class="sec-title">约定校园会面位置（支持原生地图选点）</text>
      <view class="map-pick-box" @tap="chooseNativeMapLocation">
        <view>
          <text class="poi-name">{{ meetup.poiName || '点击调起腾讯/高德地图选择校内会面点' }}</text>
          <text class="poi-addr">{{ meetup.detailAddress || '系统自动校验是否处于校园安全电子围栏内' }}</text>
        </view>
        <text class="pick-cta">选点 ›</text>
      </view>

      <text class="fence-status" :class="{ 'is-err': !fenceInside }">
        {{ fenceHint }}
      </text>
    </view>

    <view class="xyf-card">
      <text class="sec-title">履约备注（选填）</text>
      <textarea
        v-model="remark"
        class="remark-box"
        placeholder="填写具体时间偏好或补充说明，下单后双方仍可发起位置变更确认..."
      />
    </view>

    <view class="bottom-bar">
      <view>
        <text class="lbl">微信担保托管合计</text>
        <text class="total">¥{{ (Number(service.price || 0) * quantity).toFixed(2) }}</text>
      </view>
      <view class="xyf-btn-primary pay-btn" @tap="submitAndEscrowPay">
        微信担保支付
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'

const serviceId = ref(0)
const slotId = ref<number | undefined>(undefined)
const service = ref<any>(null)
const quantity = ref(1)
const remark = ref('')
const fenceInside = ref(true)
const fenceHint = ref('所选点需位于本校安全电子围栏保护区内')

const meetup = reactive({
  latitude: undefined as number | undefined,
  longitude: undefined as number | undefined,
  poiName: '',
  detailAddress: ''
})

function chooseNativeMapLocation() {
  uni.chooseLocation({
    success: async (res) => {
      meetup.latitude = Number(res.latitude.toFixed(6))
      meetup.longitude = Number(res.longitude.toFixed(6))
      meetup.poiName = res.name || '校内地图选点'
      meetup.detailAddress = res.address || ''
      const check = await mobileApi.checkFence(service.value?.campusId || 1, meetup.latitude, meetup.longitude)
      fenceInside.value = check.inside
      fenceHint.value = check.message
    },
    fail: () => {
      // H5 或模拟器未授权时提供校内默认监控点兜底
      meetup.latitude = 30.51362
      meetup.longitude = 114.35581
      meetup.poiName = '主图书馆正门监控安全区'
      meetup.detailAddress = '一楼大厅东侧长椅'
      fenceInside.value = true
      fenceHint.value = '已选定校内预设监控安全交接点'
    }
  })
}

async function submitAndEscrowPay() {
  if (!service.value) return
  if (service.value.tradeType === 1 && !fenceInside.value) {
    uni.showToast({ title: '所选位置超出校园安全电子围栏，请重新选点', icon: 'none' })
    return
  }

  const orderId = await mobileApi.createOrder({
    serviceId: service.value.id,
    quantity: quantity.value,
    tradeType: service.value.tradeType,
    tradeLocationId: service.value.tradeLocationId,
    slotId: slotId.value,
    meetupLat: meetup.latitude,
    meetupLng: meetup.longitude,
    meetupPoiName: meetup.poiName || undefined,
    meetupAddress: meetup.detailAddress || undefined,
    remark: remark.value.trim() || undefined
  })

  // 拉起微信小程序 / Android App 原生担保支付收银台
  // #ifdef MP-WEIXIN
  const prepay = await mobileApi.escrowPrepay(orderId, 'WECHAT_MINI')
  if (prepay.sandboxCallbackSign) {
    await mobileApi.sandboxEscrowCallback(prepay.paymentNo, prepay.amount, prepay.sandboxCallbackSign)
  } else {
    await new Promise<void>((resolve, reject) => {
      uni.requestPayment({
        provider: 'wxpay',
        orderInfo: prepay.paymentNo,
        timeStamp: prepay.sdkPayParams.timeStamp,
        nonceStr: prepay.sdkPayParams.nonceStr,
        package: prepay.sdkPayParams.package,
        signType: prepay.sdkPayParams.signType as any,
        paySign: prepay.sdkPayParams.paySign,
        success: () => resolve(),
        fail: (err) => reject(err)
      })
    })
  }
  // #endif

  // #ifndef MP-WEIXIN
  const appPrepay = await mobileApi.escrowPrepay(orderId, 'WECHAT_APP')
  if (appPrepay.sandboxCallbackSign) {
    await mobileApi.sandboxEscrowCallback(appPrepay.paymentNo, appPrepay.amount, appPrepay.sandboxCallbackSign)
  }
  // #endif

  uni.showToast({ title: '担保支付成功', icon: 'success' })
  setTimeout(() => {
    uni.redirectTo({ url: `/pages/order/detail?id=${orderId}` })
  }, 400)
}

onLoad(async (query) => {
  serviceId.value = Number(query?.serviceId || 0)
  if (query?.slotId) slotId.value = Number(query.slotId)
  if (serviceId.value) {
    service.value = await mobileApi.getServiceDetail(serviceId.value)
    if (service.value?.tradeLocationName) {
      meetup.poiName = service.value.tradeLocationName
      meetup.latitude = service.value.tradeLocationLat
      meetup.longitude = service.value.tradeLocationLng
    }
  }
})
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx 24rpx 170rpx;
}
.sec-title {
  display: block;
  font-size: 29rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
}
.row-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  .bold {
    font-weight: 700;
  }
}
.stepper {
  display: flex;
  align-items: center;
  gap: 20rpx;
  .step-btn {
    width: 52rpx;
    height: 52rpx;
    line-height: 52rpx;
    text-align: center;
    background: #f4f4f5;
    border-radius: 12rpx;
    font-weight: 700;
  }
}
.map-pick-box {
  background: #fafafa;
  border: 2rpx solid #e4e4e7;
  border-radius: 18rpx;
  padding: 22rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .poi-name {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
  }
  .poi-addr {
    display: block;
    font-size: 23rpx;
    color: #71717a;
    margin-top: 6rpx;
  }
  .pick-cta {
    font-size: 25rpx;
    font-weight: 600;
    color: #059669;
  }
}
.fence-status {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #059669;
  &.is-err {
    color: #dc2626;
  }
}
.remark-box {
  width: 100%;
  height: 150rpx;
  background: #fafafa;
  border-radius: 14rpx;
  padding: 16rpx;
  box-sizing: border-box;
  font-size: 26rpx;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e4e4e7;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .lbl {
    display: block;
    font-size: 22rpx;
    color: #71717a;
  }
  .total {
    font-size: 38rpx;
    font-weight: 800;
  }
  .pay-btn {
    padding: 0 48rpx;
  }
}
</style>
