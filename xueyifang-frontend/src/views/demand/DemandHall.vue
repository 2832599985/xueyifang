<template>
  <div class="demand-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Minimalist Page Header -->
      <header class="page-head">
        <div class="page-head__title-wrap">
          <h1>悬赏大厅</h1>
          <span v-if="activeWorkspace === 'market' && !loading" class="page-head__count xyf-tabular">
            {{ total }} 条需求
          </span>
        </div>

        <div class="page-head__actions">
          <div v-if="userStore.userInfo && !userStore.isAdmin" class="xyf-segmented" role="tablist">
            <button
              type="button"
              class="xyf-segmented__btn"
              :class="{ 'is-active': activeWorkspace === 'market' }"
              @click="switchWorkspace('market')"
            >
              <span>全部悬赏</span>
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

          <el-button type="primary" @click="openPublishDialog">
            <el-icon><Plus /></el-icon>
            <span>发布悬赏</span>
          </el-button>
        </div>
      </header>

      <!-- VIEW 1: Public Bounty Marketplace -->
      <template v-if="activeWorkspace === 'market'">
        <!-- Crisp Two-Row Filter Bar (zero clipped tabs) -->
        <section class="filter-bar">
          <div class="xyf-tabs filter-bar__tabs">
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

          <div class="filter-bar__controls">
            <div class="filter-bar__left">
              <div class="xyf-segmented" role="tablist" aria-label="优先级">
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': !filters.urgencyLevel }"
                  @click="selectUrgency(undefined)"
                >
                  全部
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 3 }"
                  @click="selectUrgency(3)"
                >
                  <svg class="seg-icon is-danger" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M9.3 1.8L3.4 9.1h4.1l-.8 5.1 5.9-7.3H8.5l.8-5.1Z" fill="currentColor" />
                  </svg>
                  <span>十万火急</span>
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 2 }"
                  @click="selectUrgency(2)"
                >
                  <svg class="seg-icon is-warn" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M8.7 2.2c.3 2.1-1.3 3.2-1.8 4.4-.4.9.1 1.7.7 2 .4-.6.8-1.1 1.3-1.4 1.3 1 2.2 2.4 2.2 3.9a3.8 3.8 0 0 1-7.6 0c0-2.6 1.7-4.5 3.1-6.1.8-.9 1.5-1.8 2.1-2.8Z" fill="currentColor" />
                  </svg>
                  <span>加急</span>
                </button>
                <button
                  type="button"
                  class="xyf-segmented__btn"
                  :class="{ 'is-active': filters.urgencyLevel === 1 }"
                  @click="selectUrgency(1)"
                >
                  常规
                </button>
              </div>

              <el-select
                v-model="filters.campusId"
                placeholder="全部校区"
                clearable
                class="compact-select"
                @change="reload"
              >
                <el-option
                  v-for="c in campuses"
                  :key="c.id"
                  :label="c.campusName"
                  :value="c.id"
                />
              </el-select>

              <el-select
                v-model="filters.tradeType"
                placeholder="全部交付"
                clearable
                class="compact-select compact-select--sm"
                @change="reload"
              >
                <el-option label="校内当面" :value="1" />
                <el-option label="线上远程" :value="2" />
              </el-select>
            </div>

            <div class="filter-bar__right">
              <el-input
                v-model="filters.keyword"
                placeholder="搜索悬赏关键词"
                clearable
                :prefix-icon="Search"
                class="search-input"
                @keyup.enter="reload"
                @clear="reload"
              />
              <button v-if="hasActiveFilters" type="button" class="reset-btn" @click="resetFilters">
                重置
              </button>
            </div>
          </div>
        </section>

        <!-- Loading Skeleton -->
        <div v-if="loading" class="bounty-feed">
          <div v-for="n in 4" :key="n" class="bounty-row bounty-row--skeleton">
            <el-skeleton :rows="2" animated />
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="demands.length === 0" class="empty-panel">
          <XyfEmptyState
            title="当前筛选下暂无招募悬赏"
            description="你可以清空筛选条件，或直接发布一条悬赏让校内认证创作者主动报价。"
          >
            <template #action>
              <el-button v-if="hasActiveFilters" plain @click="resetFilters">清空筛选</el-button>
              <el-button type="primary" @click="openPublishDialog">发布悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <!-- Clean Single-Column Editorial Bounty Feed (aligned tabular budget column, zero orphan cards) -->
        <div v-else class="bounty-feed">
          <article
            v-for="item in demands"
            :key="item.id"
            class="bounty-row"
            tabindex="0"
            role="link"
            :aria-label="item.title"
            @click="router.push(`/demands/${item.id}`)"
            @keydown.enter="router.push(`/demands/${item.id}`)"
          >
            <div class="bounty-row__body">
              <!-- Line 1: Animated SVG Urgency Emblem + Clean Metadata -->
              <div class="bounty-row__kicker xyf-tabular">
                <XyfUrgencyTag :level="item.urgencyLevel" size="sm" />
                <span v-if="item.tagName">{{ item.tagName }}</span>
                <span v-if="item.tagName" class="sep" aria-hidden="true">·</span>
                <span>{{ item.campusName || '主校区' }}</span>
                <template v-if="item.deadlineTime">
                  <span class="sep" aria-hidden="true">·</span>
                  <span class="meta-icon-txt" title="期望截止时间">
                    <el-icon><Clock /></el-icon>
                    <span>{{ formatShortDeadline(item.deadlineTime) }}</span>
                  </span>
                </template>
              </div>

              <!-- Line 2: Dominant Focal Title -->
              <h2 class="bounty-row__title">{{ item.title }}</h2>

              <!-- Line 3: Concise Single-Line Summary -->
              <p class="bounty-row__desc ellipsis">{{ item.description }}</p>

              <!-- Line 4: Publisher (Animated Shield) & Animated Delivery SVG -->
              <div class="bounty-row__foot">
                <div class="publisher-inline">
                  <XyfAvatar
                    :src="item.publisherAvatar"
                    :name="item.publisherName || '校内同学'"
                    :size="20"
                  />
                  <span>{{ item.publisherName || '校内实名同学' }}</span>
                  <XyfVerifiedMark :verified="item.publisherEduVerified === 1" />
                </div>
                <span class="sep" aria-hidden="true">·</span>
                <XyfDeliveryIcon
                  :trade-type="item.tradeType"
                  size="xs"
                  :text="item.tradeType === 1 ? (item.meetupPoiName || '') : ''"
                />
              </div>
            </div>

            <!-- Right Aligned Decision Column: Budget + Bid Count -->
            <div class="bounty-row__aside xyf-tabular">
              <div class="budget-val">
                <span class="yen">¥</span>{{ Number(item.budgetMin).toFixed(0) }}<span class="dash">–</span>{{ Number(item.budgetMax).toFixed(0) }}
              </div>
              <div class="budget-sub">
                <span v-if="item.demandStatus === 0">{{ item.bidCount || 0 }} 人报价</span>
                <span v-else class="is-closed">已选标</span>
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
        <div v-if="myPublished.length === 0" class="empty-panel">
          <XyfEmptyState
            title="你还没有发布过校内悬赏"
            description="发布定制需求即可收集多位认证创作者的报价方案。"
          >
            <template #action>
              <el-button type="primary" @click="openPublishDialog">发布首条悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="bounty-feed">
          <article
            v-for="d in myPublished"
            :key="d.id"
            class="bounty-row"
            tabindex="0"
            role="link"
            @click="router.push(`/demands/${d.id}`)"
            @keydown.enter="router.push(`/demands/${d.id}`)"
          >
            <div class="bounty-row__body">
              <div class="bounty-row__kicker xyf-tabular">
                <XyfStatusBadge
                  :tone="d.demandStatus === 0 ? 'dark' : d.demandStatus === 1 ? 'success' : 'neutral'"
                  :icon="d.demandStatus === 0 ? 'live' : d.demandStatus === 1 ? 'check' : 'lock'"
                  :tooltip="statusText(d.demandStatus)"
                  :icon-only="true"
                  size="xs"
                />
                <span class="xyf-mono">{{ d.demandNo }}</span>
              </div>
              <h2 class="bounty-row__title">{{ d.title }}</h2>
            </div>
            <div class="bounty-row__aside xyf-tabular">
              <div class="budget-val">
                <span class="yen">¥</span>{{ Number(d.budgetMin).toFixed(0) }}<span class="dash">–</span>{{ Number(d.budgetMax).toFixed(0) }}
              </div>
              <div class="budget-sub">{{ d.bidCount || 0 }} 份报价 →</div>
            </div>
          </article>
        </div>
      </section>

      <!-- VIEW 3: My Submitted Bids Workspace -->
      <section v-else class="workspace-panel">
        <div v-if="myBids.length === 0" class="empty-panel">
          <XyfEmptyState
            title="暂无参与的悬赏竞标记录"
            description="前往悬赏大厅挑选擅长的任务提交报价，中标后将自动生成平台担保订单。"
          >
            <template #action>
              <el-button type="primary" @click="switchWorkspace('market')">浏览招募中悬赏</el-button>
            </template>
          </XyfEmptyState>
        </div>

        <div v-else class="bounty-feed">
          <article
            v-for="b in myBids"
            :key="b.id"
            class="bounty-row"
            tabindex="0"
            role="link"
            @click="router.push(`/demands/${b.demandId}`)"
            @keydown.enter="router.push(`/demands/${b.demandId}`)"
          >
            <div class="bounty-row__body">
              <div class="bounty-row__kicker xyf-tabular">
                <XyfStatusBadge
                  :tone="b.bidStatus === 1 ? 'success' : b.bidStatus === 0 ? 'info' : 'neutral'"
                  :icon="b.bidStatus === 1 ? 'check' : b.bidStatus === 0 ? 'live' : 'lock'"
                  :tooltip="b.bidStatus === 1 ? '已中标' : b.bidStatus === 0 ? '待需求方选标' : '未中标'"
                  :icon-only="true"
                  size="xs"
                />
                <span class="meta-icon-txt">
                  <el-icon><Clock /></el-icon>
                  <span>{{ b.estimatedHours }}h</span>
                </span>
              </div>
              <h2 class="bounty-row__title">{{ b.demandTitle || `校内悬赏 #${b.demandId}` }}</h2>
              <p class="bounty-row__desc ellipsis">{{ b.bidPlan }}</p>
            </div>
            <div class="bounty-row__aside xyf-tabular">
              <div class="budget-val">
                <span class="yen">¥</span>{{ Number(b.bidPrice).toFixed(2) }}
              </div>
              <div class="budget-sub">我的报价 →</div>
            </div>
          </article>
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

      <div v-if="pubStep === 0" class="scene-templates">
        <span class="scene-templates__lbl">快捷套用场景：</span>
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
                <XyfUrgencyTag :level="u.value" :show-normal="true" :show-label="true" size="sm" />
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
import { Plus, Search, Clock } from '@element-plus/icons-vue'
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
import XyfUrgencyTag from '@/components/ui/XyfUrgencyTag.vue'
import XyfVerifiedMark from '@/components/ui/XyfVerifiedMark.vue'
import XyfStatusBadge from '@/components/ui/XyfStatusBadge.vue'
import XyfDeliveryIcon from '@/components/ui/XyfDeliveryIcon.vue'

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

