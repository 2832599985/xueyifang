<template>
  <view class="page-wrap">
    <view v-if="!userStore.isLoggedIn" class="xyf-card center-card">
      <text class="title">登录校园账号</text>
      <text class="sub">开启校园实名教育邮箱核验、履约信用分与担保收益提现</text>
      <view class="xyf-btn-primary" style="margin-top: 28rpx" @tap="goLogin">前往登录</view>
    </view>

    <template v-else>
      <view class="xyf-card profile-head">
        <view class="row-top">
          <view>
            <text class="name">{{ userStore.userInfo?.nickname }}</text>
            <text class="sid">学号：{{ userStore.userInfo?.studentId }}</text>
          </view>
          <view class="credit-pill">
            履约信用 {{ userStore.userInfo?.creditScore ?? 100 }}
          </view>
        </view>

        <view class="balance-strip">
          <view>
            <text class="lbl">可提现服务净收益</text>
            <text class="val">¥{{ Number(userStore.userInfo?.walletBalance || 0).toFixed(2) }}</text>
          </view>
          <text class="campus-lbl">{{ userStore.userInfo?.campusName || '示范大学主校区' }}</text>
        </view>
      </view>

      <!-- Campus .edu.cn Email OTP Verification Card -->
      <view class="xyf-card">
        <view class="sec-row">
          <text class="sec-title">校园教育邮箱（.edu.cn）实名核验</text>
          <text class="status-tag" :class="{ ok: userStore.userInfo?.eduVerified === 1 }">
            {{ userStore.userInfo?.eduVerified === 1 ? '已通过核验' : '未核验' }}
          </text>
        </view>

        <template v-if="userStore.userInfo?.eduVerified !== 1">
          <input
            v-model="eduEmail"
            class="inp"
            placeholder="输入本校教育邮箱，如 20240003@stu.demo.edu.cn"
          />
          <view class="otp-row">
            <input v-model="otpCode" class="inp flex-1" placeholder="6位邮箱验证码" />
            <view class="send-btn" @tap="handleSendOtp">获取验证码</view>
          </view>
          <view class="xyf-btn-primary" style="margin-top: 18rpx" @tap="handleConfirmOtp">
            验证并领取 +8 履约信用分
          </view>
        </template>
        <text v-else class="verified-mail">
          已绑定核验邮箱：{{ userStore.userInfo?.verifiedEduEmail }}
        </text>
      </view>

      <view class="xyf-btn-outline" @tap="handleLogout">退出当前账号</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { mobileApi } from '@/api'
import { useMobileUserStore } from '@/stores/user'

const userStore = useMobileUserStore()
const eduEmail = ref('')
const otpCode = ref('')

async function syncProfile() {
  if (!userStore.token) return
  try {
    const fresh = await mobileApi.getCurrentUser()
    userStore.setSession(userStore.token, fresh)
  } catch {
    // handled in request interceptor
  }
}

async function handleSendOtp() {
  if (!eduEmail.value.trim()) {
    uni.showToast({ title: '请输入 .edu.cn 校园邮箱', icon: 'none' })
    return
  }
  const res = await mobileApi.sendEduCode(eduEmail.value.trim())
  if (res.devPreviewCode) {
    otpCode.value = res.devPreviewCode
    uni.showToast({ title: `联调验证码已自动填入: ${res.devPreviewCode}`, icon: 'none' })
  } else {
    uni.showToast({ title: '验证码已发送至校园邮箱', icon: 'success' })
  }
}

async function handleConfirmOtp() {
  await mobileApi.confirmEduCode(eduEmail.value.trim(), otpCode.value.trim())
  uni.showToast({ title: '校园学籍邮箱核验通过！', icon: 'success' })
  await syncProfile()
}

function goLogin() {
  uni.navigateTo({ url: '/pages/auth/login' })
}

async function handleLogout() {
  try {
    await mobileApi.logout()
  } catch {
    // ignore logout network errors and clear local session anyway
  } finally {
    userStore.clearSession()
  }
}

onShow(syncProfile)
</script>

<style scoped lang="scss">
.page-wrap {
  padding: 24rpx;
}
.center-card {
  text-align: center;
  padding: 56rpx 36rpx;
  .title {
    display: block;
    font-size: 34rpx;
    font-weight: 800;
    margin-bottom: 12rpx;
  }
  .sub {
    font-size: 25rpx;
    color: #71717a;
  }
}
.profile-head {
  background: #18181b;
  color: #ffffff;
  .row-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 28rpx;
  }
  .name {
    display: block;
    font-size: 34rpx;
    font-weight: 800;
  }
  .sid {
    font-size: 23rpx;
    color: #a1a1aa;
  }
  .credit-pill {
    background: rgba(229, 199, 158, 0.18);
    border: 1rpx solid rgba(229, 199, 158, 0.45);
    color: #e5c79e;
    padding: 8rpx 20rpx;
    border-radius: 999rpx;
    font-size: 24rpx;
    font-weight: 700;
  }
  .balance-strip {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    padding-top: 20rpx;
    border-top: 1rpx solid rgba(255, 255, 255, 0.12);
    .lbl {
      display: block;
      font-size: 22rpx;
      color: #a1a1aa;
    }
    .val {
      font-size: 40rpx;
      font-weight: 800;
    }
    .campus-lbl {
      font-size: 23rpx;
      color: #d4d4d8;
    }
  }
}
.sec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
  .sec-title {
    font-size: 28rpx;
    font-weight: 700;
  }
  .status-tag {
    font-size: 22rpx;
    color: #b45309;
    &.ok {
      color: #18181b;
      font-weight: 700;
    }
  }
}
.inp {
  height: 76rpx;
  background: #f4f4f5;
  border-radius: 14rpx;
  padding: 0 22rpx;
  font-size: 26rpx;
  margin-bottom: 14rpx;
}
.otp-row {
  display: flex;
  gap: 14rpx;
  .flex-1 {
    flex: 1;
  }
  .send-btn {
    height: 76rpx;
    line-height: 76rpx;
    padding: 0 26rpx;
    background: #18181b;
    color: #ffffff;
    border-radius: 14rpx;
    font-size: 24rpx;
  }
}
.verified-mail {
  font-size: 25rpx;
  color: #52525b;
}
</style>
