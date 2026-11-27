<template>
  <section v-if="shouldShowCard" class="xyf-surface-card sla-card">
    <div class="sla-head">
      <div>
        <span class="xyf-kicker">APPOINTMENT SLA &amp; DEPOSIT PROTECTION</span>
        <h3>{{ claim ? '预约爽约自动退赔结算凭证' : '预约时段履约与保证金爽约赔付护航' }}</h3>
      </div>

      <el-tag v-if="claim" type="danger">已执行退本金+违约赔付</el-tag>
      <el-tag v-else-if="isOverdue" type="warning">超出预约结束时间</el-tag>
      <el-tag v-else type="success">SLA 保证金护航中</el-tag>
    </div>

    <!-- Completed Compensation Voucher -->
    <div v-if="claim" class="claim-voucher xyf-tabular">
      <div class="voucher-grid">
        <div class="v-item">
          <span>赔付存证单号</span>
          <strong class="xyf-mono">{{ claim.claimNo }}</strong>
        </div>
        <div class="v-item">
          <span>原路退回买方本金</span>
          <strong>¥{{ Number(claim.refundToBuyerAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="v-item is-accent">
          <span>保证金划扣违约赔付</span>
          <strong>+¥{{ Number(claim.compensationAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="v-item">
          <span>卖方履约信用扣减</span>
          <strong>-{{ claim.creditDeducted }} 分</strong>
        </div>
      </div>
      <p class="voucher-note">{{ claim.evidenceNote }}</p>
    </div>

    <!-- Active Protection Flow -->
    <div v-else class="sla-body">
      <div class="sla-flow">
        <div class="sla-step">
          <span class="sla-num xyf-tabular">01</span>
          <span>锁定档期：{{ order?.scheduledEnd ? String(order.scheduledEnd).replace('T', ' ').slice(0, 16) : '双方约定交付时段' }}</span>
        </div>
        <div class="sla-step">
          <span class="sla-num xyf-tabular">02</span>
          <span>超时未签到/未交付可一键申领</span>
        </div>
        <div class="sla-step">
          <span class="sla-num xyf-tabular">03</span>
          <span>100% 退本金 + 划扣卖方 10% 保证金赔付</span>
        </div>
      </div>

      <div v-if="isBuyer && isOverdue && order?.orderStatus === OrderStatus.PENDING_SHIP" class="sla-action">
        <el-button type="danger" :loading="submitting" @click="handleClaimNoShow">
          卖方逾期未履约 · 一键触发退本金与保证金赔付
        </el-button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OrderStatus } from '@/types/order'
import { notifyFallbackError } from '@/api/request'
import {
  getOrderNoShowClaim,
  buyerClaimSellerNoShow,
  type AppointmentNoshowClaimItem
} from '@/api/finance'

const props = defineProps<{
  order: Record<string, any> | null
  isBuyer: boolean
  isSeller: boolean
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const claim = ref<AppointmentNoshowClaimItem | null>(null)
const submitting = ref(false)

const isOverdue = computed(() => {
  if (!props.order?.scheduledEnd) return false
  const endTs = new Date(props.order.scheduledEnd).getTime()
  return !Number.isNaN(endTs) && Date.now() > endTs
})

const shouldShowCard = computed(() => {
  if (!props.order) return false
  return Boolean(claim.value || props.order.scheduledEnd || props.order.slotId)
})

const fetchClaim = async () => {
  if (!props.order?.id) return
  try {
    claim.value = await getOrderNoShowClaim(Number(props.order.id))
  } catch {
    claim.value = null
  }
}

const handleClaimNoShow = async () => {
  if (!props.order?.id) return
  try {
    await ElMessageBox.confirm(
      '确认卖方超出预约结束时间仍未到场或未交付服务？确认后系统将立即解冻退还您的全部订单本金，并划扣卖方履约保证金赔付至您的钱包。',
      '触发预约爽约自动退赔',
      {
        confirmButtonText: '确认触发自动赔付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    submitting.value = true
    const res = await buyerClaimSellerNoShow(
      Number(props.order.id),
      '买方确认卖方超出预约结束时段仍未履约，申请系统自动退本金并划扣保证金赔付'
    )
    claim.value = res
    ElMessage.success(
      `爽约自动退赔完成！已退回本金 ¥${Number(res.refundToBuyerAmount).toFixed(2)}，并获赔违约金 ¥${Number(res.compensationAmount).toFixed(2)}`
    )
    emit('updated')
  } catch (err: any) {
    if (err !== 'cancel' && err?.message) {
      notifyFallbackError(err, '申领爽约赔付失败')
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.order?.id,
  () => {
    fetchClaim()
  }
)

onMounted(() => {
  fetchClaim()
})
</script>

<style scoped lang="scss">
.sla-card {
  padding: 20px 24px;
}

.sla-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  h3 {
    margin: 3px 0 0;
    font-size: 16px;
    font-weight: 750;
    color: var(--xyf-ink);
  }
}

.claim-voucher {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  .voucher-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 10px;

    @media (max-width: 640px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .v-item {
    display: flex;
    flex-direction: column;
    gap: 3px;

    span {
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }

    strong {
      font-size: 14.5px;
      font-weight: 750;
      color: var(--xyf-ink);
    }

    &.is-accent strong {
      color: #b91c1c;
    }
  }

  .voucher-note {
    margin: 0;
    padding-top: 8px;
    border-top: 1px solid var(--xyf-border-subtle);
    font-size: 12px;
    color: var(--xyf-ink-2);
  }
}

.sla-flow {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;

  @media (max-width: 680px) {
    grid-template-columns: 1fr;
  }

  .sla-step {
    padding: 10px 12px;
    border-radius: 10px;
    background: var(--xyf-surface-soft);
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12.5px;
    color: var(--xyf-ink-2);

    .sla-num {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #18181b;
      color: #ffffff;
      font-size: 10.5px;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
  }
}

.sla-action {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
