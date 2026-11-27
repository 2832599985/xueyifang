<template>
  <div class="not-found-page">
    <div class="not-found-card">
      <div class="not-found-code xyf-mono">404</div>
      <h1 class="not-found-title">当前页面不存在或已迁移</h1>
      <p class="not-found-desc">
        请检查访问地址是否正确，或返回学艺坊服务大厅继续浏览校园技能。
      </p>
      <div class="not-found-actions">
        <el-button @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回上一页
        </el-button>
        <el-button type="primary" @click="router.push('/')">
          <el-icon><House /></el-icon>
          回到首页
        </el-button>
        <el-button @click="router.push('/services')">浏览服务大厅</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, House } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found-page {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.not-found-card {
  max-width: 520px;
  width: 100%;
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-lg);
  padding: 44px 32px;
  text-align: center;
  box-shadow: var(--xyf-shadow-sm);
}

.not-found-code {
  display: inline-block;
  padding: 6px 18px;
  border-radius: var(--xyf-radius-pill);
  background: var(--xyf-primary-soft);
  color: var(--xyf-primary-dark);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
}

.not-found-title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 700;
  color: var(--xyf-ink);
}

.not-found-desc {
  margin: 0 0 28px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--xyf-ink-muted);
}

.not-found-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
