<template>
  <div class="favorites-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        kicker="SAVED WORKS"
        title="我收藏的技能服务"
        subtitle="随时对比心仪的校园技能方案，快捷下单或取消收藏"
      >
        <template #actions>
          <el-button type="primary" @click="router.push('/services')">发现更多服务</el-button>
        </template>
      </XyfPageHeader>

      <div v-if="loading" class="service-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 184px; border-radius: 10px" />
              <div style="padding: 14px 4px">
                <el-skeleton-item variant="h3" style="width: 72%" />
                <el-skeleton-item variant="text" style="width: 48%; margin-top: 10px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else-if="favorites.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="还没有收藏任何技能服务"
          description="遇到感兴趣的设计、开发或学业辅导服务时，点击收藏即可归档至此。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/services')">前往服务大厅</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <div class="service-grid">
          <ServiceCard
            v-for="item in favorites"
            :key="item.favoriteId"
            :service="item.service"
            :seller-name-fallback="item.sellerName"
            @click="(id) => router.push(`/services/${id}`)"
          >
            <template #overlay>
              <button
                type="button"
                class="uncollect-pill"
                title="取消收藏"
                @click.stop="handleUncollect(item)"
              >
                <el-icon><StarFilled /></el-icon>
                <span>已收藏</span>
              </button>
            </template>
          </ServiceCard>
        </div>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="fetchFavorites"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { getMyCollections, uncollectService } from '@/api/favorite'
import { notifyFallbackError } from '@/api/request'
import type { FavoriteListItem } from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import ServiceCard from '@/components/business/ServiceCard.vue'

const router = useRouter()

const loading = ref(true)
const favorites = ref<FavoriteListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12

async function fetchFavorites() {
  loading.value = true
  try {
    const res = await getMyCollections(pageNum.value, pageSize)
    favorites.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取收藏列表失败')
    favorites.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleUncollect(item: FavoriteListItem) {
  try {
    await uncollectService(item.service.id)
    ElMessage.success('已取消收藏')
    if (favorites.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1
    }
    await fetchFavorites()
  } catch (err) {
    notifyFallbackError(err, '取消收藏失败')
  }
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped lang="scss">
.service-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);
  padding: 12px;
}

.uncollect-pill {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(20, 25, 29, 0.72);
  color: #fbbf24;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background var(--xyf-transition);

  &:hover {
    background: var(--xyf-danger);
    color: #fff;
  }
}

.pagination-wrap {
  margin-top: 26px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1024px) {
  .service-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
