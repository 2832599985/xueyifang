<template>
  <div class="about-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="交易保障与平台规则"
        subtitle="清晰的资金托管与实名履约机制，让每一次校园协作都安心可靠。"
      >
        <template #actions>
          <el-button @click="router.push('/services')">进入服务大厅</el-button>
          <el-button type="primary" @click="router.push('/user/center')">个人中心</el-button>
        </template>
      </XyfPageHeader>

      <!-- Highlights -->
      <section class="highlights-grid">
        <XyfStatTile
          label="交易流程"
          value="4 步托管"
          hint="创建订单 → 付款冻结 → 履约发货 → 确认结算"
          tone="teal"
        />
        <XyfStatTile
          label="校内身份"
          value="学号认证"
          hint="绑定校内学号与专业信息，服务者经人工复核"
          tone="sage"
        />
        <XyfStatTile
          label="纠纷保障"
          value="平台仲裁"
          hint="支持已付款、已发货及已完成订单的争议申诉"
          tone="amber"
        />
      </section>

      <!-- Escrow Lifecycle -->
      <section class="xyf-surface-card charter-card">
        <div class="xyf-section-heading">
          <h2>一、资金托管与订单生命周期</h2>
        </div>

        <div class="rules-grid">
          <div class="rule-block">
            <span class="rule-num xyf-mono">01</span>
            <h3>下单与冻结资金</h3>
            <p>
              买家选定技能服务及交易方式（线下交易需选定校内约定地点）提交订单，支付时平台仅冻结账户余额并记录流水，资金暂不直接进入卖家可用余额。
            </p>
          </div>

          <div class="rule-block">
            <span class="rule-num xyf-mono">02</span>
            <h3>沟通交付与卖家发货</h3>
            <p>
              双方可通过订单详情页一键发起站内实时聊天确认细节。卖家完成线下会面或线上交付物提交后，点击“确认发货”进入待买家验收状态。
            </p>
          </div>

          <div class="rule-block">
            <span class="rule-num xyf-mono">03</span>
            <h3>确认收货与资金结算</h3>
            <p>
              买家验收无误后点击“确认收货”，系统自动将冻结款项解冻并转入卖家可用钱包余额，随后开放买家发表客观服务评价。
            </p>
          </div>

          <div class="rule-block">
            <span class="rule-num xyf-mono">04</span>
            <h3>退款协商与平台仲裁</h3>
            <p>
              待发货状态下买家可申请退款并由卖家处理；若出现未履约、货不对板或质量争议，买家或卖家均可在订单详情中提交纠纷单，由平台管理员核查仲裁。
            </p>
          </div>
        </div>
      </section>

      <!-- Platform Conventions -->
      <section class="charter-split">
        <div class="xyf-surface-card charter-card">
          <div class="xyf-section-heading">
            <h2>二、服务发布规范</h2>
          </div>
          <ul class="charter-list">
            <li>首次发布服务前需在「个人中心」提交个人技能特长申请，审核通过后即获长期发布资格。</li>
            <li>服务标题、描述与展示图片须真实反映个人能力范围与交付内容，严禁代考、代写毕业论文等学术不端行为。</li>
            <li>单次服务需明确标注定价单位与库存数量；如选线下交易，建议优先在图书馆、学生活动中心等公开校园地标进行。</li>
            <li>服务一经修改需重新接受平台审核，审核通过后自动恢复面向全校检索展示。</li>
          </ul>
        </div>

        <div class="xyf-surface-card charter-card">
          <div class="xyf-section-heading">
            <h2>三、常见快捷入口</h2>
          </div>
          <div class="quick-links">
            <button type="button" class="quick-link-card" @click="router.push('/services')">
              <div>
                <strong>浏览技能大厅</strong>
                <p>按标签、交易方式与价格排序查找同学提供的服务</p>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>

            <button type="button" class="quick-link-card" @click="router.push('/orders')">
              <div>
                <strong>查看买方订单</strong>
                <p>跟踪订单支付、发货、退款申请与评价状态</p>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>

            <button type="button" class="quick-link-card" @click="router.push('/wallet')">
              <div>
                <strong>钱包与资金流水</strong>
                <p>查看可用余额、冻结担保金额及每一笔历史收支明细</p>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>

            <button type="button" class="quick-link-card" @click="router.push('/disputes')">
              <div>
                <strong>我的纠纷记录</strong>
                <p>追踪已提交仲裁单的处理进度与管理员裁决回复</p>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'

const router = useRouter()
</script>

<style scoped lang="scss">
.highlights-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.charter-card {
  padding: 24px;
  margin-bottom: 20px;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.rule-block {
  padding: 18px;
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-md);

  .rule-num {
    display: inline-block;
    font-size: 11px;
    font-weight: 700;
    color: var(--xyf-primary-dark);
    background: var(--xyf-primary-soft);
    padding: 2px 8px;
    border-radius: var(--xyf-radius-pill);
    margin-bottom: 8px;
  }

  h3 {
    margin: 0 0 6px;
    font-size: 15.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.68;
    color: var(--xyf-ink-secondary);
  }
}

.charter-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.charter-list {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: var(--xyf-ink-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-link-card {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 16px;
  background: var(--xyf-bg);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-sm);
  cursor: pointer;
  text-align: left;
  transition: all var(--xyf-transition);

  strong {
    display: block;
    font-size: 14px;
    color: var(--xyf-ink);
    margin-bottom: 2px;
  }

  p {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }

  .el-icon {
    color: var(--xyf-ink-muted);
    flex-shrink: 0;
  }

  &:hover {
    border-color: var(--xyf-primary);
    background: var(--xyf-primary-soft);

    .el-icon {
      color: var(--xyf-primary);
      transform: translateX(3px);
    }
  }
}

@media (max-width: 900px) {
  .highlights-grid,
  .rules-grid,
  .charter-split {
    grid-template-columns: 1fr;
  }
}
</style>
