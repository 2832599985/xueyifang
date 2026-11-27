<template>
  <div class="transactions-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="交易流水"
        :show-back="true"
        back-text="返回钱包概览"
        back-to="/wallet"
      >
        <template #actions v-if="wallet">
          <div class="balance-pill xyf-tabular">
            <span>可用 <strong>{{ formatPrice(wallet.walletBalance) }}</strong></span>
            <span class="pill-sep" />
            <span class="is-frozen">冻结 <strong>{{ formatPrice(wallet.frozenAmount) }}</strong></span>
          </div>
        </template>
      </XyfPageHeader>

      <XyfFilterBar>
        <template #tabs>
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
        </template>

        <template #actions>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            size="small"
            class="ledger-date-picker"
            @change="handleDateChange"
          />
        </template>
      </XyfFilterBar>

      <section class="xyf-surface-card ledger-card">
        <el-skeleton v-if="loading" :rows="6" animated />

        <XyfEmptyState
          v-else-if="records.length === 0"
          title="暂无匹配的流水记录"
          description="尝试切换流水类型或时间范围查看全部收支明细。"
        />

        <div v-else class="ledger-groups">
          <div v-for="group in groupedRecords" :key="group.dateKey" class="ledger-group">
            <div class="ledger-group__head xyf-tabular">
              <span>{{ group.dateLabel }}</span>
              <span class="ledger-group__count">{{ group.items.length }} 笔</span>
            </div>

            <div class="ledger-list">
              <div
                v-for="row in group.items"
                :key="row.transactionId"
                class="ledger-row"
              >
                <div class="ledger-row__left">
                  <div class="ledger-row__main">
                    <XyfStatusBadge
                      :tone="transactionTone(row.transactionType)"
                      :label="getTransactionTypeText(row.transactionType)"
                    />
                    <span class="ledger-title">{{ row.remark || row.typeName || '账户资金变动' }}</span>
                    <button
                      v-if="row.relatedOrderId"
                      type="button"
                      class="order-chip xyf-mono"
                      @click="router.push(`/orders/${row.relatedOrderId}`)"
                    >
                      订单 {{ row.relatedOrderNumber ? row.relatedOrderNumber.slice(-6) : `#${row.relatedOrderId}` }}
                    </button>
                  </div>

                  <div class="ledger-row__sub xyf-tabular">
                    <span>{{ formatTimeOnly(row.createTime) }}</span>
                    <span>·</span>
                    <span>余额 {{ formatPrice(row.balanceBefore) }} → {{ formatPrice(row.balanceAfter) }}</span>
                    <button
                      v-if="row.transactionNo"
                      type="button"
                      class="copy-no-btn xyf-mono"
                      title="复制流水号"
                      @click="copyTxNo(row.transactionNo)"
                    >
                      {{ row.transactionNo }}
                    </button>
                  </div>
                </div>

                <div class="ledger-row__right xyf-tabular">
                  <strong
                    class="amount-val"
                    :class="amountClass(row.transactionType)"
                  >
                    {{ isPositiveTransaction(row.transactionType) ? '+' : '−' }}{{ formatPrice(Math.abs(row.amount)) }}
                  </strong>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > pageSize" class="xyf-pagination">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[15, 30, 50]"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTransactionHistory, getWalletInfo } from '@/api/wallet'
import { notifyFallbackError } from '@/api/request'
import { formatPrice } from '@/utils/format'
import {
  TransactionType,
  getTransactionTypeText,
  type WalletInfo,
  type WalletTransaction
} from '@/types/wallet'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfFilterBar from '@/components/ui/XyfFilterBar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()

const typeOptions: Array<{ label: string; value: TransactionType | undefined }> = [
  { label: '全部', value: undefined },
  { label: '充值', value: TransactionType.RECHARGE },
  { label: '提现', value: TransactionType.WITHDRAW },
  { label: '支付', value: TransactionType.PAYMENT },
  { label: '冻结', value: TransactionType.FREEZE },
  { label: '解冻', value: TransactionType.UNFREEZE },
  { label: '收入', value: TransactionType.INCOME },
  { label: '退款', value: TransactionType.REFUND }
]

const loading = ref(true)
const wallet = ref<WalletInfo | null>(null)
const selectedType = ref<TransactionType | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
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

