<template>
  <section class="xyf-surface-card meetup-proposal-card">
    <div class="head-row">
      <div>
        <h2>线下会面位置双向确认</h2>
        <p class="sub-text">
          买卖双方均可在真实校园地图上标记会面点与有效核销半径，对方同意后正式锁定并执行当面围栏半径强校验
        </p>
      </div>
      <XyfStatusBadge
        :tone="order.meetupConfirmed === 1 ? 'success' : 'warning'"
        :label="order.meetupConfirmed === 1 ? '双方已确认锁定' : '待双方协商锁定'"
      />
    </div>

    <!-- Current Agreed Coordinates -->
    <div class="current-spot-box">
      <div class="spot-main">
        <span class="spot-kicker">当前约定会面点</span>
        <strong>{{ order.meetupPoiName || order.tradeLocationName || '暂未指定校内坐标' }}</strong>
        <p v-if="order.meetupAddress">{{ order.meetupAddress }}</p>
        <div class="spot-meta xyf-mono" v-if="order.meetupLat && order.meetupLng">
          <span>坐标：{{ Number(order.meetupLat).toFixed(6) }}, {{ Number(order.meetupLng).toFixed(6) }}</span>
          <span>· 有效核销范围：{{ effectiveRadiusMeters }} 米内</span>
          <span v-if="order.scheduledStart">· 约定时间：{{ formatDateTime(order.scheduledStart) }}</span>
        </div>
      </div>

      <el-button
        v-if="canNegotiate"
        plain
        size="small"
        @click="openProposeDialog"
      >
        发起新位置/改期提案
      </el-button>
    </div>

    <!-- Active Pending Proposal Banner -->
    <div v-if="pendingProposal" class="pending-proposal-alert">
      <div class="pending-info">
        <span class="badge-pill">待确认新提案</span>
        <strong>
          {{ pendingProposal.proposerRole === 1 ? '买家' : '卖家' }}（{{ pendingProposal.proposerName }}）提议改至「{{ pendingProposal.poiName }}」
          <span v-if="pendingProposal.radiusMeters">（{{ pendingProposal.radiusMeters }} 米内有效）</span>
        </strong>
        <p v-if="pendingProposal.detailAddress">具体方位：{{ pendingProposal.detailAddress }}</p>
        <p v-if="pendingProposal.remark">协商留言：{{ pendingProposal.remark }}</p>
      </div>

      <div v-if="pendingProposal.proposerId !== currentUserId" class="pending-actions">
        <el-button size="small" type="danger" plain :loading="submitting" @click="handleRespond(pendingProposal.id, false)">
          婉拒重选
        </el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="handleRespond(pendingProposal.id, true)">
          同意并锁定该位置
        </el-button>
      </div>
      <span v-else class="waiting-tag">等待对方确认中...</span>
    </div>

    <!-- Propose Dialog -->
    <el-dialog v-model="proposeDialogVisible" title="在真实校园地图上提议新会面地点" width="600px">
      <CampusMapPicker
        :campus-id="order.campusId || 1"
        v-model="draftLocation"
        @fence-change="val => (isFenceOk = val)"
      />
      <el-form label-position="top" style="margin-top: 14px">
        <el-form-item label="提议会面时间（选填）">
          <el-date-picker
            v-model="draftTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择期望碰面时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="给对方的协商说明（选填）">
          <el-input
            v-model="draftRemark"
            maxlength="120"
            placeholder="例如：刚下高数课在南区，我们在图书馆一楼监控区 60 米内碰面可以吗？"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="proposeDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!isFenceOk || !draftLocation.poiName"
          :loading="submitting"
          @click="submitProposal"
        >
          发送选点提案
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMeetupProposals, proposeMeetupLocation, respondMeetupProposal } from '@/api/order'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import { OrderStatus, type OrderDetail, type MeetupProposal } from '@/types/order'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import CampusMapPicker, { type MapLocationValue } from './CampusMapPicker.vue'

