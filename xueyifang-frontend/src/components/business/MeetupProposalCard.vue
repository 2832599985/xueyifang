<template>
  <section class="xyf-surface-card meetup-proposal-card">
    <div class="head-row">
      <div>
        <span class="xyf-kicker">CAMPUS MEETUP CHECKPOINT</span>
        <h2>校内当面交接点与电子围栏协商</h2>
      </div>
      <XyfStatusBadge
        :tone="order.meetupConfirmed === 1 ? 'success' : 'warning'"
        :label="order.meetupConfirmed === 1 ? '双方已确认锁定' : '支持双向协商改点'"
      />
    </div>

    <!-- 2-Column Split: Mini Live Map Preview + Agreed Checkpoint Info -->
    <div class="checkpoint-split" :class="{ 'has-map': Boolean(order.meetupLat && order.meetupLng) }">
      <div v-if="order.meetupLat && order.meetupLng" class="mini-map-col">
        <RealCampusMap
          :height="168"
          :interactive="false"
          :latitude="Number(order.meetupLat)"
          :longitude="Number(order.meetupLng)"
          :radius-meters="effectiveRadiusMeters"
          :center-lat="Number(order.meetupLat)"
          :center-lng="Number(order.meetupLng)"
          :fence-valid="true"
        />
      </div>

      <div class="spot-detail-col">
        <div class="spot-main">
          <span class="spot-lbl">当前约定交接地标</span>
          <strong class="spot-title">
            {{ order.meetupPoiName || order.tradeLocationName || '暂未标定校内坐标' }}
          </strong>
          <p v-if="order.meetupAddress" class="spot-addr">{{ order.meetupAddress }}</p>
        </div>

        <div class="spot-chips xyf-tabular">
          <span class="s-chip">核销有效半径 {{ effectiveRadiusMeters }}m</span>
          <span v-if="order.scheduledStart" class="s-chip">
            约定时间 {{ formatDateTime(order.scheduledStart).slice(0, 16) }}
          </span>
        </div>

        <div v-if="canNegotiate" class="spot-actions">
          <el-button plain size="small" @click="openProposeDialog">
            发起改点 / 改期提案
          </el-button>
        </div>
      </div>
    </div>

    <!-- Active Pending Proposal Diff Banner -->
    <div v-if="pendingProposal" class="proposal-diff-bar">
      <div class="proposal-diff-bar__info">
        <span class="diff-badge">新位置协商提案</span>
        <div>
          <strong>
            {{ pendingProposal.proposerRole === 1 ? '买方' : '卖方' }}（{{ pendingProposal.proposerName }}）提议改至「{{ pendingProposal.poiName }}」
            <span v-if="pendingProposal.radiusMeters" class="xyf-tabular">（{{ pendingProposal.radiusMeters }}m 围栏）</span>
          </strong>
          <p v-if="pendingProposal.detailAddress || pendingProposal.remark">
            {{ pendingProposal.detailAddress }}
            <template v-if="pendingProposal.remark"> · 备注：{{ pendingProposal.remark }}</template>
          </p>
        </div>
      </div>

      <div v-if="pendingProposal.proposerId !== currentUserId" class="proposal-diff-bar__btns">
        <el-button size="small" plain :loading="submitting" @click="handleRespond(pendingProposal.id, false)">
          婉拒重选
        </el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="handleRespond(pendingProposal.id, true)">
          同意并锁定该位置
        </el-button>
      </div>
      <span v-else class="waiting-pill">等待对方确认...</span>
    </div>

    <!-- Propose Dialog -->
    <el-dialog v-model="proposeDialogVisible" title="在真实校园地图提议新会面地点" width="640px">
      <CampusMapPicker
        :campus-id="order.campusId || 1"
        v-model="draftLocation"
        @fence-change="val => (isFenceOk = val)"
      />
      <el-form label-position="top" style="margin-top: 14px">
        <div class="form-2col">
          <el-form-item label="提议会面时间（选填）">
            <el-date-picker
              v-model="draftTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择期望碰面时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="给对方的协商留言（选填）">
            <el-input
              v-model="draftRemark"
              maxlength="120"
              placeholder="如：刚下课在南区，改到图书馆一楼方便吗？"
            />
          </el-form-item>
        </div>
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
import RealCampusMap from './RealCampusMap.vue'
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
  gap: 16px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;

  h2 {
    margin: 3px 0 0;
    font-size: 16.5px;
    font-weight: 750;
    color: var(--xyf-ink);
  }
}

.checkpoint-split {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: center;

  &.has-map {
    grid-template-columns: 280px minmax(0, 1fr);

    @media (max-width: 720px) {
      grid-template-columns: 1fr;
    }
  }
}

.spot-detail-col {
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 12px;

  .spot-lbl {
    display: block;
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }

  .spot-title {
    display: block;
    margin-top: 2px;
    font-size: 17px;
    font-weight: 750;
    color: var(--xyf-ink);
  }

  .spot-addr {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
  }

  .spot-chips {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .s-chip {
      padding: 3px 10px;
      border-radius: 999px;
      background: #ffffff;
      border: 1px solid var(--xyf-border);
      font-size: 12px;
      font-weight: 600;
      color: var(--xyf-ink-2);
    }
  }
}

.proposal-diff-bar {
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--xyf-champagne-soft);
  border: 1px solid var(--xyf-champagne-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;

  &__info {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex: 1;
    min-width: 240px;

    .diff-badge {
      padding: 2px 8px;
      border-radius: 999px;
      background: #18181b;
      color: #e5c79e;
      font-size: 11px;
      font-weight: 650;
      white-space: nowrap;
      margin-top: 2px;
    }

    strong {
      display: block;
      font-size: 13.5px;
      color: var(--xyf-ink);
    }

    p {
      margin: 3px 0 0;
      font-size: 12.5px;
      color: var(--xyf-ink-2);
    }
  }

  &__btns {
    display: flex;
    gap: 8px;
  }

  .waiting-pill {
    font-size: 12px;
    font-weight: 600;
    color: #9a5b13;
  }
}

.form-2col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}
</style>
