<template>
  <section v-if="order && order.orderStatus === OrderStatus.COMPLETED" class="xyf-surface-card cert-shell">
    <div class="cert-toolbar">
      <div>
        <h3>服务完成凭证</h3>
      </div>

      <div class="cert-toolbar__btns">
        <el-button size="small" plain :loading="verifying" @click="handleVerifySeal">
          {{ verifiedAuthentic ? '✓ 验真通过' : '在线验真' }}
        </el-button>
        <el-button size="small" type="primary" @click="handlePrint">
          打印 / 导出证书
        </el-button>
      </div>
    </div>

    <!-- Architectural Double-Hairline Parchment Certificate Frame -->
    <div v-if="certificate" class="diploma-frame">
      <!-- Official SVG Wax/Ink Stamp Emblem -->
      <svg class="diploma-seal-svg" viewBox="0 0 96 96" fill="none" aria-hidden="true">
        <circle cx="48" cy="48" r="42" stroke="rgba(184,144,88,0.38)" stroke-width="1.5" stroke-dasharray="4 3" />
        <circle cx="48" cy="48" r="34" stroke="rgba(184,144,88,0.55)" stroke-width="1.2" />
        <path d="M35 49L44 58L62 39" stroke="rgba(184,144,88,0.65)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
      </svg>

      <div class="diploma-head">
        <div>
          <span class="diploma-serial xyf-mono">NO. {{ certificate.certificateNo }}</span>
          <h4>学艺坊服务完成凭证</h4>
        </div>
        <span class="channel-pill">
          核销通道：{{ certificate.verifyChannel }}
        </span>
      </div>

      <div class="diploma-grid xyf-tabular">
        <div class="d-cell">
          <span>技能服务项目</span>
          <strong>{{ certificate.serviceTitle }}（{{ certificate.serviceTagName }}）</strong>
        </div>
        <div class="d-cell">
          <span>所属校区与核销地标</span>
          <strong>{{ certificate.campusName }}{{ certificate.meetupPoiName ? ` · ${certificate.meetupPoiName}` : '' }}</strong>
        </div>
        <div class="d-cell">
          <span>技能服务创作者</span>
          <strong>{{ certificate.sellerName }}（{{ certificate.sellerStudentIdMasked }}）</strong>
        </div>
        <div class="d-cell">
          <span>校内委托需求方</span>
          <strong>{{ certificate.buyerName }}（{{ certificate.buyerStudentIdMasked }}）</strong>
        </div>
        <div class="d-cell">
          <span>担保结算总额</span>
          <strong>¥{{ Number(certificate.totalAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="d-cell is-gold">
          <span>折算技能实践学时</span>
          <strong>{{ certificate.practiceCreditHours }} 学时</strong>
        </div>
      </div>

      <div class="hash-vault">
        <div class="hash-vault__top">
          <span>校验码（SHA-256）</span>
          <button type="button" class="copy-hash-btn" @click="copySealHash">复制校验码</button>
        </div>
        <code class="hash-code xyf-mono">{{ certificate.sha256Seal }}</code>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { OrderStatus } from '@/types/order'
import {
  getOrderCertificate,
  verifyOrderCertificateSeal,
  type OrderDeliveryCertificateData
} from '@/api/systemAudit'

const props = defineProps<{
  order: Record<string, any> | null
}>()

const certificate = ref<OrderDeliveryCertificateData | null>(null)
const verifying = ref(false)
const verifiedAuthentic = ref(false)

const fetchCertificate = async () => {
  if (!props.order?.id || props.order.orderStatus !== OrderStatus.COMPLETED) {
    certificate.value = null
    return
  }
  try {
    certificate.value = await getOrderCertificate(Number(props.order.id))
  } catch {
    certificate.value = null
  }
}

const handleVerifySeal = async () => {
  if (!certificate.value) return
  verifying.value = true
  try {
    const res = await verifyOrderCertificateSeal(
      certificate.value.orderId,
      certificate.value.sha256Seal
    )
    verifiedAuthentic.value = Boolean(res.authentic)
    if (res.authentic) {
      ElMessage.success(res.message)
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    verifying.value = false
  }
}

const copySealHash = async () => {
  if (!certificate.value?.sha256Seal) return
  try {
    await navigator.clipboard.writeText(certificate.value.sha256Seal)
    ElMessage.success('SHA-256 数字指纹已复制')
  } catch {
    ElMessage.info(certificate.value.sha256Seal)
  }
}

const handlePrint = () => {
  window.print()
}

watch(
  () => [props.order?.id, props.order?.orderStatus],
  () => {
    verifiedAuthentic.value = false
    fetchCertificate()
  }
)

onMounted(() => {
  fetchCertificate()
})
</script>

<style scoped lang="scss">
.cert-shell {
  padding: 22px 24px;
}

.cert-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;

  h3 {
    margin: 3px 0 0;
    font-size: 16.5px;
    font-weight: 750;
    color: var(--xyf-ink);
  }

  &__btns {
    display: flex;
    gap: 8px;
  }
}

.diploma-frame {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  background: linear-gradient(180deg, #fffdf9 0%, #ffffff 65%);
  border: 1px solid #e6d7be;
  box-shadow: inset 0 0 0 3px #ffffff, inset 0 0 0 4px rgba(230, 215, 190, 0.55);
  padding: 22px 22px 18px;
}

.diploma-seal-svg {
  position: absolute;
  right: 18px;
  top: 14px;
  width: 82px;
  height: 82px;
  pointer-events: none;
  opacity: 0.7;
}

.diploma-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid #eee4d3;

  .diploma-serial {
    font-size: 11.5px;
    font-weight: 600;
    color: #9a5b13;
  }

  h4 {
    margin: 3px 0 0;
    font-size: 16px;
    font-weight: 750;
    color: var(--xyf-ink);
  }

  .channel-pill {
    position: relative;
    z-index: 2;
    padding: 3px 10px;
    border-radius: 999px;
    background: #18181b;
    color: #e5c79e;
    font-size: 11px;
    font-weight: 650;
  }
}

.diploma-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 16px;

  @media (max-width: 680px) {
    grid-template-columns: 1fr;
  }
}

.d-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;

  span {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }

  strong {
    font-size: 13.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  &.is-gold strong {
    color: #9a5b13;
  }
}

.hash-vault {
  background: #141417;
  border-radius: 10px;
  padding: 11px 14px;
  color: #e4e4e7;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 11px;
    color: #a1a1aa;
    margin-bottom: 4px;
  }

  .copy-hash-btn {
    border: none;
    background: rgba(255, 255, 255, 0.1);
    color: #f4f4f5;
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 6px;
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  .hash-code {
    display: block;
    font-size: 11.5px;
    color: #e5c79e;
    word-break: break-all;
  }
}
</style>
