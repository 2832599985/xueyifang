<template>
  <section v-if="order && order.orderStatus === OrderStatus.COMPLETED" class="xyf-surface-card cert-card">
    <div class="cert-header">
      <div>
        <span class="cert-kicker">SHA-256 DIGITAL FULFILLMENT CERTIFICATE · 校园技能实践数字存证</span>
        <h3>学艺坊校园技能交付与社会实践存证证书</h3>
      </div>
      <div class="cert-actions">
        <el-button size="small" type="primary" plain :loading="verifying" @click="handleVerifySeal">
          SHA-256 指纹在线验真
        </el-button>
        <el-button size="small" @click="handlePrint">打印 / 导出证书</el-button>
      </div>
    </div>

    <div v-if="certificate" class="cert-frame">
      <div class="cert-top-bar">
        <span class="cert-no">证书编号：{{ certificate.certificateNo }}</span>
        <span class="cert-status">✓ 平台担保核销已验真 ({{ certificate.verifyChannel }})</span>
      </div>

      <div class="cert-grid">
        <div class="cert-field">
          <span class="f-label">技能服务项目</span>
          <strong class="f-val">{{ certificate.serviceTitle }}（{{ certificate.serviceTagName }}）</strong>
        </div>
        <div class="cert-field">
          <span class="f-label">所属校区与交接地标</span>
          <strong class="f-val">{{ certificate.campusName }}{{ certificate.meetupPoiName ? ` · ${certificate.meetupPoiName}` : '' }}</strong>
        </div>
        <div class="cert-field">
          <span class="f-label">技能创作者（服务方）</span>
          <strong class="f-val">{{ certificate.sellerName }}（学号 {{ certificate.sellerStudentIdMasked }}）</strong>
        </div>
        <div class="cert-field">
          <span class="f-label">校内委托方（需求方）</span>
          <strong class="f-val">{{ certificate.buyerName }}（学号 {{ certificate.buyerStudentIdMasked }}）</strong>
        </div>
        <div class="cert-field">
          <span class="f-label">担保结算金额</span>
          <strong class="f-val">¥{{ Number(certificate.totalAmount || 0).toFixed(2) }}</strong>
        </div>
        <div class="cert-field">
          <span class="f-label">折算技能实践学时</span>
          <strong class="f-val is-highlight">{{ certificate.practiceCreditHours }} 学时</strong>
        </div>
      </div>

      <div class="seal-box">
        <span class="seal-label">SHA-256 防篡改数字指纹签章（链上/平台交叉核验锚点）：</span>
        <code class="seal-code">{{ certificate.sha256Seal }}</code>
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
    if (res.authentic) {
      ElMessage.success(res.message)
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    verifying.value = false
  }
}

const handlePrint = () => {
  window.print()
}

watch(
  () => [props.order?.id, props.order?.orderStatus],
  () => {
    fetchCertificate()
  }
)

onMounted(() => {
  fetchCertificate()
})
</script>

<style scoped lang="scss">
.cert-card {
  padding: 20px 22px;
  border-left: 4px solid #10b981;
}

.cert-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  .cert-kicker {
    display: block;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #059669;
  }

  h3 {
    margin: 4px 0 0;
    font-size: 16px;
    font-weight: 700;
  }

  .cert-actions {
    display: flex;
    gap: 8px;
  }
}

.cert-frame {
  background: linear-gradient(135deg, #f0fdf4 0%, #f8fafc 100%);
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 16px 18px;
}

.cert-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px dashed #86efac;
  font-size: 12.5px;

  .cert-no {
    font-weight: 700;
    color: #065f46;
  }

  .cert-status {
    font-weight: 600;
    color: #059669;
  }
}

.cert-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 14px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.cert-field {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .f-label {
    font-size: 11.5px;
    color: #64748b;
  }

  .f-val {
    font-size: 13.5px;
    font-weight: 600;
    color: #0f172a;

    &.is-highlight {
      color: #059669;
      font-weight: 700;
    }
  }
}

.seal-box {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .seal-label {
    font-size: 11px;
    color: #94a3b8;
  }

  .seal-code {
    font-family: monospace;
    font-size: 12px;
    color: #34d399;
    word-break: break-all;
  }
}
</style>
