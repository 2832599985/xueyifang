<template>
  <div class="transactions-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        kicker="LEDGER"
        title="钱包交易流水"
        subtitle="完整记录校园钱包每一笔充值、担保冻结、结算与退款账目"
        :show-back="true"
        back-text="返回钱包概览"
        back-to="/wallet"
      />

      <section class="xyf-surface-card filter-card">
        <div class="filter-row">
          <span class="filter-label">流水类型</span>
          <div class="xyf-chip-group">
            <button
              v-for="opt in typeOptions"
              :key="opt.label"
              type="button"
              class="xyf-chip"
              :class="{ 'is-active': selectedType === opt.value }"
              @click="selectType(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </section>

      <section class="xyf-surface-card table-card">
        <el-skeleton v-if="loading" :rows="6" animated />

        <XyfEmptyState
          v-else-if="records.length === 0"
          title="暂无匹配的流水记录"
          description="尝试切换类型筛选查看全部收支明细。"
        />

        <el-table v-else :data="records" stripe>
          <el-table-column prop="transactionNo" label="流水号" min-width="150">
            <template #default="{ row }">
              <span class="xyf-mono">{{ row.transactionNo || `#${row.transactionId}` }}</span>
            </template>
          </el-table-column>

          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">
              <span class="xyf-tabular">{{ formatDateTime(row.createTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <XyfStatusBadge
                :tone="transactionTone(row.transactionType)"
                :label="getTransactionTypeText(row.transactionType)"
                :dot="false"
              />
            </template>
          </el-table-column>

          <el-table-column label="说明" min-width="200">
            <template #default="{ row }">
              <span>{{ row.remark || row.typeName || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="变动金额" width="130" align="right">
            <template #default="{ row }">
              <span
                class="amount-cell xyf-tabular"
                :class="isPositiveTransaction(row.transactionType) ? 'is-plus' : 'is-minus'"
              >
                {{ isPositiveTransaction(row.transactionType) ? '+' : '-' }}{{ formatPrice(Math.abs(row.amount)) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="余额变化" width="180" align="right">
            <template #default="{ row }">
              <span class="balance-diff xyf-tabular">
                {{ formatPrice(row.balanceBefore) }} → {{ formatPrice(row.balanceAfter) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="关联订单" width="110" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.relatedOrderId"
                text
                type="primary"
                size="small"
                @click="router.push(`/orders/${row.relatedOrderId}`)"
              >
                查看订单
              </el-button>
              <span v-else class="muted-dash">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="fetchList"
            @size-change="handleSizeChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTransactionHistory } from '@/api/wallet'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTime } from '@/utils/format'
import {
  TransactionType,
  getTransactionTypeText,
  type WalletTransaction
} from '@/types/wallet'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const typeOptions: Array<{ label: string; value: TransactionType | undefined }> = [
  { label: '全部流水', value: undefined },
  { label: '账户充值', value: TransactionType.RECHARGE },
  { label: '提现出账', value: TransactionType.WITHDRAW },
  { label: '订单支出', value: TransactionType.PAYMENT },
  { label: '担保冻结', value: TransactionType.FREEZE },
  { label: '解冻退回', value: TransactionType.UNFREEZE },
  { label: '服务收入', value: TransactionType.INCOME },
  { label: '退款入账', value: TransactionType.REFUND }
]

const loading = ref(true)
const selectedType = ref<TransactionType | undefined>(undefined)
const records = ref<WalletTransaction[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)

function isPositiveTransaction(type: TransactionType) {
  return (
    type === TransactionType.RECHARGE ||
    type === TransactionType.INCOME ||
    type === TransactionType.REFUND ||
    type === TransactionType.UNFREEZE
  )
}

function transactionTone(type: TransactionType) {
  if (type === TransactionType.INCOME || type === TransactionType.RECHARGE) return 'success'
  if (type === TransactionType.REFUND || type === TransactionType.UNFREEZE) return 'info'
  if (type === TransactionType.FREEZE) return 'warning'
  return 'accent'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getTransactionHistory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      transactionType: selectedType.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (err) {
    notifyFallbackError(err, '获取交易流水失败')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function selectType(type: TransactionType | undefined) {
  selectedType.value = type
  pageNum.value = 1
  fetchList()
}

function handleSizeChange() {
  pageNum.value = 1
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.filter-card {
  padding: 16px 20px;
  margin-bottom: 18px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--xyf-ink-muted);
}

.table-card {
  padding: 20px 22px;
}

.amount-cell {
  font-weight: 700;

  &.is-plus {
    color: var(--xyf-success);
  }

  &.is-minus {
    color: var(--xyf-accent);
  }
}

.balance-diff {
  font-size: 12.5px;
  color: var(--xyf-ink-muted);
}

.muted-dash {
  color: var(--xyf-ink-light);
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
