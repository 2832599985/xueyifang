<template>
  <div class="dispute-detail-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        :title="detail ? `纠纷单详情 · #${detail.disputeId}` : '纠纷单详情'"
        :show-back="true"
        back-text="返回纠纷列表"
        back-to="/disputes"
      />

      <div v-if="loading" class="xyf-surface-card loading-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!detail" class="xyf-surface-card loading-card">
        <XyfEmptyState
          title="无法获取纠纷单详情"
          description="请确认该纠纷单存在且您有权查看。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/disputes')">返回列表</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Ticket Header Card -->
        <section class="xyf-surface-card ticket-head-card">
          <div class="ticket-head__left">
            <div class="ticket-badges">
              <XyfStatusBadge
                size="md"
                :tone="statusTone(detail.disputeStatus)"
                :label="getDisputeStatusText(detail.disputeStatus)"
              />
              <XyfStatusBadge
                size="md"
                tone="info"
                :label="getDisputeTypeText(detail.disputeType)"
              />
            </div>
            <h2>{{ detail.serviceTitle }}</h2>
            <div class="ticket-meta xyf-tabular">
              <span>关联订单：<button type="button" class="order-link xyf-mono" @click="router.push(`/orders/${detail.orderId}`)">{{ detail.orderNumber }}</button></span>
              <span>·</span>
              <span>提交时间：{{ formatDateTime(detail.createTime) }}</span>
            </div>
          </div>

          <div class="ticket-head__right">
            <span class="amount-label">涉及订单金额</span>
            <strong class="xyf-tabular">{{ formatPrice(detail.totalAmount) }}</strong>
            <el-button size="small" plain @click="router.push(`/orders/${detail.orderId}`)">
              查看原订单
            </el-button>
          </div>
        </section>

        <div class="detail-grid">
          <div class="detail-main">
            <!-- Initiator Claim & Evidence -->
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>纠纷陈述与举证材料</h2>
              </div>
              <div class="claim-box">
                <p>{{ detail.description }}</p>
              </div>
              <div v-if="detail.evidenceImages?.length" class="evidence-grid">
                <el-image
                  v-for="(img, idx) in detail.evidenceImages"
                  :key="img + idx"
                  :src="img"
                  :preview-src-list="detail.evidenceImages"
                  :initial-index="idx"
                  fit="cover"
                  class="evidence-thumb"
                />
              </div>
            </section>

            <!-- Seller Delivery Voucher Snapshot -->
            <section
              v-if="detail.deliveryNote || detail.deliveryImages?.length"
              class="xyf-surface-card panel-card"
            >
              <div class="xyf-section-heading">
                <h2>卖方履约交付凭证存档</h2>
              </div>
              <div v-if="detail.deliveryNote" class="claim-box">
                <p>{{ detail.deliveryNote }}</p>
              </div>
              <div v-if="detail.deliveryImages?.length" class="evidence-grid">
                <el-image
                  v-for="(img, idx) in detail.deliveryImages"
                  :key="img + idx"
                  :src="img"
                  :preview-src-list="detail.deliveryImages"
                  :initial-index="idx"
                  fit="cover"
                  class="evidence-thumb"
                />
              </div>
            </section>

            <!-- Admin Ruling -->
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>平台仲裁处理结果</h2>
              </div>

              <div v-if="detail.adminReply || detail.resolution" class="ruling-box">
                <div v-if="detail.resolution" class="ruling-row">
                  <span class="ruling-label">裁决结论</span>
                  <div class="ruling-text">{{ detail.resolution }}</div>
                </div>
                <div v-if="detail.adminReply" class="ruling-row">
                  <span class="ruling-label">管理员说明</span>
                  <div class="ruling-text">{{ detail.adminReply }}</div>
                </div>
              </div>

              <el-alert
                v-else
                title="平台工作人员正在核实订单记录与双方沟通情况，请耐心等待。"
                type="info"
                :closable="false"
                show-icon
              />
            </section>
          </div>

          <!-- Parties Card -->
          <aside class="detail-aside">
            <section class="xyf-surface-card panel-card">
              <div class="xyf-section-heading">
                <h2>涉事双方</h2>
              </div>

              <div class="party-card">
                <div class="party-row">
                  <XyfAvatar :src="detail.buyerAvatar" :name="detail.buyerName" :size="38" />
                  <div class="party-info">
                    <div class="party-name">
                      {{ detail.buyerName }}
                      <XyfStatusBadge
                        v-if="detail.disputeInitiatorId === detail.buyerId"
                        tone="warning"
                        label="发起方"
                      />
                    </div>
                    <span class="party-role">买方</span>
                  </div>
                </div>

                <div class="party-row">
                  <XyfAvatar :src="detail.sellerAvatar" :name="detail.sellerName" :size="38" />
                  <div class="party-info">
                    <div class="party-name">
                      {{ detail.sellerName }}
                      <XyfStatusBadge
                        v-if="detail.disputeInitiatorId === detail.sellerId"
                        tone="warning"
                        label="发起方"
                      />
                    </div>
                    <span class="party-role">卖方</span>
                  </div>
                </div>
              </div>
            </section>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDisputeDetail } from '@/api/dispute'
import { notifyFallbackError } from '@/api/request'
import { formatPrice, formatDateTime } from '@/utils/format'
import {
  DISPUTE_STATUS,
  getDisputeTypeText,
  getDisputeStatusText,
  type DisputeDetail
} from '@/types/dispute'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const route = useRoute()

const disputeId = computed(() => Number(route.params.id))

const loading = ref(true)
const detail = ref<DisputeDetail | null>(null)

function statusTone(status: number) {
  if (status === DISPUTE_STATUS.RESOLVED) return 'success'
  if (status === DISPUTE_STATUS.REJECTED) return 'danger'
  if (status === DISPUTE_STATUS.PROCESSING) return 'info'
  return 'warning'
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getDisputeDetail(disputeId.value)
  } catch (err) {
    notifyFallbackError(err, '获取纠纷详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.loading-card {
  padding: 28px;
}

.ticket-head-card {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.ticket-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.ticket-head__left {
  h2 {
    margin: 0 0 8px;
    font-size: 20px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.ticket-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: var(--xyf-ink-muted);

  .order-link {
    border: none;
    background: transparent;
    padding: 0;
    color: var(--xyf-primary);
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.ticket-head__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;

  .amount-label {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }

  strong {
    font-size: 24px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.75fr);
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

.claim-box {
  padding: 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);

  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.7;
    color: var(--xyf-ink-secondary);
    white-space: pre-wrap;
  }
}

.evidence-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 10px;
}

.evidence-thumb {
  width: 100%;
  height: 92px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  overflow: hidden;
  cursor: zoom-in;
}

.ruling-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
}

.ruling-row {
  .ruling-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: var(--xyf-ink-3);
    margin-bottom: 4px;
  }

  .ruling-text {
    font-size: 14px;
    line-height: 1.65;
    color: var(--xyf-ink);
    white-space: pre-wrap;
  }
}

.party-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.party-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.party-info {
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
