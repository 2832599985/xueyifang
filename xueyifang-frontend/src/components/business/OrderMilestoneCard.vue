<template>
  <section v-if="shouldShowCard" class="xyf-surface-card milestone-card">
    <div class="m-head">
      <div>
        <span class="xyf-kicker">STAGED ESCROW MILESTONES</span>
        <h2>分阶段里程碑担保交付</h2>
      </div>

      <el-button
        v-if="canConfigurePlan"
        size="small"
        plain
        @click="openPlanDialog"
      >
        {{ milestones.length > 0 ? '调整阶段比例' : '+ 拆解分阶段验收' }}
      </el-button>
    </div>

    <!-- Empty / Single-Lump Escrow State -->
    <div v-if="milestones.length === 0" class="lump-escrow-box xyf-tabular">
      <div class="lump-escrow-box__left">
        <span class="lump-dot" />
        <div>
          <strong>当前采用整单一次性验收结算</strong>
          <p>若开发、设计或辅导周期较长，买卖双方可将剩余冻结款拆分为 2–5 个节点逐阶段验收放款。</p>
        </div>
      </div>
      <div class="lump-escrow-box__amt">
        <span>在池冻结</span>
        <strong>¥{{ Number(order.frozenAmount || 0).toFixed(2) }}</strong>
      </div>
    </div>

    <!-- Active Staged Escrow Funding Bar + Connected Stepper -->
    <template v-else>
      <div class="funding-bar-box xyf-tabular">
        <div class="funding-bar-meta">
          <span>已释放 <strong>¥{{ releasedTotal.toFixed(2) }}</strong></span>
          <span v-if="pendingReviewTotal > 0" class="is-amber">
            待买方验收 <strong>¥{{ pendingReviewTotal.toFixed(2) }}</strong>
          </span>
          <span>剩余冻结 <strong>¥{{ Number(order.frozenAmount || 0).toFixed(2) }}</strong></span>
        </div>
        <div class="funding-track">
          <div class="funding-seg is-released" :style="{ width: `${releasedPct}%` }" />
          <div class="funding-seg is-review" :style="{ width: `${reviewPct}%` }" />
        </div>
      </div>

      <div class="stage-timeline">
        <div
          v-for="stage in milestones"
          :key="stage.id"
          class="stage-node"
          :class="`is-st-${stage.milestoneStatus}`"
        >
          <div class="stage-node__rail">
            <span class="node-circle xyf-tabular">
              {{ stage.milestoneStatus === 2 ? '✓' : `0${stage.stageIndex}` }}
            </span>
            <span class="node-line" />
          </div>

          <div class="stage-node__card">
            <div class="stage-top">
              <div class="stage-title-group">
                <strong>{{ stage.title }}</strong>
                <span class="pct-pill xyf-tabular">{{ calcStagePct(stage.amount) }}%</span>
                <el-tag
                  size="small"
                  :type="stage.milestoneStatus === 2 ? 'success' : stage.milestoneStatus === 1 ? 'warning' : 'info'"
                >
                  {{
                    stage.milestoneStatus === 2
                      ? '已验收释放'
                      : stage.milestoneStatus === 1
                        ? '已交成果待验收'
                        : '待卖方交付'
                  }}
                </el-tag>
              </div>

              <strong class="stage-amount xyf-tabular">¥{{ Number(stage.amount).toFixed(2) }}</strong>
            </div>

            <div v-if="stage.proofNote" class="proof-box">
              <span class="proof-lbl">阶段交付凭证</span>
              <p>{{ stage.proofNote }}</p>
            </div>

            <div
              v-if="stage.milestoneStatus !== 2 && canOperate && (isSeller || isBuyer)"
              class="stage-actions"
            >
              <el-button
                v-if="isSeller"
                size="small"
                plain
                @click="openProofDialog(stage)"
              >
                {{ stage.milestoneStatus === 1 ? '补充/更新成果说明' : '提交阶段交付说明' }}
              </el-button>

              <el-button
                v-if="isBuyer"
                size="small"
                type="primary"
                :loading="releasingId === stage.id"
                @click="handleRelease(stage)"
              >
                验收通过并释放 ¥{{ Number(stage.amount).toFixed(2) }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Smart Milestone Planner Dialog -->
    <el-dialog v-model="planVisible" title="配置分阶段里程碑托管计划" width="580px">
      <div class="preset-bar">
        <span class="preset-bar__lbl">快捷比例模板：</span>
        <button type="button" class="preset-chip" @click="applySplitPreset([0.4, 0.6], ['阶段1：框架与初稿确认', '阶段2：终稿交付与答疑'])">
          四六开 (40% / 60%)
        </button>
        <button type="button" class="preset-chip" @click="applySplitPreset([0.3, 0.4, 0.3], ['阶段1：需求与原型确认', '阶段2：核心功能联调', '阶段3：终验交付'])">
          三阶段 (30% / 40% / 30%)
        </button>
        <button type="button" class="preset-chip" @click="applySplitPreset([0.5, 0.5], ['阶段1：中期成果验收', '阶段2：完整终稿验收'])">
          对半均分 (50% / 50%)
        </button>
      </div>

      <div class="allocation-meter xyf-tabular" :class="{ 'is-balanced': isPlanBalanced }">
        <span>目标冻结总额：<strong>¥{{ Number(order.frozenAmount || 0).toFixed(2) }}</strong></span>
        <span>
          当前已分配：<strong>¥{{ draftSum.toFixed(2) }}</strong>
          <small v-if="isPlanBalanced">（✓ 金额完全匹配）</small>
          <small v-else>（差额 ¥{{ (Number(order.frozenAmount || 0) - draftSum).toFixed(2) }}）</small>
        </span>
      </div>

      <div class="draft-list">
        <div v-for="(st, i) in draftStages" :key="i" class="draft-row">
          <span class="draft-idx xyf-mono">0{{ i + 1 }}</span>
          <el-input v-model="st.title" placeholder="阶段验收节点名称" style="flex: 1" />
          <el-input-number v-model="st.amount" :min="1" :precision="2" :step="5" style="width: 145px" />
          <el-button
            v-if="draftStages.length > 1"
            type="danger"
            link
            @click="draftStages.splice(i, 1)"
          >
            删除
          </el-button>
        </div>
      </div>

      <el-button
        v-if="draftStages.length < 5"
        plain
        size="small"
        style="margin-top: 10px"
        @click="addDraftStage"
      >
        + 新增验收阶段（上限 5 阶段）
      </el-button>

      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!isPlanBalanced"
          :loading="savingPlan"
          @click="submitPlan"
        >
          保存并生效里程碑计划
        </el-button>
      </template>
    </el-dialog>

    <!-- Dedicated Proof Submission Dialog (Replaces ugly ElMessageBox.prompt) -->
    <el-dialog
      v-model="proofDialogVisible"
      :title="activeProofStage ? `提交阶段 0${activeProofStage.stageIndex} 交付成果` : '提交阶段成果'"
      width="500px"
    >
      <el-form label-position="top">
        <el-form-item :label="`验收节点：${activeProofStage?.title || ''}（¥${Number(activeProofStage?.amount || 0).toFixed(2)}）`" required>
          <el-input
            v-model="proofDraft"
            type="textarea"
            :rows="4"
            maxlength="400"
            show-word-limit
            placeholder="请详细说明本阶段已完成的交付内容、网盘/代码仓库或私信发送凭证，方便买方核对放款..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="proofDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingProof" @click="confirmSubmitProof">
          提交成果并通知买方验收
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listOrderMilestones,
  saveOrderMilestonePlan,
  submitOrderMilestoneProof,
  releaseOrderMilestone,
  type OrderMilestoneItem
} from '@/api/milestone'
import { notifyFallbackError } from '@/api/request'
import { OrderStatus, type OrderDetail } from '@/types/order'

