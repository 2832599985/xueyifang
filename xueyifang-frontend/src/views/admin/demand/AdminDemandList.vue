<template>
  <div class="admin-demand-page">
    <XyfPageHeader
      kicker="C2B BOUNTY GOVERNANCE"
      title="反向悬赏需求巡检与合规治理"
      subtitle="实时监控全校学生发布的反向悬赏任务，排查学术代写/替考等违规内容并执行强制下架与审计留痕"
    >
      <template #actions>
        <el-button :loading="loading" @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
      </template>
    </XyfPageHeader>

    <!-- 4 KPI Tiles -->
    <section class="kpi-grid">
      <XyfStatTile
        label="当前检索悬赏总量"
        :value="total"
        hint="覆盖全校区反向定制需求"
      />
      <XyfStatTile
        label="本页招募竞标中"
        :value="openCount"
        hint="开放创作者在线报价"
      />
      <XyfStatTile
        label="本页已选标转单"
        :value="matchedCount"
        hint="已锁定平台担保冻结池"
      />
      <XyfStatTile
        label="本页违规关闭拦截"
        :value="closedCount"
        tone="danger"
        hint="学术合规风控强制下架"
      />
    </section>

    <!-- Segmented Status Filter + Search Bar -->
    <section class="xyf-surface-card filter-panel">
      <div class="xyf-segmented">
        <button
          v-for="opt in statusTabs"
          :key="String(opt.value)"
          type="button"
          class="xyf-segmented__btn"
          :class="{ 'is-active': statusFilter === opt.value }"
          @click="selectStatus(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索悬赏单号或标题关键词..."
          clearable
          :prefix-icon="Search"
          style="width: 260px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" plain @click="reload">检索</el-button>
      </div>
    </section>

    <!-- Main Inspection Table -->
    <section class="xyf-surface-card table-card">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%" @row-click="openInspectDrawer">
        <el-table-column label="悬赏单号 / 时间" width="195">
          <template #default="{ row }">
            <div class="xyf-mono cell-no">{{ row.demandNo }}</div>
            <div class="cell-sub xyf-tabular">{{ formatDateTime(row.createTime).slice(0, 16) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="悬赏标题与需求摘要" min-width="260">
          <template #default="{ row }">
            <div class="title-line">
              <span v-if="row.urgencyLevel === 3" class="u-badge">火急</span>
              <strong>{{ row.title }}</strong>
            </div>
            <div class="cell-sub ellipsis">{{ row.description }}</div>
          </template>
        </el-table-column>

        <el-table-column label="校区与交付围栏" width="210">
          <template #default="{ row }">
            <div class="cell-main">{{ row.campusName || '主校区' }}</div>
            <div class="cell-sub">
              <template v-if="row.tradeType === 1">
                线下 · {{ row.meetupPoiName || '校内地标' }} ({{ row.meetupRadiusMeters || 80 }}m)
              </template>
              <template v-else>线上凭证交付</template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="悬赏预算" width="145">
          <template #default="{ row }">
            <strong class="xyf-tabular">¥{{ row.budgetMin }} – ¥{{ row.budgetMax }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="竞标数" width="90" align="center">
          <template #default="{ row }">
            <span class="bid-pill xyf-tabular">{{ row.bidCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="125">
          <template #default="{ row }">
            <el-tag :type="row.demandStatus === 0 ? 'success' : row.demandStatus === 1 ? 'warning' : row.demandStatus === 3 ? 'danger' : 'info'">
              {{ statusText(row.demandStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="巡检操作" width="175" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link @click.stop="openInspectDrawer(row)">详情</el-button>
            <el-button
              v-if="row.demandStatus === 0"
              link
              type="danger"
              @click.stop="handleForceClose(row)"
            >
              违规关闭
            </el-button>
            <el-button
              v-else-if="row.matchedOrderId"
              link
              type="primary"
              @click.stop="router.push(`/admin/orders/${row.matchedOrderId}`)"
            >
              关联担保单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchData"
        />
      </div>
    </section>

    <!-- Slide-over Demand Inspection Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="悬赏需求合规巡检详情"
      size="520px"
    >
      <div v-if="activeDemand" class="inspect-drawer">
        <div class="inspect-meta xyf-tabular">
          <span class="xyf-mono">{{ activeDemand.demandNo }}</span>
          <el-tag size="small">{{ statusText(activeDemand.demandStatus) }}</el-tag>
        </div>

        <h3 class="inspect-title">{{ activeDemand.title }}</h3>

        <div class="inspect-kv xyf-tabular">
          <div><span>所属校区</span><strong>{{ activeDemand.campusName || '主校区' }}</strong></div>
          <div><span>预算区间</span><strong>¥{{ activeDemand.budgetMin }} – ¥{{ activeDemand.budgetMax }}</strong></div>
          <div><span>交付方式</span><strong>{{ activeDemand.tradeType === 1 ? '校内线下当面' : '线上远程交付' }}</strong></div>
          <div><span>报价方案</span><strong>{{ activeDemand.bidCount || 0 }} 份</strong></div>
        </div>

        <div class="inspect-desc">
          <span class="lbl">详细需求描述与验收标准</span>
          <p>{{ activeDemand.description }}</p>
        </div>

        <div v-if="activeDemand.tradeType === 1 && activeDemand.meetupLat && activeDemand.meetupLng" class="inspect-map">
          <span class="lbl">标定会面坐标：{{ activeDemand.meetupPoiName }}（{{ activeDemand.meetupRadiusMeters || 80 }}m 核销圈）</span>
          <RealCampusMap
            :height="200"
            :interactive="false"
            :latitude="Number(activeDemand.meetupLat)"
            :longitude="Number(activeDemand.meetupLng)"
            :radius-meters="activeDemand.meetupRadiusMeters || 80"
            :center-lat="Number(activeDemand.meetupLat)"
            :center-lng="Number(activeDemand.meetupLng)"
            :fence-valid="true"
          />
        </div>

        <div class="inspect-actions">
          <el-button
            v-if="activeDemand.demandStatus === 0"
            type="danger"
            style="width: 100%"
            @click="handleForceClose(activeDemand)"
          >
            判定违规并强制下架关闭
          </el-button>
          <el-button
            v-else-if="activeDemand.matchedOrderId"
            type="primary"
            style="width: 100%"
            @click="router.push(`/admin/orders/${activeDemand.matchedOrderId}`)"
          >
            查看转化的担保订单详情
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminListDemands, adminCloseDemand, getDemandDetail } from '@/api/demand'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { DemandItem } from '@/types/demand'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import RealCampusMap from '@/components/business/RealCampusMap.vue'

const router = useRouter()
const loading = ref(false)
const list = ref<DemandItem[]>([])
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const statusFilter = ref<number | undefined>(undefined)
const keyword = ref('')

const drawerVisible = ref(false)
const activeDemand = ref<DemandItem | null>(null)

const statusTabs = [
  { label: '全部状态', value: undefined },
  { label: '招募竞标中', value: 0 },
  { label: '已选标转单', value: 1 },
  { label: '发布者撤回', value: 2 },
  { label: '管理员关闭', value: 3 }
]

const openCount = computed(() => list.value.filter(i => i.demandStatus === 0).length)
const matchedCount = computed(() => list.value.filter(i => i.demandStatus === 1).length)
const closedCount = computed(() => list.value.filter(i => i.demandStatus === 3).length)

function statusText(st: number) {
  if (st === 0) return '招募竞标中'
  if (st === 1) return '已选标转单'
  if (st === 2) return '发布者撤回'
  return '管理员关闭'
}

function selectStatus(val?: number) {
  statusFilter.value = val
  reload()
}

function reload() {
  pageNum.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await adminListDemands({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: statusFilter.value,
      keyword: keyword.value.trim() || undefined
    })
    list.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (err) {
    notifyFallbackError(err, '加载悬赏巡检列表失败')
  } finally {
    loading.value = false
  }
}

async function openInspectDrawer(row: DemandItem) {
  activeDemand.value = row
  drawerVisible.value = true
  try {
    activeDemand.value = await getDemandDetail(row.id)
  } catch {
    // keep list row data
  }
}

async function handleForceClose(row: DemandItem) {
  try {
    const res = await ElMessageBox.prompt(
      `请输入关闭悬赏「${row.title}」的合规治理原因：`,
      '违规悬赏强制下架',
      {
        confirmButtonText: '确认关闭',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：涉嫌学术代写/替考违规需求，予以关闭'
      }
    )
    await adminCloseDemand(row.id, res.value || '违反平台校园悬赏规范')
    ElMessage.success('已强制关闭该违规悬赏')
    drawerVisible.value = false
    await fetchData()
  } catch {
    // cancelled
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.admin-demand-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.filter-panel {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;

  .search-box {
    display: flex;
    gap: 8px;
  }
}

.table-card {
  padding: 18px 20px;
}

.cell-no {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--xyf-ink);
}

.cell-main {
  font-size: 13px;
  font-weight: 600;
  color: var(--xyf-ink);
}

.cell-sub {
  font-size: 12px;
  color: var(--xyf-ink-3);
  margin-top: 2px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 6px;

  .u-badge {
    padding: 1px 6px;
    border-radius: 4px;
    background: #fef2f2;
    border: 1px solid #f7cece;
    color: #b91c1c;
    font-size: 10.5px;
    font-weight: 700;
    flex-shrink: 0;
  }

  strong {
    color: var(--xyf-ink);
  }
}

.bid-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  font-size: 12px;
  font-weight: 700;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* Inspection Drawer */
.inspect-drawer {
  display: flex;
  flex-direction: column;
  gap: 18px;

  .inspect-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .inspect-title {
    margin: 0;
    font-size: 18px;
    font-weight: 750;
    line-height: 1.4;
    color: var(--xyf-ink);
  }

  .inspect-kv {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    padding: 14px;
    border-radius: 12px;
    background: var(--xyf-surface-soft);
    border: 1px solid var(--xyf-border-subtle);

    div {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }

    strong {
      font-size: 13.5px;
      color: var(--xyf-ink);
    }
  }

  .lbl {
    display: block;
    font-size: 12px;
    font-weight: 650;
    color: var(--xyf-ink-3);
    margin-bottom: 8px;
  }

  .inspect-desc p {
    margin: 0;
    padding: 14px;
    border-radius: 12px;
    border: 1px solid var(--xyf-border);
    font-size: 13.5px;
    line-height: 1.65;
    white-space: pre-wrap;
    color: var(--xyf-ink-2);
  }

  .inspect-actions {
    margin-top: 8px;
  }
}
</style>
