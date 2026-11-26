<template>
  <view class="login-wrap">
    <view class="xyf-card">
      <text class="brand">学艺坊 · 校园版</text>
      <text class="sub">输入在校学号登录（测试卖家：20240002 / 买家：20240003，密码：password123）</text>

      <input v-model="studentId" class="inp" placeholder="请输入学号" />
      <input v-model="password" class="inp" password placeholder="请输入密码" />

      <view class="xyf-btn-primary" style="margin-top: 24rpx" @tap="submitLogin">
        登录进入校园大厅
      </view>

      <view class="api-cfg">
        <text class="cfg-lbl">真机联调后端地址（Android / 微信真机请填局域网 IP 或 HTTPS 域名）：</text>
        <input v-model="apiBase" class="inp small" placeholder="http://192.168.x.x:8080/api" @blur="saveApiBase" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mobileApi } from '@/api'
import { getApiBaseUrl } from '@/utils/request'
import { useMobileUserStore } from '@/stores/user'

const userStore = useMobileUserStore()
const studentId = ref('20240003')
const password = ref('password123')
const apiBase = ref(getApiBaseUrl())

function saveApiBase() {
  if (apiBase.value.trim()) {
    uni.setStorageSync('XYF_API_BASE_URL', apiBase.value.trim())
  }
}

async function submitLogin() {
  saveApiBase()
  const res: any = await mobileApi.login(studentId.value.trim(), password.value)
  const token = res.token
  const user = res.userInfo || res
  userStore.setSession(token, user)
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    uni.switchTab({ url: '/pages/index/index' })
  }, 300)
}
</script>

<style scoped lang="scss">
.login-wrap {
  padding: 40rpx 28rpx;
}
.brand {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  margin-bottom: 10rpx;
}
.sub {
  display: block;
  font-size: 24rpx;
  color: #71717a;
  margin-bottom: 28rpx;
}
.inp {
  height: 82rpx;
  background: #f4f4f5;
  border-radius: 16rpx;
  padding: 0 24rpx;
  margin-bottom: 18rpx;
  font-size: 27rpx;
  &.small {
    height: 68rpx;
    font-size: 23rpx;
    margin-top: 10rpx;
  }
}
.api-cfg {
  margin-top: 36rpx;
  padding-top: 24rpx;
  border-top: 1rpx dashed #e4e4e7;
  .cfg-lbl {
    font-size: 22rpx;
    color: #71717a;
  }
}
</style>
