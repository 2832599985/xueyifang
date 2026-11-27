<template>
  <div class="service-list-page xyf-page">
    <div class="xyf-container">
      <header class="market-head">
        <h1 class="market-head__title">全部技能服务</h1>
        <div class="market-head__metric xyf-tabular">
          <strong>{{ total }}</strong>
          <span>项在架</span>
        </div>
      </header>

      <!-- Borderless Editorial Filter Strip -->
      <section class="filter-strip">
        <div class="xyf-tabs filter-tabs">
          <button
            type="button"
            class="xyf-tabs__btn"
            :class="{ 'is-active': !filters.tagId }"
            @click="selectTag(undefined)"
          >
            全部
          </button>
          <button
            v-for="tag in dictStore.serviceTags || []"
            :key="tag.tagId"
            type="button"
            class="xyf-tabs__btn"
            :class="{ 'is-active': filters.tagId === tag.tagId }"
            @click="selectTag(tag.tagId)"
          >
            {{ tag.tagName }}
          </button>
        </div>

        <div class="filter-controls">
          <div class="filter-controls__left">
            <div class="xyf-segmented" role="tablist" aria-label="交易方式筛选">
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': !filters.tradeType }"
                @click="selectTradeType(undefined)"
              >
                全部方式
              </button>
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': filters.tradeType === TradeType.ONLINE_ESCROW }"
                @click="selectTradeType(TradeType.ONLINE_ESCROW)"
              >
                线上担保
              </button>
              <button
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': filters.tradeType === TradeType.OFFLINE }"
                @click="selectTradeType(TradeType.OFFLINE)"
              >
                线下会面
              </button>
            </div>

            <div class="xyf-segmented" role="tablist" aria-label="排序方式">
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                type="button"
                class="xyf-segmented__btn"
                :class="{ 'is-active': filters.sortBy === opt.value }"
                @click="selectSort(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <div class="filter-controls__right">
            <el-input
              v-model="searchInput"
              placeholder="搜索技能、关键词"
              clearable
              :prefix-icon="Search"
              class="market-search"
              @keyup.enter="handleKeywordSearch"
              @clear="handleKeywordClear"
            />
          </div>
        </div>

        <div v-if="filters.keyword.trim() || filters.sellerId" class="context-filter-note">
          <span v-if="filters.keyword.trim()">当前搜索：“{{ filters.keyword.trim() }}”</span>
          <span v-if="filters.sellerId">创作者 #{{ filters.sellerId }} 的在架服务</span>
          <button type="button" class="clear-link" @click="resetAllFilters">清除条件</button>
        </div>
      </section>

      <!-- Loading Skeleton -->
      <div v-if="loading" class="service-grid">
        <div v-for="n in 8" :key="n" class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 184px; border-radius: 10px" />
              <div style="padding: 14px 4px">
                <el-skeleton-item variant="h3" style="width: 75%" />
                <el-skeleton-item variant="text" style="width: 50%; margin-top: 10px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="serviceList.length === 0" class="xyf-surface-card">
        <XyfEmptyState
          title="没有匹配到符合条件的技能服务"
          description="尝试更换关键词、切换交易方式或清空筛选条件查看更多校内创作者作品。"
        >
          <template #action>
            <el-button v-if="hasActiveFilters" type="primary" @click="resetAllFilters">
              查看全部在架服务
            </el-button>
          </template>
        </XyfEmptyState>
      </div>

      <!-- Service Grid -->
      <template v-else>
        <div class="service-grid">
          <ServiceCard
            v-for="(item, idx) in serviceList"
            :key="item.id"
            :service="item"
            :lazy="idx >= 4"
            @click="(id) => router.push(`/services/${id}`)"
          />
        </div>

        <div class="pagination-wrap" v-if="total > filters.pageSize">
          <el-pagination
            v-model:current-page="filters.pageNum"
            v-model:page-size="filters.pageSize"
            :total="total"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute, type LocationQueryRaw } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import { getServiceList } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import {
  TradeType,
  type ServiceListItem,
  type ServiceSortBy
} from '@/types/service'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import ServiceCard from '@/components/business/ServiceCard.vue'

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const sortOptions: Array<{ label: string; value: ServiceSortBy }> = [
  { label: '最新', value: 'latest' },
  { label: '热度', value: 'view_desc' },
  { label: '低价', value: 'price_asc' },
  { label: '高价', value: 'price_desc' }
]

const VALID_SORTS = new Set<ServiceSortBy>(['latest', 'create_desc', 'price_asc', 'price_desc', 'view_desc'])

const loading = ref(false)
const serviceList = ref<ServiceListItem[]>([])
const total = ref(0)
const searchInput = ref('')

const filters = reactive<{
  keyword: string
  tagId: number | undefined
  tradeType: number | undefined
  sortBy: ServiceSortBy
  sellerId: number | undefined
  pageNum: number
  pageSize: number
}>({
  keyword: '',
  tagId: undefined,
  tradeType: undefined,
  sortBy: 'latest',
  sellerId: undefined,
  pageNum: 1,
  pageSize: 12
})