const props = defineProps<{
  order: OrderDetail
  currentUserId?: number
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const proposals = ref<MeetupProposal[]>([])
const proposeDialogVisible = ref(false)
const submitting = ref(false)
const isFenceOk = ref(true)
const draftTime = ref<string>('')
const draftRemark = ref('')

const effectiveRadiusMeters = computed(
  () => props.order.meetupRadiusMeters || props.order.tradeLocation?.validRadiusMeters || 80
)

const draftLocation = ref<MapLocationValue>({
  latitude: props.order.meetupLat,
  longitude: props.order.meetupLng,
  poiName: props.order.meetupPoiName || '',
  detailAddress: props.order.meetupAddress || '',
  radiusMeters: effectiveRadiusMeters.value
})

const canNegotiate = computed(() => {
  const s = props.order.orderStatus
  return s === OrderStatus.UNPAID || s === OrderStatus.PENDING_SHIP || s === OrderStatus.PENDING_RECEIPT
})

const pendingProposal = computed(() => proposals.value.find(p => p.proposalStatus === 0) || null)

function openProposeDialog() {
  draftLocation.value = {
    latitude: props.order.meetupLat,
    longitude: props.order.meetupLng,
    poiName: props.order.meetupPoiName || '',
    detailAddress: props.order.meetupAddress || '',
    radiusMeters: effectiveRadiusMeters.value
  }
  proposeDialogVisible.value = true
}

async function loadProposals() {
  if (!props.order?.id) return
  try {
    proposals.value = await listMeetupProposals(props.order.id)
  } catch {
    proposals.value = []
  }
}

async function submitProposal() {
  if (!draftLocation.value.latitude || !draftLocation.value.longitude || !draftLocation.value.poiName.trim()) {
    ElMessage.warning('请在校园地图上点击选择会面点并填写地标名称')
    return
  }
  submitting.value = true
  try {
    await proposeMeetupLocation(props.order.id, {
      latitude: draftLocation.value.latitude,
      longitude: draftLocation.value.longitude,
      poiName: draftLocation.value.poiName.trim(),
      detailAddress: draftLocation.value.detailAddress?.trim() || undefined,
      radiusMeters: draftLocation.value.radiusMeters || 80,
      proposedTime: draftTime.value || undefined,
      remark: draftRemark.value.trim() || undefined
    })
    ElMessage.success('会面选点提案已发出，等待对方确认')
    proposeDialogVisible.value = false
    draftRemark.value = ''
    await loadProposals()
    emit('updated')
  } catch (err) {
    notifyFallbackError(err, '发送会面提案失败')
  } finally {
    submitting.value = false
  }
}

async function handleRespond(proposalId: number, accept: boolean) {
  let rejectReason: string | undefined
  if (!accept) {
    try {
      const res = await ElMessageBox.prompt('请简要说明不便原因，方便对方重新在地图选点', '婉拒会面位置', {
        confirmButtonText: '提交说明',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：下午在东区上课赶不过去，建议约在中心食堂门口'
      })
      rejectReason = res.value
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    await respondMeetupProposal(proposalId, accept, rejectReason)
    ElMessage.success(accept ? '已同意并锁定线下会面位置与核销半径' : '已告知对方重新协商位置')
    await loadProposals()
    emit('updated')
  } catch (err) {
    notifyFallbackError(err, '响应提案失败')
  } finally {
    submitting.value = false
  }
}

watch(() => props.order.id, loadProposals)
onMounted(loadProposals)
</script>

<style scoped lang="scss">
.meetup-proposal-card {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;

  h2 {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .sub-text {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.current-spot-box {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;

  .spot-kicker {
    display: block;
    font-size: 11.5px;
    color: var(--xyf-ink-muted);
    margin-bottom: 3px;
  }

  strong {
    font-size: 15px;
    color: var(--xyf-ink);
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
  }

  .spot-meta {
    margin-top: 6px;
    font-size: 12px;
    color: var(--xyf-ink-muted);
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.pending-proposal-alert {
  padding: 14px 16px;
  border-radius: 10px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;

  .badge-pill {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 999px;
    background: #f59e0b;
    color: #ffffff;
    font-size: 11px;
    font-weight: 600;
    margin-right: 8px;
  }

  strong {
    font-size: 13.5px;
    color: #78350f;
  }

  p {
    margin: 4px 0 0;
    font-size: 12.5px;
    color: #92400e;
  }

  .waiting-tag {
    font-size: 12.5px;
    font-weight: 600;
    color: #b45309;
  }
}

.pending-actions {
  display: flex;
  gap: 8px;
}
</style>