function transactionTone(type: TransactionType): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  if (type === TransactionType.INCOME || type === TransactionType.RECHARGE) return 'success'
  if (type === TransactionType.REFUND || type === TransactionType.UNFREEZE) return 'info'
  if (type === TransactionType.FREEZE) return 'warning'
  return 'neutral'
}

function amountClass(type: TransactionType) {
  if (type === TransactionType.FREEZE) return 'is-frozen'
  return isPositiveTransaction(type) ? 'is-plus' : 'is-minus'
}

function formatDateHeading(raw: string) {
  if (!raw) return '近期'
  const datePart = String(raw).slice(0, 10)
  const [y, m, d] = datePart.split('-')
  if (!y || !m || !d) return datePart
  const nowYear = String(new Date().getFullYear())
  return y === nowYear ? `${Number(m)}月${Number(d)}日` : `${y}年${Number(m)}月${Number(d)}日`
}

function formatTimeOnly(raw: string) {
  if (!raw) return ''
  const normalized = String(raw).replace('T', ' ')
  return normalized.length >= 16 ? normalized.slice(11, 16) : normalized
}

const groupedRecords = computed(() => {
  const groups: Array<{ dateKey: string; dateLabel: string; items: WalletTransaction[] }> = []
  const indexMap = new Map<string, number>()

  for (const item of records.value) {
    const key = String(item.createTime || '').slice(0, 10) || 'unknown'
    const existingIdx = indexMap.get(key)
    if (existingIdx !== undefined) {
      groups[existingIdx]!.items.push(item)
    } else {
      indexMap.set(key, groups.length)
      groups.push({
        dateKey: key,
        dateLabel: formatDateHeading(item.createTime),
        items: [item]
      })
    }
  }
  return groups
})

async function copyTxNo(no: string) {
  try {
    await navigator.clipboard.writeText(no)
    ElMessage.success('流水号已复制')
  } catch {
    ElMessage.info(no)
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getTransactionHistory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      transactionType: selectedType.value,
      startTime: dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : undefined
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

function handleDateChange() {
  pageNum.value = 1
  fetchList()
}

function handleSizeChange() {
  pageNum.value = 1
  fetchList()
}

onMounted(async () => {
  getWalletInfo()
    .then((info) => {
      wallet.value = info
    })
    .catch(() => {})
  await fetchList()
})
</script>

<style scoped lang="scss">
.balance-pill {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 7px 14px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 13px;
  color: var(--xyf-ink-2);

  strong {
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .is-frozen strong {
    color: var(--xyf-champagne);
  }

  .pill-sep {
    width: 1px;
    height: 14px;
    background: var(--xyf-border-strong);
  }
}

.ledger-date-picker {
  width: 240px;
}

.ledger-card {
  padding: 20px 24px;
}

.ledger-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ledger-group {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--xyf-border);
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
  }

  &__count {
    font-weight: 500;
  }
}

.ledger-list {
  display: flex;
  flex-direction: column;
}

.ledger-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  transition: background-color var(--xyf-transition);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--xyf-bg-subtle);

    .copy-no-btn {
      opacity: 1;
    }
  }

  &__left {
    display: flex;
    flex-direction: column;
    gap: 5px;
    min-width: 0;
    flex: 1;
  }

  &__main {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .ledger-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--xyf-ink);
  }

  .order-chip {
    border: 1px solid var(--xyf-border);
    background: #ffffff;
    border-radius: var(--xyf-radius-xs);
    padding: 1px 7px;
    font-size: 12px;
    color: var(--xyf-ink-2);
    cursor: pointer;

    &:hover {
      border-color: var(--xyf-primary);
      color: var(--xyf-ink);
    }
  }

  &__sub {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  .copy-no-btn {
    border: none;
    background: transparent;
    padding: 0 4px;
    font-size: 12px;
    color: var(--xyf-ink-4);
    cursor: pointer;
    opacity: 0;
    transition: opacity var(--xyf-transition), color var(--xyf-transition);

    &:hover {
      color: var(--xyf-ink);
    }
  }

  &__right {
    flex-shrink: 0;
    text-align: right;
  }

  .amount-val {
    font-size: 16px;
    font-weight: 700;

    &.is-plus {
      color: var(--xyf-success);
    }

    &.is-minus {
      color: var(--xyf-ink);
    }

    &.is-frozen {
      color: #9a5b13;
    }
  }
}

@media (hover: none) {
  .ledger-row .copy-no-btn {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .ledger-date-picker {
    width: 100%;
  }

  .ledger-card {
    padding: 16px;
  }
}
</style>