const hasActiveFilters = computed(() =>
  Boolean(filters.keyword.trim() || filters.tagId || filters.tradeType || filters.sellerId)
)

function syncStateFromRoute() {
  const q = route.query
  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''
  searchInput.value = filters.keyword

  const parsedTag = Number(q.tagId)
  filters.tagId = Number.isFinite(parsedTag) && parsedTag > 0 ? parsedTag : undefined

  const parsedTrade = Number(q.tradeType)
  filters.tradeType =
    parsedTrade === TradeType.OFFLINE || parsedTrade === TradeType.ONLINE_ESCROW
      ? parsedTrade
      : undefined

  const rawSort = typeof q.sortBy === 'string' ? (q.sortBy as ServiceSortBy) : 'latest'
  filters.sortBy = VALID_SORTS.has(rawSort) ? rawSort : 'latest'

  const parsedSeller = Number(q.sellerId)
  filters.sellerId = Number.isFinite(parsedSeller) && parsedSeller > 0 ? parsedSeller : undefined

  const parsedPage = Number(q.pageNum)
  filters.pageNum = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1
}

function buildRouteQuery(): LocationQueryRaw {
  const q: LocationQueryRaw = {}
  if (filters.keyword.trim()) q.keyword = filters.keyword.trim()
  if (filters.tagId) q.tagId = String(filters.tagId)
  if (filters.tradeType) q.tradeType = String(filters.tradeType)
  if (filters.sortBy !== 'latest') q.sortBy = filters.sortBy
  if (filters.sellerId) q.sellerId = String(filters.sellerId)
  if (filters.pageNum > 1) q.pageNum = String(filters.pageNum)
  return q
}

async function fetchServices() {
  loading.value = true
  try {
    const res = await getServiceList({
      keyword: filters.keyword.trim() || undefined,
      tagId: filters.tagId,
      tradeType: filters.tradeType,
      sortBy: filters.sortBy,
      sellerId: filters.sellerId,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    })
    serviceList.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取服务列表失败')
    serviceList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters(resetPage = true) {
  if (resetPage) filters.pageNum = 1
  const nextQuery = buildRouteQuery()
  const currentSerialized = JSON.stringify(route.query)
  const nextSerialized = JSON.stringify(nextQuery)
  if (currentSerialized === nextSerialized) {
    fetchServices()
  } else {
    router.replace({ path: '/services', query: nextQuery })
  }
}

function selectTag(tagId?: number) {
  filters.tagId = tagId
  applyFilters(true)
}

function selectTradeType(tradeType?: number) {
  filters.tradeType = tradeType
  applyFilters(true)
}

function selectSort(sortBy: ServiceSortBy) {
  filters.sortBy = sortBy
  applyFilters(true)
}

function handleKeywordSearch() {
  filters.keyword = searchInput.value.trim()
  applyFilters(true)
}

function handleKeywordClear() {
  searchInput.value = ''
  filters.keyword = ''
  applyFilters(true)
}

function resetAllFilters() {
  searchInput.value = ''
  filters.keyword = ''
  filters.tagId = undefined
  filters.tradeType = undefined
  filters.sellerId = undefined
  filters.sortBy = 'latest'
  applyFilters(true)
}

function handlePageChange() {
  applyFilters(false)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSizeChange() {
  applyFilters(true)
}

watch(
  () => route.query,
  () => {
    syncStateFromRoute()
    fetchServices()
  },
  { immediate: true }
)

onMounted(() => {
  dictStore.fetchServiceTags().catch(() => {})
})
</script>

<style scoped lang="scss">
.market-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;

  &__title {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
    line-height: 1.15;
  }

  &__metric {
    display: inline-flex;
    align-items: baseline;
    gap: 6px;
    flex-shrink: 0;

    strong {
      font-size: 26px;
      font-weight: 700;
      color: var(--xyf-ink);
      letter-spacing: -0.02em;
      line-height: 1;
    }

    span {
      font-size: 13px;
      color: var(--xyf-ink-3);
    }
  }
}

.filter-strip {
  margin-bottom: 32px;
}

.filter-tabs {
  gap: 8px;
}

.filter-controls {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;

  &__left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  &__right {
    flex-shrink: 0;
  }
}

.market-search {
  width: 220px;
}

.context-filter-note {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--xyf-ink-2);
}

.clear-link {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--xyf-ink);
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
  font-family: inherit;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 32px 24px;
}

.skeleton-card {
  background: transparent;
}

.pagination-wrap {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1120px) {
  .service-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .market-head__title {
    font-size: 26px;
  }

  .market-search {
    width: 100%;
  }

  .filter-controls__right {
    width: 100%;
  }

  .service-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px 16px;
  }
}

@media (max-width: 540px) {
  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
