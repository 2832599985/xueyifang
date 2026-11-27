<template>
  <div class="admin-system-health-page">
    <XyfPageHeader title="系统检查" dense>
      <template #actions>
        <el-button type="primary" :loading="loading" @click="runInspection">
          <el-icon><Refresh /></el-icon>
          重新检查
        </el-button>
      </template>
    </XyfPageHeader>

    <div v-if="loading && !report" class="xyf-surface-card" style="padding: 32px">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="report">
      <!-- Top Hero Split: SVG Circular Health Gauge + 4 Invariant Metrics -->
      <div class="health-hero-split">
        <section class="xyf-surface-card gauge-hero xyf-tabular">
          <div class="gauge-ring-box">
            <svg viewBox="0 0 120 120" class="health-svg">
              <circle cx="60" cy="60" r="48" fill="none" stroke="#f1f1f4" stroke-width="9" />
              <circle
                cx="60"
                cy="60"
                r="48"
                fill="none"
                :stroke="report.overallHealthScore >= 90 ? '#18181b' : '#dc2626'"
                stroke-width="9"
                stroke-linecap="round"
                :stroke-dasharray="2 * Math.PI * 48"
                :stroke-dashoffset="2 * Math.PI * 48 * (1 - Math.min(1, Math.max(0, report.overallHealthScore / 100)))"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="gauge-center">
              <strong>{{ report.overallHealthScore }}</strong>
              <span>健康指数</span>
            </div>
          </div>

          <div class="gauge-copy">
            <XyfStatusBadge
              size="md"
              :tone="report.healthLevel === 'EXCELLENT' ? 'success' : report.healthLevel === 'WARNING' ? 'warning' : 'danger'"
              :label="healthLevelLabel(report.healthLevel)"
              :dot="report.healthLevel !== 'EXCELLENT'"
            />
            <h3>{{ report.overallHealthScore >= 90 ? '系统运行正常' : '存在异常项，请及时处理' }}</h3>
            <p class="batch-line xyf-mono">
              批次 {{ report.auditBatchNo }} · {{ report.inspectTime }}
            </p>
          </div>
        </section>

        <section class="invariant-tiles">
          <XyfStatTile
            label="异常负余额账户"
            :value="report.negativeWalletCount"
            :hint="`已检查 ${report.totalUsers} 个账户`"
            :tone="report.negativeWalletCount === 0 ? 'default' : 'danger'"
          />
          <XyfStatTile
            label="冻结金额异常订单"
            :value="report.escrowMismatchCount"
            :hint="`已完成订单 ${report.totalCompletedOrders} 笔`"
            :tone="report.escrowMismatchCount === 0 ? 'default' : 'danger'"
          />
          <XyfStatTile
            label="数据库索引状态"
            :value="report.compositeIndexOptimized ? '正常 (6/6)' : '待检查'"
            :hint="`在架 ${report.totalActiveServices} · 拼班 ${report.totalGroupSessions} · 悬赏 ${report.totalDemands}`"
          />
          <XyfStatTile
            label="待处理风险 / 逾期单"
            :value="`${report.pendingHighRiskAlertCount} / ${report.overdueUncompensatedOrderCount}`"
            hint="高风险预警 / 逾期未赔付"
          />
        </section>
      </div>

      <!-- Diagnostic Matrix Grid -->
      <section class="diagnostic-section">
        <div class="xyf-section-heading">
          <h2>检查明细</h2>
        </div>

        <div class="diag-grid">
          <article
            v-for="item in report.securityChecks"
            :key="item.checkCode"
            class="xyf-surface-card diag-card"
            :class="{ 'is-failed': !item.passed }"
          >
            <div class="diag-card__top">
              <h3>{{ item.checkTitle }}</h3>
              <XyfStatusBadge
                :tone="item.passed ? 'success' : 'danger'"
                :label="item.passed ? '正常' : '异常'"
              />
            </div>

            <p class="diag-detail">{{ item.detail }}</p>

            <div class="diag-rec">
              <span>处理建议</span>
              <p>{{ item.recommendation }}</p>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { inspectSystemHealth, type SystemHealthAuditData } from '@/api/systemAudit'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'

const loading = ref(false)
const report = ref<SystemHealthAuditData | null>(null)

function healthLevelLabel(level?: string) {
  if (level === 'EXCELLENT') return '运行正常'
  if (level === 'WARNING') return '存在预警'
  return '存在异常'
}

const runInspection = async () => {
  loading.value = true
  try {
    report.value = await inspectSystemHealth()
    ElMessage.success(`检查完成，当前评分 ${report.value.overallHealthScore} 分`)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  runInspection()
})
</script>

<style scoped lang="scss">
.admin-system-health-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.health-hero-split {
  display: grid;
  grid-template-columns: minmax(320px, 0.85fr) minmax(0, 1.35fr);
  gap: 18px;
  align-items: stretch;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.gauge-hero {
  padding: 24px 26px;
  display: flex;
  align-items: center;
  gap: 22px;

  .gauge-ring-box {
    position: relative;
    width: 112px;
    height: 112px;
    flex-shrink: 0;

    .health-svg {
      width: 100%;
      height: 100%;
    }

    .gauge-center {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      line-height: 1.1;

      strong {
        font-size: 28px;
        font-weight: 700;
        color: var(--xyf-ink);
      }

      span {
        font-size: 11px;
        color: var(--xyf-ink-3);
      }
    }
  }

  .gauge-copy {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;

    .level-pill {
      padding: 3px 10px;
      border-radius: 999px;
      background: #18181b;
      color: #e5c79e;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.05em;

      &.is-critical {
        background: #dc2626;
        color: #ffffff;
      }
    }

    h3 {
      margin: 2px 0 0;
      font-size: 17px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    .batch-line {
      margin: 0;
      font-size: 11.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.invariant-tiles {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.diag-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
  }
}

.diag-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &.is-failed {
    border-color: #f5caca;
    background: #fffcfc;
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .diag-detail {
    margin: 0;
    font-size: 13px;
    line-height: 1.55;
    color: var(--xyf-ink-2);
    flex: 1;
  }

  .diag-rec {
    margin-top: auto;
    padding: 10px 12px;
    border-radius: var(--xyf-radius-sm);
    background: var(--xyf-surface-soft);

    span {
      display: block;
      font-size: 12px;
      font-weight: 600;
      color: var(--xyf-ink-3);
      margin-bottom: 2px;
    }

    p {
      margin: 0;
      font-size: 12px;
      color: var(--xyf-ink-2);
      line-height: 1.45;
    }
  }
}
</style>
