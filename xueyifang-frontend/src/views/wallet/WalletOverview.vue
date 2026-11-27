<template>
  <div class="wallet-page xyf-page">
    <div class="xyf-container">
      <header class="wallet-head">
        <div>
          <h1>我的钱包</h1>
          <p>实时查看可用余额、担保托管资金与收支明细</p>
        </div>
        <button type="button" class="all-ledger-btn" @click="router.push('/wallet/transactions')">
          全部流水记录
          <el-icon><ArrowRight /></el-icon>
        </button>
      </header>

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <template v-else-if="wallet">
        <!-- Modern Fintech Split Stage: 3D Titanium Campus Card + Animated Escrow Ring -->
        <section class="wallet-stage">
          <!-- Interactive 3D Brushed Obsidian Titanium Virtual Card -->
          <div
            class="campus-card-wrap"
            @mousemove="onCardMove"
            @mouseleave="onCardLeave"
          >
            <div class="campus-card" :style="cardTransformStyle">
              <!-- Live Cursor Specular Metallic Sheen -->
              <div class="campus-card__sheen" :style="sheenStyle" />

              <!-- Animated Topographic Silk Contour SVG -->
              <svg
                class="campus-card__waves"
                viewBox="0 0 560 320"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true"
              >
                <path
                  d="M-30 220 C 90 150, 190 270, 320 190 C 430 122, 500 185, 590 130"
                  stroke="rgba(255,255,255,0.09)"
                  stroke-width="1.2"
                  class="wave-drift-1"
                />
                <path
                  d="M-30 250 C 110 185, 220 295, 350 218 C 460 152, 515 215, 590 165"
                  stroke="rgba(229,199,158,0.14)"
                  stroke-width="1.2"
                  class="wave-drift-2"
                />
                <path
                  d="M-30 185 C 120 120, 210 230, 340 155 C 450 92, 510 150, 590 95"
                  stroke="rgba(255,255,255,0.05)"
                  stroke-width="1"
                  class="wave-drift-3"
                />
                <circle cx="486" cy="56" r="96" stroke="rgba(255,255,255,0.05)" stroke-width="1" />
                <circle cx="486" cy="56" r="64" stroke="rgba(229,199,158,0.09)" stroke-width="1" stroke-dasharray="4 6" class="card-ring-spin" />
              </svg>

              <!-- Card Top Bar: Emblem + Animated Contactless Signal -->
              <div class="campus-card__top">
                <div class="card-brand">
                  <XyfBrandLogo theme="light" compact title="学艺坊" subtitle="" />
                  <span class="card-edition">校园担保钱包</span>
                </div>

                <!-- Animated Contactless Wave SVG -->
                <svg class="contactless-svg" viewBox="0 0 28 28" fill="none" aria-hidden="true">
                  <path d="M9.5 18.5C11.2 16 11.2 12 9.5 9.5" stroke="rgba(255,255,255,0.45)" stroke-width="1.8" stroke-linecap="round" class="nfc-wave-1" />
                  <path d="M13.5 21C16.2 17 16.2 11 13.5 7" stroke="rgba(255,255,255,0.65)" stroke-width="1.8" stroke-linecap="round" class="nfc-wave-2" />
                  <path d="M17.5 23.5C21.2 18.1 21.2 9.9 17.5 4.5" stroke="#E5C79E" stroke-width="1.8" stroke-linecap="round" class="nfc-wave-3" />
                </svg>
              </div>

              <!-- Card Center: Available Balance -->
              <div class="campus-card__center">
                <span class="balance-caption">可用余额</span>
                <div class="balance-figure xyf-tabular">
                  {{ formatPrice(wallet.walletBalance) }}
                </div>
              </div>

              <!-- Card Footer: Metallic EMV Chip + Holder Info -->
              <div class="campus-card__bottom">
                <div class="holder-group">
                  <!-- Brushed Gold/Titanium EMV Chip SVG -->
                  <svg class="emv-chip" viewBox="0 0 42 32" fill="none" aria-hidden="true">
                    <defs>
                      <linearGradient id="chip-metal" x1="0" y1="0" x2="42" y2="32" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#F3E2C7" />
                        <stop offset="50%" stop-color="#C8A97E" />
                        <stop offset="100%" stop-color="#8F7552" />
                      </linearGradient>
                    </defs>
                    <rect x="1" y="1" width="40" height="30" rx="6" fill="url(#chip-metal)" />
                    <path d="M1 11H15M27 11H41M1 21H15M27 21H41M15 1V31M27 1V31" stroke="rgba(24,24,27,0.35)" stroke-width="1.1" />
                  </svg>

                  <div class="holder-meta">
                    <span class="holder-name">{{ holderName }}</span>
                    <span class="holder-id xyf-mono">{{ holderStudentId }}</span>
                  </div>
                </div>

                <span class="escrow-pill">
                  <span class="escrow-dot" />
                  担保托管已开启
                </span>
              </div>
            </div>
          </div>

          <!-- Right Asset Breakdown + Animated SVG Escrow Ratio Ring + Single Clean Action Dock -->
          <div class="asset-panel">
            <div class="asset-panel__top">
              <div class="ring-box">
                <svg class="ratio-ring" viewBox="0 0 112 112" aria-hidden="true">
                  <circle
                    cx="56"
                    cy="56"
                    r="44"
                    fill="none"
                    stroke="#F1F1F4"
                    stroke-width="9"
                  />
                  <!-- Available Balance Arc (Obsidian) -->
                  <circle
                    cx="56"
                    cy="56"
                    r="44"
                    fill="none"
                    stroke="#18181B"
                    stroke-width="9"
                    stroke-linecap="round"
                    :stroke-dasharray="ringCircumference"
                    :stroke-dashoffset="availableDashOffset"
                    transform="rotate(-90 56 56)"
                    class="ring-arc"
                  />
                  <!-- Frozen Escrow Arc (Warm Champagne Gold) -->
                  <circle
                    v-if="frozenRatio > 0"
                    cx="56"
                    cy="56"
                    r="44"
                    fill="none"
                    stroke="#D97706"
                    stroke-width="9"
                    stroke-linecap="round"
                    :stroke-dasharray="ringCircumference"
                    :stroke-dashoffset="frozenDashOffset"
                    :transform="`rotate(${frozenStartDeg} 56 56)`"
                    class="ring-arc"
                  />
                </svg>
                <div class="ring-center">
                  <strong class="xyf-tabular">{{ availablePercent }}%</strong>
                  <span>可用占比</span>
                </div>
              </div>

              <div class="asset-stats">
                <div class="asset-row">
                  <div class="asset-row__label">
                    <span class="legend-dot is-dark" />
                    <span>账户总资产</span>
                  </div>
                  <strong class="asset-row__val xyf-tabular">{{ formatPrice(wallet.totalAmount) }}</strong>
                </div>

                <div class="asset-row">
                  <div class="asset-row__label">
                    <span class="legend-dot is-gold" />
                    <span>担保冻结金额</span>
                  </div>
                  <strong class="asset-row__val xyf-tabular">{{ formatPrice(wallet.frozenAmount) }}</strong>
                </div>

                <p class="asset-hint">
                  订单付款后暂存于担保托管池，确认收货验收后即时结算。
                </p>
              </div>
            </div>

            <div class="asset-panel__actions">
              <button type="button" class="wallet-btn-primary" @click="openRechargeDialog">
                充值余额
              </button>
              <button type="button" class="wallet-btn-secondary" @click="openWithdrawDialog">
                申请提现
              </button>
            </div>
          </div>
        </section>

        <!-- Architectural Creator Deposit Shield Vault Card -->
        <section class="deposit-vault-card">
          <div class="deposit-vault-card__left">
            <div class="vault-emblem" aria-hidden="true">
              <svg viewBox="0 0 40 40" fill="none">
                <path d="M20 5L32 9.8V18.8C32 26.2 26.9 32.3 20 34.8C13.1 32.3 8 26.2 8 18.8V9.8L20 5Z" stroke="#18181B" stroke-width="1.8" stroke-linejoin="round" />
                <path d="M15.2 19.8L18.6 23.2L25.2 16.5" stroke="#B89058" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>

            <div class="vault-copy">
              <div class="vault-title-row">
                <h3>创作者履约保证金专户</h3>
                <span class="vault-status-pill" :class="{ 'is-active': sellerDepositBalance > 0 }">
                  {{ sellerDepositBalance > 0 ? '✦ 金盾信誉护航中' : '可选开通' }}
                </span>
              </div>
              <div class="vault-perks">
                <span>个人工作室点亮金盾标识</span>
                <span>·</span>
                <span>悬赏竞标高信誉加权</span>
                <span>·</span>
                <span>无在途销售单随时无息秒退</span>
              </div>
            </div>
          </div>

          <div class="deposit-vault-card__right xyf-tabular">
            <div class="vault-balance">
              <span>在池保证金</span>
              <strong>¥{{ Number(sellerDepositBalance).toFixed(2) }}</strong>
            </div>

            <div class="vault-btns">
              <el-button type="primary" @click="openDepositDialog('pay')">充入保证金</el-button>
              <el-button v-if="sellerDepositBalance > 0" plain @click="openDepositDialog('withdraw')">
                退回余额
              </el-button>
            </div>
          </div>
        </section>

        <!-- Clean Recent Transactions Ledger -->
        <section class="ledger-card">
          <div class="ledger-head">
            <h2>近期收支明细</h2>
            <button type="button" class="ledger-more" @click="router.push('/wallet/transactions')">
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <XyfEmptyState
            v-if="recentTransactions.length === 0"
            title="暂无钱包交易记录"
            description="充值或完成订单交易后，每一笔账务变动都会记录于此。"
          />

          <div v-else class="tx-list">
            <div v-for="row in recentTransactions" :key="row.transactionId" class="tx-row">
              <div class="tx-row__left">
                <XyfStatusBadge
                  :tone="transactionTone(row.transactionType)"
                  :label="getTransactionTypeText(row.transactionType)"
                />
                <div class="tx-row__desc">
                  <div class="tx-title">{{ row.remark || row.typeName || '-' }}</div>
                  <div class="tx-time xyf-tabular">{{ formatDateTimeMinute(row.createTime) }}</div>
                </div>
              </div>

              <div class="tx-row__right xyf-tabular">
                <span
                  class="tx-amount"
                  :class="isPositiveTransaction(row.transactionType) ? 'is-plus' : 'is-minus'"
                >
                  {{ isPositiveTransaction(row.transactionType) ? '+' : '-' }}{{ formatPrice(Math.abs(row.amount)) }}
                </span>
                <span class="tx-balance">余额 {{ formatPrice(row.balanceAfter) }}</span>
              </div>
            </div>
          </div>
        </section>
      </template>
    </div>

    <!-- Recharge Dialog -->
    <el-dialog v-model="rechargeVisible" title="钱包快捷充值" width="420px">
      <el-form label-position="top">
        <el-form-item label="选择或输入充值金额（元）">
          <div class="preset-chips">
            <button
              v-for="amt in [50, 100, 200, 500]"
              :key="amt"
              type="button"
              class="xyf-chip"
              :class="{ 'is-active': rechargeAmount === amt }"
              @click="rechargeAmount = amt"
            >
              +{{ amt }} 元
            </button>
          </div>
          <el-input-number
            v-model="rechargeAmount"
            :min="1"
            :max="5000"
            :precision="2"
            style="width: 100%; margin-top: 12px"
          />
          <p class="wallet-risk-note">
            校园体验金模拟网关 · 单笔上限 ¥5,000，单日累计充值上限 ¥20,000
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRecharge">
          确认充值
        </el-button>
      </template>
    </el-dialog>

    <!-- Withdraw Dialog -->
    <el-dialog v-model="withdrawVisible" title="申请余额提现" width="420px">
      <el-form label-position="top">
        <div class="withdraw-avail">
          当前可提现金额：<strong class="xyf-tabular">{{ formatPrice(wallet?.walletBalance || 0) }}</strong>
          <el-button
            text
            type="primary"
            size="small"
            @click="withdrawAmount = wallet?.walletBalance || 0"
          >
            全部提现
          </el-button>
        </div>

        <el-form-item label="提现金额（元）">
          <el-input-number
            v-model="withdrawAmount"
            :min="0.01"
            :max="Math.max(0.01, wallet?.walletBalance || 0)"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleWithdraw">
          确认提现
        </el-button>
      </template>
    </el-dialog>

    <!-- Dedicated Deposit Vault Dialog -->
    <el-dialog
      v-model="depositDialogVisible"
      :title="depositMode === 'pay' ? '充入创作者履约保证金' : '无责退还保证金至钱包余额'"
      width="440px"
    >
      <div class="withdraw-avail xyf-tabular">
        <template v-if="depositMode === 'pay'">
          当前钱包可用余额：<strong>{{ formatPrice(wallet?.walletBalance || 0) }}</strong>
        </template>
        <template v-else>
          当前在池可退保证金：<strong>¥{{ Number(sellerDepositBalance).toFixed(2) }}</strong>
          <el-button text type="primary" size="small" @click="depositAmount = sellerDepositBalance">
            全额退回
          </el-button>
        </template>
      </div>

      <el-form label-position="top">
        <el-form-item :label="depositMode === 'pay' ? '选择或输入充入金额（最低 ¥10）' : '输入退还金额（最低 ¥10）'">
          <div v-if="depositMode === 'pay'" class="preset-chips" style="margin-bottom: 12px">
            <button
              v-for="amt in [50, 100, 200, 500]"
              :key="amt"
              type="button"
              class="xyf-chip"
              :class="{ 'is-active': depositAmount === amt }"
              @click="depositAmount = amt"
            >
              ¥{{ amt }}
            </button>
          </div>
          <el-input-number
            v-model="depositAmount"
            :min="10"
            :max="depositMode === 'pay' ? Math.max(10, wallet?.walletBalance || 5000) : Math.max(10, sellerDepositBalance)"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <p class="wallet-risk-note">
            {{
              depositMode === 'pay'
                ? '款项将从可用余额划入您的独立保证金专户，立即在创作者档案点亮金盾信誉标识。'
                : '当名下无未完结销售订单及未处理仲裁时，保证金即时原路无息退回钱包可用余额。'
            }}
          </p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="depositDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDepositAction">
          {{ depositMode === 'pay' ? '确认划入保证金' : '确认退回可用余额' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getSellerDepositInfo, paySellerDeposit, withdrawSellerDeposit } from '@/api/milestone'
import {
  getWalletInfo,
  getTransactionHistory,
  rechargeWallet,
  withdrawWallet
} from '@/api/wallet'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTimeMinute } from '@/utils/format'
import {
  TransactionType,
  getTransactionTypeText,
  type WalletInfo,
  type WalletTransaction
} from '@/types/wallet'
import XyfBrandLogo from '@/components/ui/XyfBrandLogo.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const wallet = ref<WalletInfo | null>(null)
const sellerDepositBalance = ref(0)

