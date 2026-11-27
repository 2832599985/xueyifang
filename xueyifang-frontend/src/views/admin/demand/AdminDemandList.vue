<template>
  <div class="admin-demand-page">
    <div class="xyf-surface-card toolbar-card">
      <div>
        <h2>校内悬赏需求巡检与风控治理</h2>
        <p class="sub">实时巡检全校学生发布的反向悬赏需求，支持过滤排查违规代考/作弊类需求并强制下架</p>
      </div>
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="reload">
          <el-option label="招募竞标中" :value="0" />
          <el-option label="已选标转单" :value="1" />
          <el-option label="发布者撤回" :value="2" />
          <el-option label="管理员关闭" :value="3" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索悬赏单号或标题..."
          clearable
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <div class="xyf-surface-card table-card">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="demandNo" label="悬赏单号" width="185" />
        <el-table-column prop="title" label="悬赏标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="campusName" label="所属校区" width="140" />
        <el-table-column label="预算区间" width="140">
          <template #default="{ row }">
            <span class="xyf-tabular">¥{{ row.budgetMin }} - ¥{{ row.budgetMax }}</span>
          </template>
        </el-table-column>
        <el-table-column label="交付与围栏" width="190">
          <template #default="{ row }">
            <span v-if="row.tradeType === 1">
              线下 · {{ row.meetupPoiName || '校内' }} ({{ row.meetupRadiusMeters || 80 }}m)
            </span>
            <span v-else>线上交付</span>
          </template>
        </el-table-column>
        <el-table-column prop="bidCount" label="竞标数" width="85" align="center" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.demandStatus === 0 ? 'success' : row.demandStatus === 1 ? 'primary' : 'info'">
              {{ statusText(row.demandStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.demandStatus === 0"
              link
              type="danger"
              @click="handleForceClose(row)"
            >
              违规关闭
            </el-button>
            <el-button
              v-else-if="row.matchedOrderId"
              link
              type="primary"
              @click="router.push(`/admin/orders/${row.matchedOrderId}`)"
            >
              查看关联订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminListDemands, adminCloseDemand } from '@/api/demand'
import { notifyFallbackError } from '@/api/request'
import type { DemandItem } from '@/types/demand'

const router = useRouter()
const loading = ref(false)
const list = ref<DemandItem[]>([])
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const statusFilter = ref<number | undefined>(undefined)
const keyword = ref('')

function statusText(st: number) {
  if (st === 0) return '招募竞标中'
  if (st === 1) return '已选标转单'
  if (st === 2) return '发布者撤回'
  return '管理员关闭'
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
  gap: 16px;
}

.toolbar-card {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;

  h2 {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 700;
  }

  .sub {
    margin: 0;
    font-size: 13px;
    color: var(--xyf-ink-muted);
  }
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.table-card {
  padding: 18px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
