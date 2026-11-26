<template>
  <div class="admin-dispute-detail">
    <XyfPageHeader
      :title="detail ? `仲裁处理 · 纠纷 #${detail.disputeId}` : '仲裁详情'"
      :show-back="true"
      back-text="返回纠纷列表"
      back-to="/admin/disputes"
    >
      <template #actions v-if="detail">
        <el-button @click="router.push(`/admin/orders/${detail.orderId}`)">
          查看关联订单详情
        </el-button>
      </template>
    </XyfPageHeader>

    <div v-if="loading" class="xyf-surface-card loading-card">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="!detail" class="xyf-surface-card loading-card">
      <XyfEmptyState title="未找到该纠纷记录" />
    </div>

    <template v-else>
      <!-- Header Summary -->
      <section class="xyf-surface-card ticket-summary">
        <div class="ticket-summary__left">
          <div class="badges-row">
            <XyfStatusBadge
              size="md"
              :tone="statusTone(detail.disputeStatus)"
              :label="getDisputeStatusText(detail.disputeStatus)"
            />
            <XyfStatusBadge
              size="md"
              tone="accent"
              :label="getDisputeTypeText(detail.disputeType)"
              :dot="false"
            />
          </div>
          <h2>{{ detail.serviceTitle }}</h2>
          <p class="ticket-meta xyf-tabular">
            <span>订单号 <span class="xyf-mono">{{ detail.orderNumber }}</span></span>
            <span>·</span>
            <span>提交时间 {{ formatDateTime(detail.createTime) }}</span>
          </p>
        </div>

        <div class="ticket-summary__right">
          <span class="amt-label">涉及金额</span>
          <strong class="xyf-tabular">{{ formatPrice(detail.totalAmount) }}</strong>
        </div>
      </section>

      <div class="detail-grid">
        <div class="detail-main">
          <!-- 1. Dispute Claim & Evidence -->
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>申诉陈述与举证图片</h2>
            </div>
            <div class="claim-box">
              {{ detail.description }}
            </div>
            <div v-if="detail.evidenceImages?.length" class="evidence-grid">
              <el-image
                v-for="(img, idx) in detail.evidenceImages"
                :key="img + idx"
                :src="img"
                :preview-src-list="detail.evidenceImages"
                :initial-index="idx"
                fit="cover"
                class="evidence-img"
              />
            </div>
          </section>

          <!-- 2. Seller Fulfillment Proof Comparison -->
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>卖方订单交付凭证核验</h2>
            </div>

            <template v-if="detail.deliveryNote || detail.deliveryImages?.length">
              <div v-if="detail.deliveryNote" class="claim-box">
                {{ detail.deliveryNote }}
              </div>
              <div v-if="detail.deliveryImages?.length" class="evidence-grid">
                <el-image
                  v-for="(img, idx) in detail.deliveryImages"
                  :key="img + idx"
                  :src="img"
                  :preview-src-list="detail.deliveryImages"
                  :initial-index="idx"
                  fit="cover"
                  class="evidence-img"
                />
              </div>
            </template>
            <p v-else class="empty-proof-hint">
              卖方在发货节点未上传附加文字说明或凭证截图。
            </p>
          </section>

          <!-- 3. Real-name Chat Evidence Transcript -->
          <section class="xyf-surface-card panel-card">
            <div class="section-bar">
              <h2>交易双方站内沟通留痕快照</h2>
              <span class="record-count xyf-tabular">最近 {{ chatLogs.length }} 条记录</span>
            </div>

            <div v-if="chatLogs.length === 0" class="empty-proof-hint">
              买卖双方暂无站内私信沟通记录。
            </div>

            <div v-else class="transcript-list">
              <div v-for="msg in chatLogs" :key="msg.messageId" class="transcript-row">
                <div class="transcript-row__head">
                  <span class="speaker-tag" :class="msg.sender?.userId === detail.buyerId ? 'is-buyer' : 'is-seller'">
                    {{ msg.sender?.userId === detail.buyerId ? '买方' : '卖方' }}
                  </span>
                  <span class="speaker-name">{{ msg.sender?.realName || `用户 #${msg.sender?.userId}` }}</span>
                  <span class="speaker-time xyf-mono">{{ msg.createTime }}</span>
                </div>

                <div class="transcript-row__body">
                  <el-image
                    v-if="msg.messageType === 2"
                    :src="msg.content"
                    :preview-src-list="[msg.content]"
                    fit="cover"
                    class="transcript-img"
                  />
                  <p v-else>{{ msg.content }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- 4. Arbitration Decision Form or Read-only Result -->
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>{{ canArbitrate ? '管理员裁决处理' : '仲裁处理档案' }}</h2>
            </div>

            <template v-if="canArbitrate">
              <el-form
                ref="formRef"
                :model="handleForm"
                :rules="rules"
                label-position="top"
              >
                <el-form-item label="裁决方向" prop="actionType">
                  <el-radio-group v-model="handleForm.actionType">
                    <el-radio-button :label="HANDLE_ACTION.RESOLVE">支持申诉并解决</el-radio-button>
                    <el-radio-button :label="HANDLE_ACTION.REJECT">驳回申诉请求</el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <el-form-item
                  v-if="handleForm.actionType === HANDLE_ACTION.RESOLVE"
                  label="资金处理策略"
                >
                  <el-radio-group v-model="handleForm.needRefund">
                    <el-radio :label="true" :disabled="!isEscrowRefundable">
                      将担保资金原路退还买方
                      <span v-if="detail.frozenAmount !== undefined" class="xyf-tabular">
                        （当前冻结：{{ formatPrice(detail.frozenAmount || 0) }}）
                      </span>
                    </el-radio>
                    <el-radio :label="false">维持交易结算（不触发退款）</el-radio>
                  </el-radio-group>
                  <div v-if="!isEscrowRefundable" class="refund-disabled-hint">
                    提示：该订单已完成确认收货结算，担保资金已释放给卖家，仅支持裁决归档，无法执行原路退款。
                  </div>
                </el-form-item>

                <el-form-item label="裁决结论摘要" prop="resolution">
                  <el-input
                    v-model="handleForm.resolution"
                    placeholder="例如：核实卖方未按期交付，裁定全额退款给买方并完结订单"
                    maxlength="200"
                    show-word-limit
                  />
                </el-form-item>

                <el-form-item label="回复交易双方详细说明" prop="adminReply">
                  <el-input
                    v-model="handleForm.adminReply"
                    type="textarea"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    placeholder="请写明事实依据与处理理由，双端纠纷详情页将同步展示此回复..."
                  />
                </el-form-item>

                <div class="submit-row">
                  <el-button
                    type="primary"
                    size="large"
                    :loading="submitting"
                    @click="submitDecision"
                  >
                    确认提交仲裁裁决
                  </el-button>
                </div>
              </el-form>
            </template>

            <template v-else>
              <div class="resolved-box">
                <div class="resolved-item" v-if="detail.resolution">
                  <span>裁决结论</span>
                  <p>{{ detail.resolution }}</p>
                </div>
                <div class="resolved-item" v-if="detail.adminReply">
                  <span>管理员说明</span>
                  <p>{{ detail.adminReply }}</p>
                </div>
              </div>
            </template>
          </section>
        </div>

        <aside class="detail-aside">
          <section class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>涉事交易方</h2>
            </div>

            <div class="party-stack">
              <div class="party-row">
                <XyfAvatar :src="detail.buyerAvatar" :name="detail.buyerName" :size="38" />
                <div>
                  <div class="party-name">
                    {{ detail.buyerName }}
                    <XyfStatusBadge
                      v-if="detail.disputeInitiatorId === detail.buyerId"
                      tone="warning"
                      label="发起方"
                      :dot="false"
                    />
                  </div>
                  <div class="party-role">买方 #{{ detail.buyerId }}</div>
                </div>
              </div>

              <div class="party-row">
                <XyfAvatar :src="detail.sellerAvatar" :name="detail.sellerName" :size="38" />
                <div>
                  <div class="party-name">
                    {{ detail.sellerName }}
                    <XyfStatusBadge
                      v-if="detail.disputeInitiatorId === detail.sellerId"
                      tone="warning"
                      label="发起方"
                      :dot="false"
                    />
                  </div>
                  <div class="party-role">卖方 #{{ detail.sellerId }}</div>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getAdminDisputeDetail, getAdminDisputeChatLogs, handleDispute } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTime } from '@/utils/format'
import {
  DISPUTE_STATUS,
  HANDLE_ACTION,
  getDisputeTypeText,
  getDisputeStatusText,
  type DisputeDetail
} from '@/types/dispute'
import type { ChatMessage } from '@/types/chat'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const route = useRoute()

const disputeId = computed(() => Number(route.params.id))

const formRef = ref<FormInstance>()
const loading = ref(true)
const submitting = ref(false)
const detail = ref<DisputeDetail | null>(null)
const chatLogs = ref<ChatMessage[]>([])

const handleForm = reactive({
  actionType: HANDLE_ACTION.RESOLVE as number,
  needRefund: true,
  resolution: '',
  adminReply: ''
})

const rules: FormRules = {
  resolution: [{ required: true, message: '请填写裁决结论摘要', trigger: 'blur' }],
  adminReply: [{ required: true, message: '请填写管理员回复说明', trigger: 'blur' }]
}

const canArbitrate = computed(() => {
  const s = detail.value?.disputeStatus
  return s === DISPUTE_STATUS.PENDING || s === DISPUTE_STATUS.PROCESSING
})

const isEscrowRefundable = computed(() => {
  if (!detail.value) return false
  if (detail.value.frozenAmount === undefined || detail.value.frozenAmount === null) return true
  return Number(detail.value.frozenAmount) > 0
})

function statusTone(status: number) {
  if (status === DISPUTE_STATUS.RESOLVED) return 'success'
  if (status === DISPUTE_STATUS.REJECTED) return 'danger'
  if (status === DISPUTE_STATUS.PROCESSING) return 'info'
  return 'warning'
}

async function fetchDetail() {
  loading.value = true
  try {
    const [disputeRes, logsRes] = await Promise.all([
      getAdminDisputeDetail(disputeId.value),
      getAdminDisputeChatLogs(disputeId.value).catch(() => [])
    ])
    detail.value = disputeRes
    chatLogs.value = logsRes || []
    if (detail.value && detail.value.frozenAmount !== undefined && Number(detail.value.frozenAmount) <= 0) {
      handleForm.needRefund = false
    }
  } catch (err) {
    notifyFallbackError(err, '获取纠纷仲裁详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function submitDecision() {
  if (!formRef.value || submitting.value || !detail.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  try {
    const summaryText =
      handleForm.actionType === HANDLE_ACTION.RESOLVE
        ? handleForm.needRefund
          ? '确认裁定支持诉求并执行退款给买家吗？'
          : '确认裁定结案（不执行退款）吗？'
        : '确认驳回此次纠纷诉求吗？'

    await ElMessageBox.confirm(summaryText, '确认提交裁决', {
      confirmButtonText: '确认提交',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true
    await handleDispute(detail.value.disputeId, {
      actionType: handleForm.actionType,
      resolution: handleForm.resolution.trim(),
      adminReply: handleForm.adminReply.trim(),
      needRefund:
        handleForm.actionType === HANDLE_ACTION.RESOLVE ? handleForm.needRefund : false
    })
    ElMessage.success('仲裁裁决已生效')
    await fetchDetail()
  } catch (err) {
    if (err !== 'cancel') notifyFallbackError(err, '提交仲裁失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.ticket-summary {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;

  h2 {
    margin: 0 0 6px;
    font-size: 19px;
    font-weight: 750;
    color: var(--xyf-ink);
  }
}

.badges-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.ticket-meta {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ticket-summary__right {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;

  .amt-label {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  strong {
    font-size: 26px;
    font-weight: 800;
    color: var(--xyf-ink);
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.75fr);
  gap: 20px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card {
  padding: 22px 24px;
}

.section-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .record-count {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.claim-box {
  padding: 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  font-size: 14px;
  line-height: 1.7;
  color: var(--xyf-ink-secondary);
  white-space: pre-wrap;
}

.evidence-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}

.evidence-img {
  width: 100%;
  height: 90px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  overflow: hidden;
  cursor: zoom-in;
}

.empty-proof-hint {
  margin: 0;
  padding: 14px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px dashed var(--xyf-border);
  font-size: 13px;
  color: var(--xyf-ink-3);
}

.transcript-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
}

.transcript-row {
  padding: 10px 12px;
  border-radius: 9px;
  background: #ffffff;
  border: 1px solid var(--xyf-border-light);

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
    font-size: 12px;
  }

  .speaker-tag {
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    background: #f4f4f5;
    color: var(--xyf-ink);

    &.is-buyer {
      background: #18181b;
      color: #ffffff;
    }
  }

  .speaker-name {
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .speaker-time {
    margin-left: auto;
    color: var(--xyf-ink-3);
    font-size: 11px;
  }

  &__body {
    p {
      margin: 0;
      font-size: 13.5px;
      line-height: 1.55;
      color: var(--xyf-ink-2);
      white-space: pre-wrap;
    }
  }

  .transcript-img {
    width: 116px;
    height: 86px;
    border-radius: 8px;
    border: 1px solid var(--xyf-border);
    cursor: zoom-in;
  }
}

.refund-disabled-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.submit-row {
  display: flex;
  justify-content: flex-end;
}

.resolved-box {
  padding: 16px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  display: flex;
  flex-direction: column;
  gap: 12px;

  .resolved-item {
    span {
      display: block;
      font-size: 12px;
      font-weight: 650;
      color: var(--xyf-ink-3);
      margin-bottom: 4px;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.6;
      color: var(--xyf-ink);
    }
  }
}

.party-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.party-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .party-name {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .party-role {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