async function refreshDepositBalance() {
  try {
    const info = await getSellerDepositInfo()
    sellerDepositBalance.value = Number(info.sellerDeposit || 0)
  } catch {
    sellerDepositBalance.value = 0
  }
}

const depositDialogVisible = ref(false)
const depositMode = ref<'pay' | 'withdraw'>('pay')
const depositAmount = ref(100)

function openDepositDialog(mode: 'pay' | 'withdraw') {
  depositMode.value = mode
  depositAmount.value = mode === 'pay' ? 100 : Math.max(10, sellerDepositBalance.value)
  depositDialogVisible.value = true
}

async function submitDepositAction() {
  if (!depositAmount.value || depositAmount.value < 10) {
    ElMessage.warning('保证金操作金额最低为 10 元')
    return
  }
  submitting.value = true
  try {
    if (depositMode.value === 'pay') {
      await paySellerDeposit(Number(depositAmount.value))
      ElMessage.success('履约保证金划入成功，专属金盾信誉标识已点亮')
    } else {
      await withdrawSellerDeposit(Number(depositAmount.value))
      ElMessage.success('履约保证金已无责退回您的钱包可用余额')
    }
    depositDialogVisible.value = false
    await Promise.all([fetchWalletData(), refreshDepositBalance()])
  } catch (err) {
    notifyFallbackError(err, depositMode.value === 'pay' ? '缴纳保证金失败' : '退还保证金失败')
  } finally {
    submitting.value = false
  }
}
const recentTransactions = ref<WalletTransaction[]>([])

