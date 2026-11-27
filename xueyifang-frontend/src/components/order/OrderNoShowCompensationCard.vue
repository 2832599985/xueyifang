<template>
  <section v-if="shouldShowCard" class="xyf-surface-card noshow-card">
    <div class="noshow-head">
      <div>
        <span class="noshow-kicker">APPOINTMENT NO-SHOW PROTECTION · 预约爽约自动赔付保障</span>
        <h3>{{ claim ? '预约爽约自动退赔存证单' : '预约时段履约与爽约自动赔付保障' }}</h3>
      </div>
      <el-tag v-if="claim" type="danger" effect="dark">已执行爽约退一赔十</el-tag>
      <el-tag v-else-if="isOverdue" type="warning" effect="dark">预约时段已逾期未履约</el-tag>
      <el-tag v-else type="success" effect="plain">爽约保证金护航中</el-tag>
    </div>

    <!-- 已完成赔付存证展示 -->
    <div v-if="claim" class="claim-certificate">
      <div class="cert-grid">
        <div class="cert-item">
          <span class="c-label">赔付存证单号</span>
          <strong class="c-val">{{ claim.claimNo }}</strong>
        </div>
        <div class="cert-item">
          <span class="c-label">全额退回买方本金</span>
          <strong class="c-val is-green">¥{{ Number(claim.refundToBuyerAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="cert-item">
          <span class="c-label">卖方保证金划扣赔付</span>
          <strong class="c-val is-red">+¥{{ Number(claim.compensationAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="cert-item">
          <span class="c-label">卖方履约信用分扣减</span>
          <strong class="c-val">-{{ claim.creditDeducted }} 分</strong>
        </div>
      </div>
      <p class="cert-note">{{ claim.evidenceNote }}</p>
    </div>

    <!-- 待履约或已逾期可一键申领爽约赔付 -->
    <div v-else class="noshow-body">
      <p class="policy-desc">
        本订单受「学艺坊预约爽约自动赔付机制」保护：若卖方超出约定结束时间（{{ order?.scheduledEnd || '约定档期' }}）仍未签到或交付，买方可一键触发系统自动解冻退还 100% 托管本金，并直接从卖方履约保证金专户划扣 10%（最低 ¥5.00）违约金即时赔付至买方钱包。
      </p>
      <div v-if="isBuyer && isOverdue && order?.orderStatus === OrderStatus.PENDING_SHIP" class="claim-action-bar">
        <el-button type="danger" :loading="submitting" @click="handleClaimNoShow">
          卖方逾期未到场 · 一键申领爽约全额退本金 + 保证金赔付
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
.noshow-card {
  padding: 20px 22px;
  border-left: 4px solid #f59e0b;
}

.noshow-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  .noshow-kicker {
    display: block;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #b45309;
  }

  h3 {
    margin: 4px 0 0;
    font-size: 16px;
    font-weight: 700;
  }
}

.claim-certificate {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 14px 16px;
}

.cert-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 10px;

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.cert-item {
  display: flex;
  flex-direction: column;
  gap: 3px;

  .c-label {
    font-size: 11.5px;
    color: #78350f;
  }

  .c-val {
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;

    &.is-green {
      color: #059669;
    }
    &.is-red {
      color: #dc2626;
    }
  }
}

.cert-note {
  margin: 0;
  font-size: 12.5px;
  color: #92400e;
}

.policy-desc {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-2);
  line-height: 1.6;
}

.claim-action-bar {
  margin-top: 14px;
}
</style>