const props = defineProps<{
  order: OrderDetail
  isBuyer: boolean
  isSeller: boolean
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const milestones = ref<OrderMilestoneItem[]>([])
const planVisible = ref(false)
const savingPlan = ref(false)
const releasingId = ref<number | null>(null)
const draftStages = ref<Array<{ title: string; amount: number }>>([])

const proofDialogVisible = ref(false)
const submittingProof = ref(false)
const activeProofStage = ref<OrderMilestoneItem | null>(null)
const proofDraft = ref('')

const canOperate = computed(
  () =>
    props.order.orderStatus === OrderStatus.PENDING_SHIP ||
    props.order.orderStatus === OrderStatus.PENDING_RECEIPT
)

const shouldShowCard = computed(
  () =>
    canOperate.value ||
    (props.order.orderStatus === OrderStatus.COMPLETED && milestones.value.length > 0)
)

const canConfigurePlan = computed(
  () =>
    canOperate.value &&
    Number(props.order.frozenAmount || 0) >= 2 &&
    !milestones.value.some(m => m.milestoneStatus === 2)
)

const draftSum = computed(() =>
  draftStages.value.reduce((acc, s) => acc + Number(s.amount || 0), 0)
)

const isPlanBalanced = computed(
  () => Math.abs(draftSum.value - Number(props.order.frozenAmount || 0)) <= 0.005
)

const totalMilestoneSum = computed(() =>
  Math.max(0.01, milestones.value.reduce((acc, m) => acc + Number(m.amount || 0), 0))
)

const releasedTotal = computed(() =>
  milestones.value
    .filter(m => m.milestoneStatus === 2)
    .reduce((acc, m) => acc + Number(m.amount || 0), 0)
)

const pendingReviewTotal = computed(() =>
  milestones.value
    .filter(m => m.milestoneStatus === 1)
    .reduce((acc, m) => acc + Number(m.amount || 0), 0)
)

const releasedPct = computed(() =>
  Math.min(100, Math.round((releasedTotal.value / totalMilestoneSum.value) * 100))
)

const reviewPct = computed(() =>
  Math.min(100 - releasedPct.value, Math.round((pendingReviewTotal.value / totalMilestoneSum.value) * 100))
)

function calcStagePct(amt: number) {
  return Math.round((Number(amt || 0) / totalMilestoneSum.value) * 100)
}

async function loadMilestones() {
  if (!props.order?.id) return
  try {
    milestones.value = await listOrderMilestones(props.order.id)
  } catch {
    milestones.value = []
  }
}

function applySplitPreset(ratios: number[], titles: string[]) {
  const target = Number(props.order.frozenAmount || 0)
  let allocated = 0
  draftStages.value = ratios.map((r, idx) => {
    const isLast = idx === ratios.length - 1
    const amt = isLast
      ? Number(Math.max(0.01, target - allocated).toFixed(2))
      : Number(Math.max(0.01, target * r).toFixed(2))
    allocated += amt
    return {
      title: titles[idx] || `第${idx + 1}阶段验收`,
      amount: amt
    }
  })
}

function openPlanDialog() {
  if (milestones.value.length > 0) {
    draftStages.value = milestones.value.map(m => ({
      title: m.title,
      amount: Number(m.amount)
    }))
  } else {
    applySplitPreset(
      [0.4, 0.6],
      ['第一阶段：方案框架与初稿确认', '第二阶段：完整终稿交付与答疑验收']
    )
  }
  planVisible.value = true
}

function addDraftStage() {
  draftStages.value.push({ title: `第${draftStages.value.length + 1}阶段验收`, amount: 10 })
}

async function submitPlan() {
  if (!isPlanBalanced.value) {
    ElMessage.warning('各阶段金额总和必须严格等于当前冻结金额')
    return
  }
  savingPlan.value = true
  try {
    milestones.value = await saveOrderMilestonePlan(props.order.id, draftStages.value)
    ElMessage.success('分阶段里程碑托管计划已生效')
    planVisible.value = false
  } catch (err) {
    notifyFallbackError(err, '保存里程碑计划失败')
  } finally {
    savingPlan.value = false
  }
}

function openProofDialog(stage: OrderMilestoneItem) {
  activeProofStage.value = stage
  proofDraft.value = stage.proofNote || ''
  proofDialogVisible.value = true
}

async function confirmSubmitProof() {
  if (!activeProofStage.value) return
  if (!proofDraft.value.trim()) {
    ElMessage.warning('请填写阶段交付成果说明')
    return
  }
  submittingProof.value = true
  try {
    await submitOrderMilestoneProof(activeProofStage.value.id, proofDraft.value.trim())
    ElMessage.success('阶段交付成果已提交，等待买方验收释放款项')
    proofDialogVisible.value = false
    await loadMilestones()
    emit('updated')
  } catch (err) {
    notifyFallbackError(err, '提交阶段成果失败')
  } finally {
    submittingProof.value = false
  }
}

async function handleRelease(stage: OrderMilestoneItem) {
  try {
    await ElMessageBox.confirm(
      `确认验收通过阶段 0${stage.stageIndex}「${stage.title}」并立即向卖方释放托管款 ¥${Number(stage.amount).toFixed(2)} 吗？释放后不可撤回。`,
      '验收释放阶段托管款',
      { confirmButtonText: '确认验收放款', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  releasingId.value = stage.id
  try {
    await releaseOrderMilestone(stage.id)
    ElMessage.success(`已向卖方释放 ¥${Number(stage.amount).toFixed(2)}`)
    await loadMilestones()
    emit('updated')
  } catch (err) {
    notifyFallbackError(err, '释放阶段款项失败')
  } finally {
    releasingId.value = null
  }
}

watch(() => props.order.id, loadMilestones)
onMounted(loadMilestones)
</script>

<style scoped lang="scss">
.milestone-card {
  padding: 22px 24px;
}

.m-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  h2 {
    margin: 3px 0 0;
    font-size: 16.5px;
    font-weight: 750;
    color: var(--xyf-ink);
  }
}

.lump-escrow-box {
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  &__left {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    flex: 1;
    min-width: 220px;

    .lump-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #18181b;
      margin-top: 6px;
      flex-shrink: 0;
    }

    strong {
      display: block;
      font-size: 13.5px;
      color: var(--xyf-ink);
    }

    p {
      margin: 3px 0 0;
      font-size: 12.5px;
      color: var(--xyf-ink-3);
      line-height: 1.5;
    }
  }

  &__amt {
    text-align: right;

    span {
      display: block;
      font-size: 11px;
      color: var(--xyf-ink-3);
    }

    strong {
      font-size: 18px;
      font-weight: 800;
      color: var(--xyf-ink);
    }
  }
}

.funding-bar-box {
  margin-bottom: 18px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);

  .funding-bar-meta {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
    color: var(--xyf-ink-2);
    margin-bottom: 8px;

    strong {
      color: var(--xyf-ink);
    }

    .is-amber strong {
      color: #9a5b13;
    }
  }

  .funding-track {
    height: 7px;
    border-radius: 999px;
    background: #e4e4e7;
    display: flex;
    overflow: hidden;

    .funding-seg {
      height: 100%;
      transition: width 0.3s ease;

      &.is-released {
        background: #18181b;
      }

      &.is-review {
        background: #c8a97e;
      }
    }
  }
}

.stage-timeline {
  display: flex;
  flex-direction: column;
}

.stage-node {
  display: flex;
  gap: 14px;

  &:last-child .node-line {
    display: none;
  }

  &__rail {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex-shrink: 0;

    .node-circle {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: 1.5px solid var(--xyf-border-strong);
      background: #ffffff;
      color: var(--xyf-ink-2);
      font-size: 11.5px;
      font-weight: 750;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .node-line {
      flex: 1;
      width: 1.5px;
      background: var(--xyf-border);
      min-height: 16px;
    }
  }

  &.is-st-2 .node-circle {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }

  &.is-st-1 .node-circle {
    border-color: #b45309;
    color: #b45309;
    background: var(--xyf-champagne-soft);
  }

  &__card {
    flex: 1;
    margin-bottom: 12px;
    padding: 14px 16px;
    border-radius: 12px;
    border: 1px solid var(--xyf-border);
    background: #ffffff;
  }
}

.stage-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;

  .stage-title-group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    strong {
      font-size: 14px;
      color: var(--xyf-ink);
    }
  }

  .pct-pill {
    padding: 1px 7px;
    border-radius: 999px;
    background: var(--xyf-surface-muted);
    font-size: 11px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }

  .stage-amount {
    font-size: 16px;
    font-weight: 800;
    color: var(--xyf-ink);
  }
}

.proof-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 9px;
  background: var(--xyf-surface-soft);

  .proof-lbl {
    display: block;
    font-size: 11px;
    font-weight: 650;
    color: var(--xyf-ink-3);
    margin-bottom: 2px;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
    line-height: 1.55;
  }
}

.stage-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* Planner Dialog */
.preset-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  &__lbl {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.preset-chip {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface-soft);
  color: var(--xyf-ink);
  font-size: 12px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    border-color: #18181b;
    background: #ffffff;
  }
}

.allocation-meter {
  padding: 10px 14px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #f7cece;
  color: #991b1b;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12.5px;
  margin-bottom: 14px;

  &.is-balanced {
    background: #f2fbf5;
    border-color: #ccebd7;
    color: #15803d;
  }
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.draft-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .draft-idx {
    font-size: 12px;
    font-weight: 700;
    color: var(--xyf-ink-3);
  }
}
</style>
