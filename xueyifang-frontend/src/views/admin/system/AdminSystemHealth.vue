<template>
  <div class="admin-system-health-page">
    <div class="health-banner xyf-surface-card">
      <div class="health-banner__info">
        <span class="health-kicker">HIGH-CONCURRENCY INDEX &amp; SECURITY INVARIANT RADAR · v4.5</span>
        <h2>全链路资金不变量、复合覆盖索引与安全健康自检中心</h2>
        <p>
          实时核验全平台学生钱包非负不变量、在途担保订单与冻结资金池 1:1 锚定关系、在架服务创作者实名资质、L3 高危互刷积压与 6 组高并发复合覆盖索引运行状态。
        </p>
      </div>
      <div class="health-banner__actions">
        <div class="score-badge" :class="levelClass" v-if="report">
          <span class="score-num">{{ report.overallHealthScore }}</span>
          <span class="score-unit">分 · {{ report.healthLevel }}</span>
        </div>
        <el-button type="primary" size="large" :loading="loading" @click="runInspection">
          重新执行全链路深度自检
        </el-button>
      </div>
    </div>

    <div class="kpi-grid" v-if="report">
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">钱包非负不变量异常数</span>
        <strong class="kpi-val" :class="report.negativeWalletCount === 0 ? 'is-ok' : 'is-err'">
          {{ report.negativeWalletCount }}
        </strong>
        <span class="kpi-sub">覆盖注册账户 {{ report.totalUsers }} 人</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">在途托管冻结失锚订单</span>
        <strong class="kpi-val" :class="report.escrowMismatchCount === 0 ? 'is-ok' : 'is-warn'">
          {{ report.escrowMismatchCount }}
        </strong>
        <span class="kpi-sub">累计完结担保单 {{ report.totalCompletedOrders }} 笔</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">高并发复合覆盖索引</span>
        <strong class="kpi-val is-ok">{{ report.compositeIndexOptimized ? '6/6 已就绪' : '待优化' }}</strong>
        <span class="kpi-sub">在架服务 {{ report.totalActiveServices }} · 拼班 {{ report.totalGroupSessions }} · 悬赏 {{ report.totalDemands }}</span>
      </div>
      <div class="kpi-card xyf-surface-card">
        <span class="kpi-label">待处置高危互刷 / 逾期预约</span>
        <strong class="kpi-val" :class="report.pendingHighRiskAlertCount + report.overdueUncompensatedOrderCount === 0 ? 'is-ok' : 'is-warn'">
          {{ report.pendingHighRiskAlertCount }} / {{ report.overdueUncompensatedOrderCount }}
        </strong>
        <span class="kpi-sub">巡检批次：{{ report.auditBatchNo }}</span>
      </div>
    </div>

    <div class="checks-panel xyf-surface-card" v-if="report">
      <div class="panel-head">
        <h3>6 项核心安全不变量与高并发架构自检清单</h3>
        <span class="inspect-time">最近巡检时间：{{ report.inspectTime }}</span>
      </div>

      <div class="check-list">
        <div
          v-for="item in report.securityChecks"
          :key="item.checkCode"
          class="check-row"
          :class="{ 'is-failed': !item.passed }"
        >
          <div class="check-row__status">
            <el-tag v-if="item.passed" type="success" effect="dark">PASS · 通过</el-tag>
            <el-tag v-else-if="item.severity === 'CRITICAL'" type="danger" effect="dark">CRITICAL · 告警</el-tag>
            <el-tag v-else type="warning" effect="dark">WARNING · 待关注</el-tag>
          </div>
          <div class="check-row__main">
            <div class="check-title">
              <strong>{{ item.checkTitle }}</strong>
              <code class="check-code">{{ item.checkCode }}</code>
            </div>
            <p class="check-detail">{{ item.detail }}</p>
            <p class="check-rec">架构建议：{{ item.recommendation }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { inspectSystemHealth, type SystemHealthAuditData } from '@/api/systemAudit'

const loading = ref(false)
const report = ref<SystemHealthAuditData | null>(null)

const levelClass = computed(() => {
  if (!report.value) return ''
  if (report.value.healthLevel === 'EXCELLENT') return 'is-excellent'
  if (report.value.healthLevel === 'WARNING') return 'is-warning'
  return 'is-critical'
})

const runInspection = async () => {
  loading.value = true
  try {
    report.value = await inspectSystemHealth()
    ElMessage.success(`全链路安全巡检完成，当前综合健康评分 ${report.value.overallHealthScore} 分`)
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
  gap: 18px;
}

.health-banner {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;

  .health-kicker {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: var(--xyf-primary);
  }

  h2 {
    margin: 4px 0 6px;
    font-size: 20px;
    font-weight: 700;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
    max-width: 680px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 14px;
  }
}

.score-badge {
  padding: 8px 16px;
  border-radius: 12px;
  display: flex;
  align-items: baseline;
  gap: 6px;

  &.is-excellent {
    background: #dcfce7;
    color: #15803d;
  }
  &.is-warning {
    background: #fef3c7;
    color: #b45309;
  }
  &.is-critical {
    background: #fee2e2;
    color: #b91c1c;
  }

  .score-num {
    font-size: 26px;
    font-weight: 800;
  }
  .score-unit {
    font-size: 12px;
    font-weight: 700;
  }
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .kpi-label {
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  .kpi-val {
    font-size: 24px;
    font-weight: 800;

    &.is-ok {
      color: #059669;
    }
    &.is-warn {
      color: #d97706;
    }
    &.is-err {
      color: #dc2626;
    }
  }

  .kpi-sub {
    font-size: 11.5px;
    color: var(--xyf-ink-3);
  }
}

.checks-panel {
  padding: 20px 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
  }

  .inspect-time {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.check-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.check-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;

  &.is-failed {
    background: #fffbeb;
    border-color: #fde68a;
  }

  &__status {
    flex-shrink: 0;
    padding-top: 2px;
  }

  &__main {
    flex: 1;
  }
}

.check-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;

  strong {
    font-size: 14px;
    color: #0f172a;
  }

  .check-code {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 4px;
    background: #e2e8f0;
    color: #475569;
  }
}

.check-detail {
  margin: 4px 0;
  font-size: 13px;
  color: #334155;
}

.check-rec {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}
</style>
