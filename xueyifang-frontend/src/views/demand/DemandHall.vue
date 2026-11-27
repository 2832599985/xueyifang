<template>
  <div class="demand-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Editorial Market Header + Integrated Workspace Switcher -->
      <header class="bounty-header">
        <div class="bounty-header__left">
          <h1>悬赏大厅</h1>
        </div>

        <div class="bounty-header__right">
          <div v-if="userStore.userInfo && !userStore.isAdmin" class="xyf-segmented" role="tablist">
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'market' }"
              @click="switchWorkspace('market')"
            >
              <span>全部悬赏</span>
              <span class="xyf-segmented__count">{{ total }}</span>
            </button>
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'published' }"
              @click="switchWorkspace('published')"
            >
              <span>我发布的</span>
              <span v-if="myPublished.length" class="xyf-segmented__count">{{ myPublished.length }}</span>
            </button>
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'bids' }"
              @click="switchWorkspace('bids')"
            >
              <span>我竞标的</span>
              <span v-if="myBids.length" class="xyf-segmented__count">{{ myBids.length }}</span>
            </button>
          </div>

          <el-button type="primary" size="large" @click="openPublishDialog">
            <el-icon><Plus /></el-icon>
            <span>发布悬赏</span>
          </el-button>
        </div>
      </header>

      <!-- VIEW 1: Public Bounty Marketplace -->
      <template v-if="activeWorkspace === 'market'">
        <!-- Borderless Two-Tier Editorial Filter Studio -->
        <section class="filter-studio">
          <!-- Tier 1: Category Underline Tabs + Campus Switcher -->
          <div class="filter-studio__top">
            <div class="xyf-tabs">
              <button
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': !filters.tagId }"
                @click="selectTag(undefined)"
              >
                全部领域
              </button>
              <button
                v-for="t in dictStore.serviceTags || []"
                :key="t.tagId"
                type="button"
                class="xyf-tab"
                :class="{ 'is-active': filters.tagId === t.tagId }"
                @click="selectTag(t.tagId)"
              >
                {{ t.tagName }}
              </button>
            </div>

            <div class="campus-pills">
              <button
                type="button"
                class="campus-pill"
                :class="{ 'is-active': !filters.campusId }"
                @click="selectCampus(undefined)"
              >
                全部校区
              </button>
              <button
                v-for="c in campuses"
                :key="c.id"
                type="button"
                class="campus-pill"
                :class="{ 'is-active': filters.campusId === c.id }"
                @click="selectCampus(c.id)"
              >
                {{ c.campusName }}
              </button>
            </div>
          </div>

          <!-- Tier 2: Urgency Pills + Mode Pills + Search Box -->
          <div class="filter-studio__bottom">
            <div class="pill-cluster">
              <div class="xyf-segmented">
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': !filters.urgencyLevel }"
                  @click="selectUrgency(undefined)"
                >
                  全部优先级
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 3 }"
                  @click="selectUrgency(3)"
                >
                  十万火急
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 2 }"
                  @click="selectUrgency(2)"
                >
                  加急优先
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 1 }"
                  @click="selectUrgency(1)"
                >
                  常规需求
                </button>
              </div>

              <div class="xyf-segmented">
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': !filters.tradeType }"
                  @click="selectTradeType(undefined)"
                >
                  全部交付
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.tradeType === 1 }"
                  @click="selectTradeType(1)"
                >
                  校内当面
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.tradeType === 2 }"
                  @click="selectTradeType(2)"
                >
                  线上远程
                </button>
              </div>
            </div>

            <div class="search-inline">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索关键词（如 高数积分 / STM32 / 跟拍）..."
                clearable
                :prefix-icon="Search"
                @keyup.enter="reload"
                @clear="reload"
              />
              <button
                v-if="hasActiveFilters"
                type="button"
                class="reset-link"
                @click="resetFilters"
              >
                重置
              </button>
            </div>
          </div>
        </section>

        <!-- Loading Skeleton -->
        <div v-if="loading" class="ticket-grid">
          <div v-for="n in 6" :key="n" class="xyf-surface-card skeleton-ticket">
            <el-skeleton :rows="4" animated />
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="demands.length === 0" class="xyf-surface-card empty-wrap">
          <XyfEmptyState
            title="当前筛选下暂无招募悬赏"
            description="你可以清空筛选条件，或直接发布一条定制悬赏让本校认证创作者主动报价。"
          >
            <template #action>
              <el-button v-if="hasActiveFilters" plain @click="resetFilters">清空筛选</el-button>
              <el-button type="primary" @click="openPublishDialog">立即发布悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <!-- Commercial Escrow Bounty Ticket Grid -->
        <div v-else class="ticket-grid">
          <article
            v-for="item in demands"
            :key="item.id"
            class="bounty-ticket"
            :class="{ 'is-urgent': item.urgencyLevel === 3 }"
            @click="router.push(`/demands/${item.id}`)"
          >
            <!-- Top Meta Row -->
            <div class="bounty-ticket__head">
              <div class="bounty-tags">
                <span class="urgency-chip" :class="`is-u${item.urgencyLevel}`">
                  <span class="urgency-chip__dot" />
                  {{ urgencyLabel(item.urgencyLevel) }}
                </span>
                <span v-if="item.tagName" class="meta-chip">{{ item.tagName }}</span>
                <span class="meta-chip">{{ item.campusName || '主校区' }}</span>
              </div>

              <span class="state-pill" :class="{ 'is-open': item.demandStatus === 0 }">
                {{ item.demandStatus === 0 ? '招募竞标中' : '已选标成单' }}
              </span>
            </div>

            <!-- Main Content -->
            <h3 class="bounty-ticket__title ellipsis-2">{{ item.title }}</h3>
            <p class="bounty-ticket__desc ellipsis-2">{{ item.description }}</p>

            <!-- Fulfillment & Geofence Strip -->
            <div class="fulfill-strip">
              <div class="fulfill-strip__item">
                <el-icon><Location v-if="item.tradeType === 1" /><Monitor v-else /></el-icon>
                <span v-if="item.tradeType === 1" class="ellipsis">
                  {{ item.meetupPoiName || '校内指定地标' }} · {{ item.meetupRadiusMeters || 80 }}m 围栏核销
                </span>
                <span v-else>线上交付凭证验收 · 担保结算</span>
              </div>
              <span v-if="item.deadlineTime" class="deadline-chip xyf-tabular">
                截止 {{ formatShortDeadline(item.deadlineTime) }}
              </span>
            </div>

            <!-- Perforated Ticket Stub Footer -->
            <div class="bounty-ticket__stub">
              <div class="publisher-mini">
                <XyfAvatar
                  :src="item.publisherAvatar"
                  :name="item.publisherName || '校内同学'"
                  :size="28"
                />
                <div class="publisher-mini__info">
                  <span class="publisher-mini__name ellipsis">{{ item.publisherName || '校内实名同学' }}</span>
                  <span class="publisher-mini__trust xyf-tabular">
                    {{ item.publisherEduVerified === 1 ? 'edu.cn 已核验' : '实名学号' }} · 信用 {{ item.publisherCreditScore ?? 100 }}
                  </span>
                </div>
              </div>

              <div class="budget-callout xyf-tabular">
                <div class="budget-callout__amount">
                  <span class="yen">¥</span>{{ Number(item.budgetMin).toFixed(0) }}<span class="dash">–</span>{{ Number(item.budgetMax).toFixed(0) }}
                </div>
                <span class="budget-callout__bids">
                  {{ item.bidCount || 0 }} 位创作者报价 →
                </span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="total > pageSize" class="pager-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="fetchList"
          />
        </div>
      </template>

      <!-- VIEW 2: My Published Demands Workspace -->
      <section v-else-if="activeWorkspace === 'published'" class="workspace-panel">
        <div v-if="myPublished.length === 0" class="xyf-surface-card empty-wrap">
          <XyfEmptyState
            title="你还没有发布过校内悬赏"
            description="遇到期末难点、硬件调试或设计拍摄需求时，发布悬赏即可收集多位认证创作者的定制方案。"
          >
            <template #action>
              <el-button type="primary" @click="openPublishDialog">发布首条悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="my-records-grid">
          <div
            v-for="d in myPublished"
            :key="d.id"
            class="xyf-surface-card record-card"
            @click="router.push(`/demands/${d.id}`)"
          >
            <div class="record-card__top">
              <span class="xyf-mono record-no">{{ d.demandNo }}</span>
              <el-tag size="small" :type="d.demandStatus === 0 ? 'success' : d.demandStatus === 1 ? 'warning' : 'info'">
                {{ statusText(d.demandStatus) }}
              </el-tag>
            </div>
            <h3>{{ d.title }}</h3>
            <div class="record-card__foot xyf-tabular">
              <span>预算 ¥{{ d.budgetMin }} – ¥{{ d.budgetMax }}</span>
              <strong>已收到 {{ d.bidCount || 0 }} 份竞标方案 →</strong>
            </div>
          </div>
        </div>
      </section>

      <!-- VIEW 3: My Submitted Bids Workspace -->
      <section v-else class="workspace-panel">
        <div v-if="myBids.length === 0" class="xyf-surface-card empty-wrap">
          <XyfEmptyState
            title="暂无参与的悬赏竞标记录"
            description="前往全校招募广场挑选擅长的悬赏任务提交报价，中标后将自动为您生成平台担保销售单。"
          >
            <template #action>
              <el-button type="primary" @click="switchWorkspace('market')">浏览招募中悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="my-records-grid">
          <div
            v-for="b in myBids"
            :key="b.id"
            class="xyf-surface-card record-card"
            @click="router.push(`/demands/${b.demandId}`)"
          >
            <div class="record-card__top">
              <span class="xyf-tabular">承诺 {{ b.estimatedHours }} 小时内交付</span>
              <el-tag
                size="small"
                :type="b.bidStatus === 1 ? 'success' : b.bidStatus === 0 ? 'warning' : 'info'"
              >
                {{ b.bidStatus === 1 ? '已中标转担保单' : b.bidStatus === 0 ? '待需求方选标' : '未中标' }}
              </el-tag>
            </div>
            <h3>{{ b.demandTitle || `校内悬赏 #${b.demandId}` }}</h3>
            <p class="record-plan ellipsis-2">{{ b.bidPlan }}</p>
            <div class="record-card__foot xyf-tabular">
              <span>我的报价 <strong>¥{{ Number(b.bidPrice).toFixed(2) }}</strong></span>
              <span>查看悬赏详情 →</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Guided 3-Step Commercial Bounty Composer Dialog -->
    <el-dialog
      v-model="publishVisible"
      title="发布校内反向悬赏需求"
      width="720px"
      :close-on-click-modal="false"
    >
      <!-- Step Indicator -->
      <div class="composer-steps">
        <button
          v-for="(s, idx) in composerSteps"
          :key="s.label"
          type="button"
          class="composer-step"
          :class="{ 'is-active': pubStep === idx, 'is-done': pubStep > idx }"
          @click="pubStep = idx"
        >
          <span class="composer-step__num xyf-tabular">0{{ idx + 1 }}</span>
          <span class="composer-step__txt">{{ s.label }}</span>
        </button>
      </div>

      <!-- Quick Campus Scene Templates -->
      <div v-if="pubStep === 0" class="scene-templates">
        <span class="scene-templates__lbl">快捷套用高频场景：</span>
        <button
          v-for="tpl in scenePresets"
          :key="tpl.name"
          type="button"
          class="scene-pill"
          @click="applyScenePreset(tpl)"
        >
          {{ tpl.name }}
        </button>
      </div>

      <el-form label-position="top" class="composer-form">
        <!-- STEP 1: Scope & Criteria -->
        <div v-show="pubStep === 0">
          <el-form-item label="悬赏标题（清晰概括科目/技术栈与期望成果，4~120字）" required>
            <el-input
              v-model="pubForm.title"
              maxlength="120"
              show-word-limit
              placeholder="例如：周四晚南湖图书馆高数下册曲线曲面积分 2 小时一对一突击"
            />
          </el-form-item>

          <div class="row-2">
            <el-form-item label="所属校区" required>
              <el-select v-model="pubForm.campusId" style="width: 100%">
                <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="技能分类">
              <el-select v-model="pubForm.tagId" placeholder="选择关联分类" style="width: 100%">
                <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="详细需求背景与验收标准" required>
            <el-input
              v-model="pubForm.description"
              type="textarea"
              :rows="5"
              maxlength="800"
              show-word-limit
              placeholder="请说明：1. 当前进度或遇到的问题；2. 需要创作者具备哪些经验；3. 明确的验收交付物或时长要求..."
            />
          </el-form-item>
        </div>

        <!-- STEP 2: Budget & Urgency -->
        <div v-show="pubStep === 1">
          <div class="row-2">
            <el-form-item label="预算下限（元）" required>
              <el-input-number v-model="pubForm.budgetMin" :min="1" :max="9999" :step="10" style="width: 100%" />
            </el-form-item>
            <el-form-item label="预算上限（元）" required>
              <el-input-number v-model="pubForm.budgetMax" :min="1" :max="9999" :step="10" style="width: 100%" />
            </el-form-item>
          </div>

          <el-form-item label="需求紧急度">
            <div class="urgency-selector">
              <button
                v-for="u in urgencyChoices"
                :key="u.value"
                type="button"
                class="urgency-choice"
                :class="{ 'is-selected': pubForm.urgencyLevel === u.value }"
                @click="pubForm.urgencyLevel = u.value"
              >
                <strong>{{ u.title }}</strong>
                <span>{{ u.desc }}</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item label="期望最晚完成时间（选填）">
            <el-date-picker
              v-model="pubForm.deadlineTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择期望截止时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <!-- STEP 3: Fulfillment Mode & Campus Map Geofence -->
        <div v-show="pubStep === 2">
          <el-form-item label="交付与验收方式" required>
            <div class="mode-selector">
              <button
                type="button"
                class="mode-choice"
                :class="{ 'is-selected': pubForm.tradeType === 1 }"
                @click="pubForm.tradeType = 1"
              >
                <strong>校园线下当面履约</strong>
                <span>绑定真实校园地图坐标与米级电子围栏，当面动态码/蓝牙核销</span>
              </button>
              <button
                type="button"
                class="mode-choice"
                :class="{ 'is-selected': pubForm.tradeType === 2 }"
                @click="pubForm.tradeType = 2"
              >
                <strong>线上远程数字交付</strong>
                <span>创作者站内上传成果凭证，支持拆解多阶段里程碑验收结算</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item
            v-if="pubForm.tradeType === 1"
            label="在真实校园地图标定会面地标与核销有效半径"
            required
          >
            <CampusMapPicker
              :campus-id="pubForm.campusId || 1"
              v-model="pubMapSpot"
              style="width: 100%"
              @fence-change="ok => (pubFenceValid = ok)"
            />
          </el-form-item>

          <!-- Live Ticket Preview Summary -->
          <div class="composer-preview xyf-tabular">
            <div>
              <span class="composer-preview__kicker">即将发布的悬赏预览</span>
              <strong>{{ pubForm.title || '未填写悬赏标题' }}</strong>
            </div>
            <div class="composer-preview__price">
              预算 ¥{{ pubForm.budgetMin }} – ¥{{ pubForm.budgetMax }}
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-step-footer">
          <el-button @click="publishVisible = false">取消</el-button>
          <div class="dialog-step-footer__right">
            <el-button v-if="pubStep > 0" plain @click="pubStep--">上一步</el-button>
            <el-button v-if="pubStep < 2" type="primary" @click="nextComposerStep">下一步</el-button>
            <el-button
              v-else
              type="primary"
              :loading="publishing"
              :disabled="pubForm.tradeType === 1 && !pubFenceValid"
              @click="submitPublish"
            >
              确认发布悬赏
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Location, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { listCampuses } from '@/api/campus'
import {
  listDemands,
  publishDemand,
  listMyPublishedDemands,
  listMyDemandBids
} from '@/api/demand'
import { notifyFallbackError } from '@/api/request'
import type { CampusInfo } from '@/types/dict'
import type { DemandItem, DemandBidItem } from '@/types/demand'
import CampusMapPicker, { type MapLocationValue } from '@/components/business/CampusMapPicker.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dictStore = useDictStore()