const rechargeVisible = ref(false)
const rechargeAmount = ref(100)
const withdrawVisible = ref(false)
const withdrawAmount = ref(0)

// 3D Tilt & Specular Sheen State
const tilt = ref({ rx: 0, ry: 0, sx: 50, sy: 50, active: false })

function onCardMove(e: MouseEvent) {
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const px = (e.clientX - rect.left) / rect.width
  const py = (e.clientY - rect.top) / rect.height
  tilt.value = {
    rx: (0.5 - py) * 10,
    ry: (px - 0.5) * 12,
    sx: px * 100,
    sy: py * 100,
    active: true
  }
}

function onCardLeave() {
  tilt.value = { rx: 0, ry: 0, sx: 50, sy: 50, active: false }
}

const cardTransformStyle = computed(() => ({
  transform: `perspective(1000px) rotateX(${tilt.value.rx.toFixed(2)}deg) rotateY(${tilt.value.ry.toFixed(2)}deg)`
}))

const sheenStyle = computed(() => ({
  opacity: tilt.value.active ? 1 : 0,
  background: `radial-gradient(circle at ${tilt.value.sx}% ${tilt.value.sy}%, rgba(255, 255, 255, 0.16), rgba(229, 199, 158, 0.06) 36%, transparent 65%)`
}))