function selectUrgency(level?: number) {
  filters.urgencyLevel = level
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
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 24px;

  &__title-wrap {
    display: flex;
    align-items: baseline;
    gap: 12px;

    h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: -0.025em;
      color: var(--xyf-ink);
    }
  }

  &__count {
    font-size: 13px;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

/* Crisp Two-Row Filter Bar */
.filter-bar {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__tabs {
    width: 100%;
  }

  &__controls {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.compact-select {
  width: 164px;

  &--sm {
    width: 126px;
  }
}

.search-input {
  width: 228px;

  @media (max-width: 640px) {
    width: 100%;
  }
}

.reset-btn {
  border: none;
  background: transparent;
  padding: 6px 4px;
  font-size: 13px;
  color: var(--xyf-ink-3);
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    color: var(--xyf-ink);
  }
}

.empty-panel {
  padding: 64px 24px;
  border-top: 1px solid var(--xyf-border);
}

/* Clean Editorial Bounty Feed */
.bounty-feed {
  border-top: 1px solid var(--xyf-border);
}

.bounty-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 172px;
  gap: 28px;
  align-items: center;
  padding: 22px 16px;
  border-bottom: 1px solid var(--xyf-border);
  cursor: pointer;
  transition: background 0.16s ease;

  &:hover,
  &:focus-visible {
    outline: none;
    background: var(--xyf-bg-subtle);

    .bounty-row__title {
      color: #000000;
    }
  }

  &--skeleton {
    display: block;
    cursor: default;
    &:hover {
      background: transparent;
    }
  }

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 18px 8px;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 7px;
    min-width: 0;
  }

  &__kicker {
    display: flex;
    align-items: center;
    gap: 7px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }

  &__title {
    margin: 0;
    font-size: 17.5px;
    font-weight: 600;
    line-height: 1.4;
    letter-spacing: -0.015em;
    color: var(--xyf-ink);
    transition: color 0.16s ease;
  }

  &__desc {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.55;
    color: var(--xyf-ink-2);
    max-width: 72ch;
  }

  &__foot {
    margin-top: 2px;
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
    min-width: 0;
  }

  &__aside {
    text-align: right;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;

    @media (max-width: 720px) {
      flex-direction: row;
      align-items: baseline;
      justify-content: space-between;
      text-align: left;
    }
  }
}

.seg-icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;

  &.is-danger {
    color: #dc2626;
  }

  &.is-warn {
    color: #d97706;
  }
}

.meta-icon-txt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;

  .el-icon {
    font-size: 13px;
    color: var(--xyf-ink-3);
    flex-shrink: 0;
  }
}

.sep {
  color: var(--xyf-ink-4);
  flex-shrink: 0;
}

.publisher-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--xyf-ink-2);
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.budget-val {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--xyf-ink);
  line-height: 1.15;

  .yen {
    font-size: 13px;
    font-weight: 600;
    margin-right: 1px;
  }

  .dash {
    margin: 0 2px;
    font-weight: 400;
    color: var(--xyf-ink-4);
  }
}

.budget-sub {
  font-size: 12.5px;
  color: var(--xyf-ink-3);

  .is-closed {
    color: var(--xyf-ink-4);
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
