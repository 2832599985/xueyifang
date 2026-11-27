<template>
  <div class="about-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader title="平台规则">
        <template #actions>
          <el-button @click="router.push('/services')">进入服务大厅</el-button>
          <el-button type="primary" @click="router.push('/user/center')">个人中心</el-button>
        </template>
      </XyfPageHeader>

      <!-- Highlights -->
      <section class="highlights-grid">
        <XyfStatTile
          label="资金担保"
          value="4 步托管"
          hint="下单冻结、交付确认、验收放款"
        />
        <XyfStatTile
          label="校内身份"
          value="学号认证"
          hint="实名学号登记，发布权限人工复核"
        />
        <XyfStatTile
          label="争议保障"
          value="平台仲裁"
          hint="已付款、待收货与完结订单均可申诉"
          tone="gold"
        />
      </section>

      <!-- 4-Step Horizontal Escrow Flow Strip -->
      <section class="xyf-surface-card charter-card">
        <div class="xyf-section-heading">
          <h2>资金托管流程</h2>
        </div>

        <div class="flow-strip">
          <div v-for="(step, idx) in escrowSteps" :key="step.num" class="flow-node">
            <div class="flow-node__head">
              <span class="flow-num xyf-mono">{{ step.num }}</span>
              <span v-if="idx < escrowSteps.length - 1" class="flow-arrow" aria-hidden="true">→</span>
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </section>

      <!-- Three Platform Modes -->
      <section class="xyf-surface-card charter-card">
        <div class="xyf-section-heading">
          <h2>站内三种交易方式</h2>
        </div>

        <div class="modes-grid">
          <div class="mode-card" @click="router.push('/services')">
            <div class="mode-card__top">
              <strong>技能服务直购</strong>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <p>按标价直接下单，适合有明确交付内容与价格的常规辅导、设计、调试与摄影服务。</p>
          </div>

          <div class="mode-card" @click="router.push('/demands')">
            <div class="mode-card__top">
              <strong>悬赏定制竞标</strong>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <p>由需求方发布预算区间与验收要求，校内行家提交方案报价，选标后自动转入担保订单。</p>
          </div>

          <div class="mode-card" @click="router.push('/groups')">
            <div class="mode-card__top">
              <strong>拼班小班团课</strong>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <p>多人共同拼团开课，达到阶梯人数后按优惠价结算并自动退回已多付差价。</p>
          </div>
        </div>
      </section>

      <!-- Platform Redlines & FAQ Accordion -->
      <section class="charter-split">
        <div class="xyf-surface-card charter-card">
          <div class="xyf-section-heading">
            <h2>发布与交易红线</h2>
          </div>
          <ul class="charter-list">
            <li>首次发布服务前须在「个人中心」提交技能说明，审核通过后获得长期发布资格。</li>
            <li>严禁代考、代写毕业论文及任何违反学术诚信与法律法规的服务内容。</li>
            <li>线下交易建议优先选择图书馆、学生活动中心等公开校园地标，并通过动态码完成核销。</li>
            <li>服务信息修改后需重新接受平台审核，审核通过后恢复全校展示。</li>
          </ul>
        </div>

        <div class="xyf-surface-card charter-card">
          <div class="xyf-section-heading">
            <h2>常见问题</h2>
          </div>

          <el-collapse v-model="openFaqs" class="faq-collapse">
            <el-collapse-item title="付款后钱会直接打给卖家吗？" name="1">
              不会。付款后资金冻结在买方账户的担保冻结池中，直到买方确认验收或线下核销完成，资金才会结算入卖方钱包。
            </el-collapse-item>
            <el-collapse-item title="大额或长周期服务可以分阶段验收吗？" name="2">
              支持。订单支付后可在订单详情页将冻结资金拆分为 2 至 5 个阶段节点，每完成一个阶段即可单独验收放款。
            </el-collapse-item>
            <el-collapse-item title="线下约定爽约或产生交付争议怎么办？" name="3">
              超出预约时段卖方未履约时可直接在订单页触发爽约退赔；若存在质量或交付分歧，可上传沟通与凭证截图提交平台仲裁。
            </el-collapse-item>
          </el-collapse>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfStatTile from '@/components/ui/XyfStatTile.vue'

const router = useRouter()
const openFaqs = ref<string[]>(['1'])

const escrowSteps = [
  {
    num: '01',
    title: '下单并冻结资金',
    desc: '选定服务与交接方式并支付，款项进入担保冻结池并记录完整流水。'
  },
  {
    num: '02',
    title: '站内沟通与履约',
    desc: '通过站内消息确认需求细节，卖家完成线下会面或线上成果提交。'
  },
  {
    num: '03',
    title: '验收或核销结算',
    desc: '买家确认收货或现场动态码核销后，冻结资金自动结算入卖家钱包。'
  },
  {
    num: '04',
    title: '售后协商与仲裁',
    desc: '遇未履约或质量分歧时，可申请退款或提交证据由平台介入裁决。'
  }
]
</script>

<style scoped lang="scss">
.highlights-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.charter-card {
  padding: 24px;
  margin-bottom: 20px;
}

.flow-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.flow-node {
  padding: 16px;
  border-radius: var(--xyf-radius-sm);
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .flow-num {
    font-size: 12px;
    font-weight: 700;
    color: var(--xyf-ink);
    background: #ffffff;
    border: 1px solid var(--xyf-border);
    padding: 2px 8px;
    border-radius: var(--xyf-radius-xs);
  }

  .flow-arrow {
    color: var(--xyf-ink-4);
    font-size: 14px;
  }

  h3 {
    margin: 0 0 6px;
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: var(--xyf-ink-2);
  }
}

.modes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mode-card {
  padding: 18px;
  border-radius: var(--xyf-radius-sm);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  cursor: pointer;
  transition: border-color var(--xyf-transition), box-shadow var(--xyf-transition),
    transform var(--xyf-transition);

  &:hover {
    transform: translateY(-2px);
    border-color: var(--xyf-border-strong);
    box-shadow: var(--xyf-shadow-sm);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: var(--xyf-ink-2);
  }
}

.charter-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  .charter-card {
    margin-bottom: 0;
  }
}

.charter-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: var(--xyf-ink-2);
  font-size: 14px;
  line-height: 1.65;
}

.faq-collapse {
  border-top: none;
  border-bottom: none;

  :deep(.el-collapse-item__header) {
    font-size: 14px;
    font-weight: 500;
    color: var(--xyf-ink);
    border-bottom-color: var(--xyf-border-subtle);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom-color: var(--xyf-border-subtle);
  }

  :deep(.el-collapse-item__content) {
    font-size: 13px;
    line-height: 1.7;
    color: var(--xyf-ink-2);
    padding-bottom: 14px;
  }
}

@media (max-width: 960px) {
  .highlights-grid,
  .flow-strip,
  .modes-grid,
  .charter-split {
    grid-template-columns: 1fr;
  }
}
</style>