const holderName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.realName || '校园用户'
)
const holderStudentId = computed(() => userStore.userInfo?.studentId || '校内实名学籍')

// Animated SVG Ratio Ring
const ringCircumference = 2 * Math.PI * 44
const totalAssets = computed(() => Math.max(0, Number(wallet.value?.totalAmount || 0)))
const availableRatio = computed(() => {
  if (totalAssets.value <= 0) return 1
  return Math.min(1, Math.max(0, Number(wallet.value?.walletBalance || 0) / totalAssets.value))
})
const frozenRatio = computed(() => {
  if (totalAssets.value <= 0) return 0
  return Math.min(1, Math.max(0, Number(wallet.value?.frozenAmount || 0) / totalAssets.value))
})
const availablePercent = computed(() => Math.round(availableRatio.value * 100))
const availableDashOffset = computed(() => ringCircumference * (1 - availableRatio.value))
const frozenDashOffset = computed(() => ringCircumference * (1 - Math.max(0.03, frozenRatio.value)))
const frozenStartDeg = computed(() => -90 + availableRatio.value * 360)

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

function openRechargeDialog() {
  rechargeAmount.value = 100
  rechargeVisible.value = true
}

function openWithdrawDialog() {
  const avail = wallet.value?.walletBalance || 0
  if (avail <= 0) {
    ElMessage.info('当前无可提现可用余额')
    return
  }
  withdrawAmount.value = Math.min(avail, 100)
  withdrawVisible.value = true
}

