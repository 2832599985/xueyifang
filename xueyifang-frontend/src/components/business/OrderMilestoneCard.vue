<template>
  <section v-if="shouldShowCard" class="xyf-surface-card milestone-card">
    <div class="m-head">
      <div>
        <h2>分阶段里程碑托管交付</h2>
        <p class="sub">
          支持将托管总额拆分为 2~5 个验收节点（如初稿确认、终稿交付），买方逐阶段验收并按比例释放担保款至卖方钱包
        </p>
      </div>
      <el-button
        v-if="canConfigurePlan"
        size="small"
        plain
        @click="openPlanDialog"
      >
        {{ milestones.length > 0 ? '调整阶段拆解' : '+ 配置分阶段里程碑' }}
      </el-button>
    </div>

    <div v-if="milestones.length === 0" class="empty-plan">
      当前采用整单一次性验收结算（剩余托管冻结 <strong>¥{{ Number(order.frozenAmount || 0).toFixed(2) }}</strong>）。若服务周期较长，买卖任意一方均可点击右上角拆分为多阶段里程碑交付。
    </div>

    <div v-else class="stage-list">
      <div
        v-for="stage in milestones"
        :key="stage.id"
        class="stage-row"
        :class="`is-st-${stage.milestoneStatus}`"
      >
        <div class="stage-row__main">
          <div class="stage-title-line">
            <span class="stage-idx xyf-mono">#{{ stage.stageIndex }}</span>
            <strong>{{ stage.title }}</strong>
            <el-tag
              size="small"
              :type="stage.milestoneStatus === 2 ? 'success' : stage.milestoneStatus === 1 ? 'warning' : 'info'"
            >
              {{
                stage.milestoneStatus === 2
                  ? '已验收释放'
                  : stage.milestoneStatus === 1
                  ? '卖方已交付待验收'
                  : '待卖方提交成果'
              }}
            </el-tag>
          </div>
          <p v-if="stage.proofNote" class="proof-text">阶段成果说明：{{ stage.proofNote }}</p>
        </div>

        <div class="stage-row__right">
          <strong class="stage-amt xyf-tabular">¥{{ Number(stage.amount).toFixed(2) }}</strong>

          <el-button
            v-if="isSeller && stage.milestoneStatus !== 2 && canOperate"
            size="small"
            plain
            @click="handleSubmitProof(stage)"
          >
            {{ stage.milestoneStatus === 1 ? '更新成果说明' : '提交阶段成果' }}
          </el-button>

          <el-button
            v-if="isBuyer && stage.milestoneStatus !== 2 && canOperate"
            size="small"
            type="primary"
            :loading="releasingId === stage.id"
            @click="handleRelease(stage)"
          >
            验收并释放该笔款项
          </el-button>
        </div>
      </div>
    </div>

    <!-- Configure Stages Dialog -->
    <el-dialog v-model="planVisible" title="配置分阶段里程碑拆解计划" width="540px">
      <div class="plan-tip xyf-tabular">
        各阶段金额总和需严格等于当前剩余托管冻结金额：
        <strong>¥{{ Number(order.frozenAmount || 0).toFixed(2) }}</strong>
        （当前已分配：¥{{ draftSum.toFixed(2) }}）
      </div>

      <div v-for="(st, i) in draftStages" :key="i" class="draft-row">
        <el-input v-model="st.title" placeholder="阶段名称（如：阶段1 架构初稿验收）" style="flex: 1" />
        <el-input-number v-model="st.amount" :min="1" :precision="2" style="width: 140px" />
        <el-button
          v-if="draftStages.length > 1"
          type="danger"
          link
          @click="draftStages.splice(i, 1)"
        >
          移除
        </el-button>
      </div>

      <el-button
        v-if="draftStages.length < 5"
        plain
        size="small"
        style="margin-top: 8px"
        @click="addDraftStage"
      >
        + 增加一阶段（最多 5 阶段）
      </el-button>

      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPlan" @click="submitPlan">保存里程碑计划</el-button>
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

async function loadMilestones() {
  if (!props.order?.id) return
  try {
    milestones.value = await listOrderMilestones(props.order.id)
  } catch {
    milestones.value = []
  }
}

function openPlanDialog() {
  const total = Number(props.order.frozenAmount || 0)
  if (milestones.value.length > 0) {
    draftStages.value = milestones.value.map(m => ({
      title: m.title,
      amount: Number(m.amount)
    }))
  } else {
    const first = Number((total * 0.4).toFixed(2))
    const second = Number((total - first).toFixed(2))
    draftStages.value = [
      { title: '第一阶段：方案框架与初稿确认', amount: Math.max(1, first) },
      { title: '第二阶段：完整终稿交付与答疑验收', amount: Math.max(1, second) }
    ]
  }
  planVisible.value = true
}

function addDraftStage() {
  draftStages.value.push({ title: `第${draftStages.value.length + 1}阶段验收`, amount: 10 })
}

async function submitPlan() {
  const target = Number(props.order.frozenAmount || 0)
  if (Math.abs(draftSum.value - target) > 0.001) {
    ElMessage.warning(`各阶段总和（¥${draftSum.value.toFixed(2)}）必须等于冻结金额（¥${target.toFixed(2)}）`)
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

async function handleSubmitProof(stage: OrderMilestoneItem) {
  try {
    const res = await ElMessageBox.prompt(
      `请填写阶段 #${stage.stageIndex}「${stage.title}」的交付成果说明：`,
      '提交阶段交付成果',
      {
        confirmButtonText: '提交成果',
        cancelButtonText: '取消',
        inputValue: stage.proofNote || '',
        inputPlaceholder: '例如：已完成数据库设计与核心模块代码初稿，已发送至站内私信请查收'
      }
    )
    if (!res.value?.trim()) {
      ElMessage.warning('成果说明不能为空')
      return
    }
    await submitOrderMilestoneProof(stage.id, res.value.trim())
    ElMessage.success('阶段成果已提交，等待买家验收释放款项')
    await loadMilestones()
    emit('updated')
  } catch {
    // cancelled
  }
}

async function handleRelease(stage: OrderMilestoneItem) {
  try {
    await ElMessageBox.confirm(
      `确认验收通过阶段 #${stage.stageIndex}「${stage.title}」并立即向卖家释放托管款 ¥${Number(stage.amount).toFixed(2)} 吗？释放后不可撤回。`,
      '验收释放阶段托管款',
      { confirmButtonText: '确认验收释放', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  releasingId.value = stage.id
  try {
    await releaseOrderMilestone(stage.id)
    ElMessage.success(`阶段 #${stage.stageIndex} 款项已释放结算至卖家钱包`)
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
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.m-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;

  h2 {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 700;
  }

  .sub {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
    max-width: 560px;
  }
}

.empty-plan {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 13px;
  color: var(--xyf-ink-2);
}

.stage-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stage-row {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;

  &.is-st-2 {
    background: #f0fdf4;
    border-color: #a7f3d0;
  }

  &.is-st-1 {
    background: #fffbeb;
    border-color: #fde68a;
  }

  .stage-title-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .stage-idx {
    font-size: 12px;
    font-weight: 700;
    color: var(--xyf-ink-muted);
  }

  .proof-text {
    margin: 6px 0 0;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;

    .stage-amt {
      font-size: 16px;
      font-weight: 800;
    }
  }
}

.plan-tip {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f4f5;
  font-size: 12.5px;
  margin-bottom: 12px;
}

.draft-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
</style>
