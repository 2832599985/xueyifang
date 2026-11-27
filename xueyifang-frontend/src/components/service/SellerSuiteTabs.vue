<template>
  <nav class="seller-suite-bar" aria-label="卖家工作台切换">
    <div class="xyf-segmented">
      <button
        v-for="item in items"
        :key="item.path"
        type="button"
        class="xyf-segmented__btn"
        :class="{ 'is-active': route.path === item.path }"
        @click="router.push(item.path)"
      >
        {{ item.label }}
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const items = [
  { path: '/my-services', label: '我的服务' },
  { path: '/orders/sales', label: '销售订单' },
  { path: '/statistics/sales', label: '销售统计' }
]
</script>

<style scoped lang="scss">
.seller-suite-bar {
  margin-bottom: 20px;
}
</style>