async function fetchWalletData() {
  loading.value = true
  try {
    const [info, txRes] = await Promise.all([
      getWalletInfo(),
      getTransactionHistory({ pageNum: 1, pageSize: 6 })
    ])
    wallet.value = info
    recentTransactions.value = txRes.records || []
  } catch (err) {
    notifyFallbackError(err, '加载钱包信息失败')
  } finally {
    loading.value = false
  }
}

async function handleRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入有效充值金额')
    return
  }
  submitting.value = true
  try {
    await rechargeWallet({ amount: Number(rechargeAmount.value) })
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    await fetchWalletData()
  } catch (err) {
    notifyFallbackError(err, '充值失败')
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!withdrawAmount.value || withdrawAmount.value <= 0) {
    ElMessage.warning('请输入有效提现金额')
    return
  }
  if (wallet.value && withdrawAmount.value > wallet.value.walletBalance) {
    ElMessage.warning('提现金额不能超过可用余额')
    return
  }
  submitting.value = true
  try {
    await withdrawWallet({ amount: Number(withdrawAmount.value) })
    ElMessage.success('提现申请已受理')
    withdrawVisible.value = false
    await fetchWalletData()
  } catch (err) {
    notifyFallbackError(err, '提现失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchWalletData()
  refreshDepositBalance()
})
</script>

