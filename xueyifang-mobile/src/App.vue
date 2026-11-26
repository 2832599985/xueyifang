<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useMobileUserStore } from './stores/user'

onLaunch(() => {
  const userStore = useMobileUserStore()
  userStore.hydrateFromStorage()
})
</script>

<style lang="scss">
page {
  background-color: #f6f7f9;
  color: #18181b;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  line-height: 1.5;
}

.xyf-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 22rpx;
  box-shadow: 0 4rpx 20rpx rgba(24, 24, 27, 0.04);
}

.xyf-btn-primary {
  background: #18181b;
  color: #ffffff;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
}

.xyf-btn-outline {
  background: #ffffff;
  color: #18181b;
  border: 2rpx solid #e4e4e7;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
}
</style>
