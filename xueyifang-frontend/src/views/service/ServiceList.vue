<template>
  <div class="service-list-page xyf-page">
    <div class="xyf-container">
      <header class="market-head">
        <div>
          <h1 class="market-head__title">全部技能服务</h1>
          <p class="market-head__sub">按分类、交付方式与热度发现校园里的专业同学</p>
        </div>
        <div class="market-head__meta xyf-tabular">共 {{ total }} 项在架</div>
      </header>

      <!-- Minimalist Borderless Filter Strip -->
      <section class="filter-strip">
        <div class="filter-tabs">
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': !filters.tagId }"
            @click="selectTag(undefined)"
          >
            全部
          </button>
          <button
            v-for="tag in dictStore.serviceTags || []"
            :key="tag.tagId"
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filters.tagId === tag.tagId }"
            @click="selectTag(tag.tagId)"
          >
            {{ tag.tagName }}
          </button>
        </div>

        <div class="filter-controls">
          <div class="pill-group">
            <button
              type="button"
              class="pill-btn"
              :class="{ 'is-active': !filters.tradeType }"
              @click="selectTradeType(undefined)"
            >
              全部方式
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ 'is-active': filters.tradeType === TradeType.ONLINE_ESCROW }"
              @click="selectTradeType(TradeType.ONLINE_ESCROW)"
            >
              线上担保
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ 'is-active': filters.tradeType === TradeType.OFFLINE }"
              @click="selectTradeType(TradeType.OFFLINE)"
            >
              线下会面
            </button>
          </div>

          <div class="pill-group">
            <button
              v-for="opt in sortOptions"
              :key="opt.value"
              type="button"
              class="pill-btn"
              :class="{ 'is-active': filters.sortBy === opt.value }"
              @click="selectSort(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div v-if="hasActiveFilters" class="active-summary">
          <el-tag
            v-if="filters.keyword"
            closable
            effect="plain"
            round
            @close="clearFilter('keyword')"
          >
            搜索：{{ filters.keyword }}
          </el-tag>
          <el-tag
            v-if="activeTagName"
            closable
            effect="plain"
            round
            @close="clearFilter('tagId')"
          >
            {{ activeTagName }}
          </el-tag>
          <el-tag
            v-if="filters.tradeType"
            closable
            effect="plain"
            round
            @close="clearFilter('tradeType')"
          >
            {{ getTradeTypeText(filters.tradeType) }}
          </el-tag>
          <el-tag
            v-if="filters.sellerId"
            closable
            effect="plain"
            round
            type="warning"
            @close="clearFilter('sellerId')"
          >
            发布者 #{{ filters.sellerId }}
          </el-tag>
          <button type="button" class="clear-all-link" @click="resetAllFilters">重置筛选</button>
        </div>
      </section>

      <!-- Loading Skeleton -->
      <div v-if="loading" class="service-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card">
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
import { useDictStore } from '@/stores/dict'
import { getServiceList } from '@/api/service'
import { notifyFallbackError } from '@/api/request'
import {
  TradeType,
  getTradeTypeText,
  type ServiceListItem,
  type ServiceSortBy
} from '@/types/service'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import ServiceCard from '@/components/business/ServiceCard.vue'

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const sortOptions: Array<{ label: string; value: ServiceSortBy }> = [
  { label: '最新发布', value: 'latest' },
  { label: '热度优先', value: 'view_desc' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

const VALID_SORTS = new Set<ServiceSortBy>(['latest', 'create_desc', 'price_asc', 'price_desc', 'view_desc'])

const loading = ref(false)
const serviceList = ref<ServiceListItem[]>([])
const total = ref(0)

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

const activeTagName = computed(() => {
  if (!filters.tagId) return ''
  return dictStore.getTagName(filters.tagId)
})

function syncStateFromRoute() {
  const q = route.query
  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''

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

function clearFilter(field: 'keyword' | 'tagId' | 'tradeType' | 'sellerId') {
  if (field === 'keyword') filters.keyword = ''
  else if (field === 'tagId') filters.tagId = undefined
  else if (field === 'tradeType') filters.tradeType = undefined
  else if (field === 'sellerId') filters.sellerId = undefined
  applyFilters(true)
}

function resetAllFilters() {
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
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;

  &__title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
  }

  &__sub {
    margin: 6px 0 0;
    font-size: 14.5px;
    color: var(--xyf-ink-2);
  }

  &__meta {
    font-size: 13.5px;
    color: var(--xyf-ink-3);
  }
}

.filter-strip {
  margin-bottom: 32px;
}

.filter-tabs {
  display: flex;
  align-items: center;
  gap: 28px;
  border-bottom: 1px solid var(--xyf-border);
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.filter-tab {
  position: relative;
  border: none;
  background: transparent;
  padding: 12px 2px;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--xyf-ink-2);
  white-space: nowrap;
  cursor: pointer;
  font-family: inherit;
  transition: color var(--xyf-transition);

  &:hover {
    color: var(--xyf-ink);
  }

  &.is-active {
    color: var(--xyf-ink);
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 2px;
      border-radius: 2px;
      background: var(--xyf-ink);
    }
  }
}

.filter-controls {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.pill-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.pill-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: var(--xyf-surface-soft);
  color: var(--xyf-ink-2);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    color: var(--xyf-ink);
    background: var(--xyf-surface-muted);
  }

  &.is-active {
    background: var(--xyf-ink);
    color: #ffffff;
    font-weight: 600;
  }
}

.active-summary {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.clear-all-link {
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--xyf-primary);
  cursor: pointer;
  font-family: inherit;
  padding: 2px 6px;

  &:hover {
    text-decoration: underline;
  }
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 36px 24px;
}

.skeleton-card {
  background: transparent;
}

.pagination-wrap {
  margin-top: 48px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1120px) {
  .service-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
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