<style scoped lang="scss">
.wallet-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;

  h1 {
    margin: 0;
    font-size: 28px;
    font-weight: 750;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
  }

  p {
    margin: 6px 0 0;
    font-size: 14px;
    color: var(--xyf-ink-2);
  }
}

.all-ledger-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    color: var(--xyf-ink);
  }
}

.loading-card {
  padding: 28px;
}

.wallet-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 24px;
  margin-bottom: 28px;
  align-items: stretch;
}

/* 3D Brushed Obsidian Titanium Virtual Card */
.campus-card-wrap {
  perspective: 1100px;
}

.campus-card {
  position: relative;
  height: 100%;
  min-height: 264px;
  border-radius: 22px;
  padding: 28px 30px;
  background:
    radial-gradient(circle at 86% 14%, rgba(229, 199, 158, 0.14), transparent 46%),
    linear-gradient(142deg, #16161a 0%, #0c0c0f 58%, #18181d 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 28px 56px -20px rgba(9, 9, 11, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.14);
  color: #ffffff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
  transform-style: preserve-3d;

  &__sheen {
    position: absolute;
    inset: 0;
    pointer-events: none;
    transition: opacity 0.25s ease;
    z-index: 1;
  }

  &__waves {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
  }

  .wave-drift-1 {
    animation: card-wave-float 9s ease-in-out infinite;
  }

  .wave-drift-2 {
    animation: card-wave-float 11s ease-in-out infinite reverse;
  }

  .card-ring-spin {
    transform-origin: 486px 56px;
    animation: card-spin 36s linear infinite;
  }

  &__top,
  &__center,
  &__bottom {
    position: relative;
    z-index: 2;
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .card-brand {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .card-edition {
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.14em;
    color: rgba(255, 255, 255, 0.48);
    padding-left: 12px;
    border-left: 1px solid rgba(255, 255, 255, 0.14);
  }

  .contactless-svg {
    width: 26px;
    height: 26px;

    .nfc-wave-1 {
      animation: nfc-pulse 2.4s ease-in-out infinite;
    }

    .nfc-wave-2 {
      animation: nfc-pulse 2.4s ease-in-out infinite 0.3s;
    }

    .nfc-wave-3 {
      animation: nfc-pulse 2.4s ease-in-out infinite 0.6s;
    }
  }

  &__center {
    margin: 22px 0;

    .balance-caption {
      display: block;
      font-size: 12.5px;
      color: rgba(255, 255, 255, 0.56);
      margin-bottom: 6px;
    }

    .balance-figure {
      font-size: clamp(32px, 3.6vw, 42px);
      font-weight: 750;
      letter-spacing: -0.03em;
      line-height: 1.08;
      color: #ffffff;
    }
  }

  &__bottom {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 12px;
  }

  .holder-group {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .emv-chip {
    width: 38px;
    height: 28px;
    flex-shrink: 0;
  }

  .holder-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .holder-name {
      font-size: 13.5px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.92);
    }

    .holder-id {
      font-size: 11.5px;
      color: rgba(255, 255, 255, 0.48);
      letter-spacing: 0.08em;
    }
  }

  .escrow-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 11.5px;
    color: rgba(255, 255, 255, 0.72);
    background: rgba(255, 255, 255, 0.07);
    border: 1px solid rgba(255, 255, 255, 0.12);
    padding: 5px 11px;
    border-radius: 999px;

    .escrow-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #e5c79e;
      box-shadow: 0 0 8px rgba(229, 199, 158, 0.85);
    }
  }
}