const activeWorkspace = ref<'market' | 'published' | 'bids'>('market')
const loading = ref(false)
const demands = ref<DemandItem[]>([])
const campuses = ref<CampusInfo[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filters = reactive({
  campusId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  urgencyLevel: undefined as number | undefined,
  tradeType: undefined as number | undefined,
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : ''
})

const publishVisible = ref(false)
const publishing = ref(false)
const pubStep = ref(0)
const pubFenceValid = ref(true)

const composerSteps = [
  { label: '需求场景与标准' },
  { label: '预算区间与优先级' },
  { label: '交接围栏与发布' }
]

const urgencyChoices = [
  { value: 1, title: '常规需求', desc: '按发布时间正常展示与撮合' },
  { value: 2, title: '加急优先', desc: '优先推荐给同院系活跃创作者' },
  { value: 3, title: '十万火急', desc: '醒目急单标识，适合24h内急需场景' }
]

const scenePresets = [
  {
    name: '考前 1v1 突击',
    title: '高数/线代核心大题 2 小时一对一考前突击串讲',
    desc: '希望熟悉本校期末历年真题题型，当面梳理重点解题步骤并答疑，时长约 2 小时。',
    min: 60,
    max: 120,
    tradeType: 1,
    urgency: 3
  },
  {
    name: '嵌入式/代码联调',
    title: 'STM32 传感器串口通信与上位机联调排错指导',
    desc: '硬件接线已完成，目前串口数据偶发丢包，需有嵌入式经验的同学协助定位并跑通演示。',
    min: 80,
    max: 180,
    tradeType: 1,
    urgency: 2
  },
  {
    name: '答辩路演 PPT 精修',
    title: '大创结题路演 18 页 PPT 视觉排版重塑与图表美化',
    desc: '文字初稿与数据已备齐，需要极简商业学术风排版，交付可编辑源文件。',
    min: 70,
    max: 150,
    tradeType: 2,
    urgency: 2
  },
  {
    name: '校园活动约拍跟拍',
    title: '周末校内毕业/社团活动 1.5 小时胶片感人像跟拍',
    desc: '自带微单或单反设备，提供全部底片及 9 张精修调色成片。',
    min: 90,
    max: 180,
    tradeType: 1,
    urgency: 1
  }
]

const pubForm = reactive({
  campusId: 1,
  tagId: undefined as number | undefined,
  title: '',
  description: '',
  budgetMin: 50,
  budgetMax: 120,
  tradeType: 1,
  urgencyLevel: 2,
  deadlineTime: ''
})

const pubMapSpot = ref<MapLocationValue>({
  latitude: undefined,
  longitude: undefined,
  poiName: '',
  detailAddress: '',
  radiusMeters: 80
})

const myPublished = ref<DemandItem[]>([])
const myBids = ref<DemandBidItem[]>([])

const hasActiveFilters = computed(() =>
  Boolean(
    filters.campusId ||
      filters.tagId ||
      filters.urgencyLevel ||
      filters.tradeType ||
      filters.keyword.trim()
  )
)

function urgencyLabel(level: number) {
  if (level === 3) return '十万火急'
  if (level === 2) return '加急'
  return '常规'
}

function statusText(st: number) {
  if (st === 0) return '招募竞标中'
  if (st === 1) return '已选标转单'
  if (st === 2) return '已撤回'
  return '已关闭'
}

function formatShortDeadline(dt: string) {
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return dt.slice(0, 10)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function applyScenePreset(tpl: (typeof scenePresets)[number]) {
  pubForm.title = tpl.title
  pubForm.description = tpl.desc
  pubForm.budgetMin = tpl.min
  pubForm.budgetMax = tpl.max
  pubForm.tradeType = tpl.tradeType
  pubForm.urgencyLevel = tpl.urgency
}

function nextComposerStep() {
  if (pubStep.value === 0) {
    if (!pubForm.title.trim() || pubForm.title.trim().length < 4) {
      ElMessage.warning('请先填写至少 4 个字的悬赏标题')
      return
    }
    if (!pubForm.description.trim()) {
      ElMessage.warning('请填写详细需求背景与验收标准')
      return
    }
  }
  if (pubStep.value === 1 && pubForm.budgetMax < pubForm.budgetMin) {
    ElMessage.warning('预算上限不能低于预算下限')
    return
  }
  pubStep.value++
}

function selectTag(id?: number) {
  filters.tagId = id
  reload()
}

function selectCampus(id?: number) {
  filters.campusId = id
  reload()
}

function selectUrgency(level?: number) {
  filters.urgencyLevel = level
  reload()
}

function selectTradeType(type?: number) {
  filters.tradeType = type
  reload()
}

function resetFilters() {
  filters.campusId = undefined
  filters.tagId = undefined
  filters.urgencyLevel = undefined
  filters.tradeType = undefined
  filters.keyword = ''
  reload()
}

function reload() {
  pageNum.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listDemands({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      campusId: filters.campusId,
      tagId: filters.tagId,
      urgencyLevel: filters.urgencyLevel,
      tradeType: filters.tradeType,
      keyword: filters.keyword.trim() || undefined
    })
    demands.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (err) {
    notifyFallbackError(err, '加载悬赏大厅失败')
  } finally {
    loading.value = false
  }
}

async function loadMyWorkspace() {
  if (!userStore.userInfo || userStore.isAdmin) return
  const [pub, bids] = await Promise.all([
    listMyPublishedDemands().catch(() => []),
    listMyDemandBids().catch(() => [])
  ])
  myPublished.value = pub
  myBids.value = bids
}

async function switchWorkspace(tab: 'market' | 'published' | 'bids') {
  activeWorkspace.value = tab
  if (tab !== 'market') {
    await loadMyWorkspace()
  }
}

function openPublishDialog() {
  if (!userStore.userInfo) {
    router.push({ path: '/login', query: { redirect: '/demands?compose=1' } })
    return
  }
  if (userStore.isAdmin) {
    ElMessage.info('管理员账号请前往后台巡检模块管理悬赏')
    return
  }
  pubStep.value = 0
  pubForm.campusId = userStore.userInfo.campusId || campuses.value[0]?.id || 1
  pubForm.tagId = dictStore.serviceTags?.[0]?.tagId
  publishVisible.value = true
}

async function submitPublish() {
  if (!pubForm.title.trim() || pubForm.title.trim().length < 4) {
    pubStep.value = 0
    ElMessage.warning('悬赏标题至少填写 4 个字')
    return
  }
  if (!pubForm.description.trim()) {
    pubStep.value = 0
    ElMessage.warning('请填写详细需求说明')
    return
  }
  if (pubForm.budgetMax < pubForm.budgetMin) {
    pubStep.value = 1
    ElMessage.warning('预算上限不能低于预算下限')
    return
  }
  if (pubForm.tradeType === 1 && (!pubMapSpot.value.latitude || !pubMapSpot.value.poiName.trim())) {
    ElMessage.warning('请在校园地图上点击标记会面地标')
    return
  }

  publishing.value = true
  try {
    const id = await publishDemand({
      campusId: pubForm.campusId,
      tagId: pubForm.tagId,
      title: pubForm.title.trim(),
      description: pubForm.description.trim(),
      budgetMin: pubForm.budgetMin,
      budgetMax: pubForm.budgetMax,
      tradeType: pubForm.tradeType,
      urgencyLevel: pubForm.urgencyLevel,
      deadlineTime: pubForm.deadlineTime || undefined,
      meetupLat: pubForm.tradeType === 1 ? pubMapSpot.value.latitude : undefined,
      meetupLng: pubForm.tradeType === 1 ? pubMapSpot.value.longitude : undefined,
      meetupPoiName: pubForm.tradeType === 1 ? pubMapSpot.value.poiName.trim() : undefined,
      meetupAddress: pubForm.tradeType === 1 ? pubMapSpot.value.detailAddress?.trim() : undefined,
      meetupRadiusMeters: pubForm.tradeType === 1 ? pubMapSpot.value.radiusMeters || 80 : undefined
    })
    ElMessage.success('悬赏需求已发布，正在向本校认证创作者开放报价')
    publishVisible.value = false
    router.push(`/demands/${id}`)
  } catch (err) {
    notifyFallbackError(err, '发布悬赏失败')
  } finally {
    publishing.value = false
  }
}

watch(
  () => route.query,
  (q) => {
    if (typeof q.keyword === 'string' && q.keyword !== filters.keyword) {
      filters.keyword = q.keyword
      reload()
    }
    if (q.view === 'mine') {
      switchWorkspace('published')
    }
    if (q.compose === '1') {
      openPublishDialog()
    }
  }
)

onMounted(async () => {
  await Promise.all([
    listCampuses().then(res => (campuses.value = res)).catch(() => {}),
    dictStore.fetchServiceTags().catch(() => {}),
    fetchList(),
    loadMyWorkspace()
  ])
  if (route.query.view === 'mine') {
    activeWorkspace.value = 'published'
  }
  if (route.query.compose === '1') {
    openPublishDialog()
  }
})
</script>

<style scoped lang="scss">
.bounty-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 22px;

  &__left {
    max-width: 640px;

    h1 {
      margin: 6px 0 8px;
      font-size: 32px;
      font-weight: 700;
      letter-spacing: -0.03em;
      color: var(--xyf-ink);
    }

    p {
      margin: 0;
      font-size: 14.5px;
      color: var(--xyf-ink-2);
      line-height: 1.55;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

/* Live Market Pulse Strip */
.pulse-strip {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  padding: 14px 22px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  margin-bottom: 24px;
}

.pulse-metric {
  display: flex;
  align-items: baseline;
  gap: 10px;

  &__label {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    font-size: 12.5px;
    color: var(--xyf-ink-2);
  }

  strong {
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.pulse-divider {
  width: 1px;
  height: 18px;
  background: var(--xyf-border-strong);

  @media (max-width: 768px) {
    display: none;
  }
}

.pulse-guarantee {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--xyf-ink-3);

  @media (max-width: 960px) {
    margin-left: 0;
  }
}

/* Editorial Filter Studio */
.filter-studio {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    border-bottom: 1px solid var(--xyf-border);

    .xyf-tabs {
      border-bottom: none;
      flex: 1;
      min-width: 240px;
    }
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.campus-pills {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 8px;
  flex-wrap: wrap;
}

.campus-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink-2);
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    color: var(--xyf-ink);
  }

  &.is-active {
    background: var(--xyf-ink);
    border-color: var(--xyf-ink);
    color: #ffffff;
  }
}

.pill-cluster {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 280px;
  flex: 1;
  max-width: 360px;

  .reset-link {
    border: none;
    background: transparent;
    color: var(--xyf-ink-2);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
      color: var(--xyf-ink);
      text-decoration: underline;
    }
  }
}

/* Escrow Bounty Ticket Grid */
.ticket-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.skeleton-ticket {
  padding: 24px;
}

.empty-wrap {
  padding: 52px 24px;
}

.bounty-ticket {
  position: relative;
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 18px;
  padding: 22px 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  transition: transform var(--xyf-transition), box-shadow var(--xyf-transition), border-color var(--xyf-transition);

  &:hover {
    transform: translateY(-3px);
    border-color: #18181b;
    box-shadow: var(--xyf-shadow-md);
  }

  &.is-urgent {
    border-color: #ebd0c7;
    background: linear-gradient(180deg, #fffcfb 0%, #ffffff 36%);

    &:hover {
      border-color: #18181b;
    }
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    line-height: 1.42;
    letter-spacing: -0.015em;
    color: var(--xyf-ink);
  }

  &__desc {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.58;
    color: var(--xyf-ink-2);
    min-height: 42px;
  }

  &__stub {
    margin-top: auto;
    padding-top: 14px;
    border-top: 1px dashed var(--xyf-border-strong);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
}

.bounty-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.urgency-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #71717a;
  }

  &.is-u3 {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #f7cece;
    .urgency-chip__dot {
      background: #dc2626;
    }
  }

  &.is-u2 {
    background: var(--xyf-champagne-soft);
    color: #9a5b13;
    border: 1px solid var(--xyf-champagne-border);
    .urgency-chip__dot {
      background: #d97706;
    }
  }
}

.meta-chip {
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  color: var(--xyf-ink-2);
}

.state-pill {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-3);
  white-space: nowrap;

  &.is-open {
    background: #18181b;
    color: #ffffff;
  }
}

.fulfill-strip {
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--xyf-ink-2);

  &__item {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  .deadline-chip {
    font-size: 11.5px;
    color: #9a5b13;
    font-weight: 600;
    white-space: nowrap;
  }
}

.publisher-mini {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;

  &__info {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  &__trust {
    font-size: 11px;
    color: var(--xyf-ink-3);
  }
}

.budget-callout {
  text-align: right;
  flex-shrink: 0;

  &__amount {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--xyf-ink);
    line-height: 1.1;

    .yen {
      font-size: 13px;
      margin-right: 1px;
    }

    .dash {
      margin: 0 2px;
      font-weight: 500;
      color: var(--xyf-ink-3);
    }
  }

  &__bids {
    display: block;
    margin-top: 3px;
    font-size: 11.5px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }
}

/* Workspace Cards (My Published / My Bids) */
.my-records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.record-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-ink);
    transform: translateY(-2px);
    box-shadow: var(--xyf-shadow-sm);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: var(--xyf-ink-3);
  }

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  .record-plan {
    margin: 0;
    font-size: 13px;
    color: var(--xyf-ink-2);
  }

  &__foot {
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid var(--xyf-border-subtle);
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    color: var(--xyf-ink-2);

    strong {
      color: var(--xyf-ink);
    }
  }
}

.pager-wrap {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

/* 3-Step Guided Composer */
.composer-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 18px;
}

.composer-step {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-family: inherit;
  transition: all var(--xyf-transition);

  &__num {
    font-size: 12px;
    font-weight: 700;
    color: var(--xyf-ink-3);
  }

  &__txt {
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink-2);
  }

  &.is-active {
    background: #18181b;
    border-color: #18181b;

    .composer-step__num {
      color: var(--xyf-champagne-metallic);
    }
    .composer-step__txt {
      color: #ffffff;
    }
  }

  &.is-done {
    border-color: var(--xyf-border-strong);
    background: #ffffff;
    .composer-step__txt {
      color: var(--xyf-ink);
    }
  }
}

.scene-templates {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--xyf-surface-soft);

  &__lbl {
    font-size: 12px;
    color: var(--xyf-ink-3);
    font-weight: 500;
  }
}

.scene-pill {
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

.row-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.urgency-selector,
.mode-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  width: 100%;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.mode-selector {
  grid-template-columns: repeat(2, 1fr);
}

.urgency-choice,
.mode-choice {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all var(--xyf-transition);

  strong {
    font-size: 13.5px;
    color: var(--xyf-ink);
  }

  span {
    font-size: 12px;
    color: var(--xyf-ink-3);
    line-height: 1.45;
  }

  &.is-selected {
    border-color: #18181b;
    box-shadow: 0 0 0 1px #18181b inset;
    background: var(--xyf-surface-soft);
  }
}

.composer-preview {
  margin-top: 8px;
  padding: 14px 18px;
  border-radius: 12px;
  background: #18181b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  &__kicker {
    display: block;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 2px;
  }

  strong {
    font-size: 14px;
  }

  &__price {
    font-size: 16px;
    font-weight: 700;
    color: var(--xyf-champagne-metallic);
    white-space: nowrap;
  }
}

.dialog-step-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__right {
    display: flex;
    gap: 8px;
  }
}
</style>