/* Right Asset Vault Panel */
.asset-panel {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 22px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;

  &__top {
    display: flex;
    align-items: center;
    gap: 26px;
  }

  .ring-box {
    position: relative;
    width: 112px;
    height: 112px;
    flex-shrink: 0;
  }

  .ratio-ring {
    width: 100%;
    height: 100%;

    .ring-arc {
      transition: stroke-dashoffset 0.8s cubic-bezier(0.22, 1, 0.36, 1);
    }
  }

  .ring-center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    strong {
      font-size: 18px;
      font-weight: 750;
      color: var(--xyf-ink);
      line-height: 1.1;
    }

    span {
      font-size: 11px;
      color: var(--xyf-ink-3);
      margin-top: 2px;
    }
  }

  .asset-stats {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .asset-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;

    &__label {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-size: 13.5px;
      color: var(--xyf-ink-2);
    }

    &__val {
      font-size: 18px;
      font-weight: 700;
      color: var(--xyf-ink);
    }
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.is-dark {
      background: #18181b;
    }

    &.is-gold {
      background: #d97706;
    }
  }

  .asset-hint {
    margin: 2px 0 0;
    font-size: 12.5px;
    line-height: 1.55;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
}

.wallet-btn-primary,
.wallet-btn-secondary {
  height: 44px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);
}

.wallet-btn-primary {
  border: none;
  background: #18181b;
  color: #ffffff;

  &:hover {
    background: #2e2e34;
    transform: translateY(-1px);
  }
}

.wallet-btn-secondary {
  border: 1px solid var(--xyf-border-strong);
  background: #ffffff;
  color: var(--xyf-ink);

  &:hover {
    border-color: var(--xyf-ink);
    background: var(--xyf-surface-soft);
  }
}


/* Creator Deposit Shield Vault Card */
.deposit-vault-card {
  background: linear-gradient(180deg, #fffdf9 0%, #ffffff 70%);
  border: 1px solid var(--xyf-champagne-border);
  border-radius: 20px;
  padding: 22px 26px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;

  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;
    min-width: 260px;
  }

  .vault-emblem {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    background: var(--xyf-champagne-soft);
    border: 1px solid var(--xyf-champagne-border);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    svg {
      width: 28px;
      height: 28px;
    }
  }

  .vault-title-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    h3 {
      margin: 0;
      font-size: 16.5px;
      font-weight: 750;
      color: var(--xyf-ink);
    }
  }

  .vault-status-pill {
    padding: 2px 9px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 650;
    background: var(--xyf-surface-muted);
    color: var(--xyf-ink-3);

    &.is-active {
      background: #18181b;
      color: #e5c79e;
    }
  }

  .vault-perks {
    margin-top: 5px;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-wrap: wrap;

    .vault-balance {
      text-align: right;

      span {
        display: block;
        font-size: 11.5px;
        color: var(--xyf-ink-3);
      }

      strong {
        font-size: 22px;
        font-weight: 800;
        color: var(--xyf-ink);
      }
    }

    .vault-btns {
      display: flex;
      gap: 8px;
    }
  }
}

/* Recent Ledger */
.ledger-card {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 20px;
  padding: 24px 28px;
}

.ledger-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.ledger-more {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--xyf-ink-2);
  font-family: inherit;
  cursor: pointer;

  &:hover {
    color: var(--xyf-ink);
  }
}

.tx-list {
  display: flex;
  flex-direction: column;
}

.tx-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 6px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  transition: background 0.18s ease;

  &:last-child {
    border-bottom: none;
    padding-bottom: 4px;
  }

  &:hover {
    background: var(--xyf-surface-soft);
    border-radius: 12px;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }

  &__desc {
    min-width: 0;

    .tx-title {
      font-size: 14px;
      font-weight: 500;
      color: var(--xyf-ink);
    }

    .tx-time {
      margin-top: 3px;
      font-size: 12px;
      color: var(--xyf-ink-3);
    }
  }

  &__right {
    text-align: right;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .tx-amount {
    font-size: 15.5px;
    font-weight: 700;

    &.is-plus {
      color: var(--xyf-ink);
    }

    &.is-minus {
      color: var(--xyf-ink-2);
    }
  }

  .tx-balance {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.preset-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.withdraw-avail {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: var(--xyf-ink-2);
  margin-bottom: 14px;

  strong {
    color: var(--xyf-ink);
    font-size: 15px;
  }
}

.wallet-risk-note {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--xyf-ink-3);
}

@keyframes card-wave-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@keyframes card-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes nfc-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .wallet-stage {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .asset-panel__top {
    flex-direction: column;
    align-items: flex-start;
  }

  .ledger-card {
    padding: 18px 16px;
  }
}
</style>
