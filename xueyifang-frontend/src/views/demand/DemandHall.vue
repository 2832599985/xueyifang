<template>
  <div class="demand-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Hero Banner -->
      <section class="hall-hero">
        <div class="hall-hero__main">
          <span class="kicker">C2B 反向定制 · 平台托管担保</span>
          <h1>校园悬赏需求大厅</h1>
          <p>
            找不到现成服务？直接发一条校内悬赏（考前突击、代码联调、答辩PPT精修、校园跟拍），认证创作者在线报价，选标后一键转为米级电子围栏担保订单。
          </p>
        </div>
        <div class="hall-hero__actions">
          <el-button v-if="userStore.userInfo && !userStore.isAdmin" plain @click="openMyDrawer">
            我的悬赏 / 竞标
          </el-button>
          <el-button type="primary" size="large" @click="openPublishDialog">
            + 发布校内悬赏需求
          </el-button>
        </div>
      </section>

      <!-- Filter Strip -->
      <section class="xyf-surface-card filter-card">
        <div class="filter-row">
          <el-select v-model="filters.campusId" placeholder="全部校区" clearable style="width: 180px" @change="reload">
            <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
          </el-select>

          <el-select v-model="filters.tagId" placeholder="全部分类" clearable style="width: 160px" @change="reload">
            <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
          </el-select>

          <el-select v-model="filters.urgencyLevel" placeholder="紧急程度" clearable style="width: 140px" @change="reload">
            <el-option label="十万火急" :value="3" />
            <el-option label="加急需求" :value="2" />
            <el-option label="常规需求" :value="1" />
          </el-select>

          <el-select v-model="filters.tradeType" placeholder="交付方式" clearable style="width: 150px" @change="reload">
            <el-option label="校园线下当面" :value="1" />
            <el-option label="线上远程交付" :value="2" />
          </el-select>

          <el-input
            v-model="filters.keyword"
            placeholder="搜索悬赏关键词（如 高数 / STM32 / 跟拍）..."
            clearable
            style="flex: 1; min-width: 200px"
            @keyup.enter="reload"
            @clear="reload"
          />

          <el-button type="primary" plain @click="reload">筛选</el-button>
        </div>
      </section>

      <!-- Demand Cards Grid -->
      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="demands.length === 0" class="xyf-surface-card" style="padding: 48px 24px">
        <XyfEmptyState
          title="当前暂无匹配条件的校内悬赏"
          description="成为第一个发起定制悬赏的同学，认证创作者将主动提交报价方案。"
        >
          <template #action>
            <el-button type="primary" @click="openPublishDialog">立即发布悬赏</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <div v-else class="demand-grid">
        <article
          v-for="item in demands"
          :key="item.id"
          class="xyf-surface-card demand-card"
          @click="router.push(`/demands/${item.id}`)"
        >
          <div class="demand-card__top">
            <div class="badges">
              <span class="urgency-pill" :class="`is-u${item.urgencyLevel}`">
                {{ urgencyLabel(item.urgencyLevel) }}
              </span>
              <span class="mode-pill">
                {{ item.tradeType === 1 ? '校内当面' : '线上交付' }}
              </span>
              <span v-if="item.tagName" class="tag-pill">{{ item.tagName }}</span>
            </div>
            <el-tag size="small" :type="item.demandStatus === 0 ? 'success' : 'info'">
              {{ item.demandStatus === 0 ? '招募竞标中' : '已选标成单' }}
            </el-tag>
          </div>

          <h3 class="demand-title">{{ item.title }}</h3>
          <p class="demand-desc">{{ item.description }}</p>

          <div v-if="item.tradeType === 1 && item.meetupPoiName" class="spot-strip xyf-mono">
            <span>约定地标：{{ item.meetupPoiName }}（{{ item.meetupRadiusMeters || 80 }}m 围栏核销）</span>
          </div>

          <div class="demand-card__foot">
            <div class="budget-box xyf-tabular">
              <span class="label">悬赏预算</span>
              <strong>¥{{ Number(item.budgetMin).toFixed(0) }} - ¥{{ Number(item.budgetMax).toFixed(0) }}</strong>
            </div>
            <div class="bid-stat">
              <span>{{ item.campusName || '主校区' }}</span>
              <span>·</span>
              <strong>{{ item.bidCount || 0 }} 人已报价</strong>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > pageSize" class="pager-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- Publish Demand Dialog -->
    <el-dialog v-model="publishVisible" title="发布校内反向悬赏需求" width="660px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="悬赏标题（4~120字）" required>
          <el-input
            v-model="pubForm.title"
            maxlength="120"
            show-word-limit
            placeholder="例如：周四晚南湖图书馆高数下册曲线曲面积分 2 小时一对一突击"
          />
        </el-form-item>

        <div class="row-3">
          <el-form-item label="所属校区" required>
            <el-select v-model="pubForm.campusId" style="width: 100%">
              <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="需求分类">
            <el-select v-model="pubForm.tagId" placeholder="选择分类" style="width: 100%">
              <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
            </el-select>
          </el-form-item>

          <el-form-item label="紧急程度">
            <el-select v-model="pubForm.urgencyLevel" style="width: 100%">
              <el-option label="常规需求" :value="1" />
              <el-option label="加急（优先排序）" :value="2" />
              <el-option label="十万火急（置顶高亮）" :value="3" />
            </el-select>
          </el-form-item>
        </div>

        <div class="row-3">
          <el-form-item label="预算下限（元）" required>
            <el-input-number v-model="pubForm.budgetMin" :min="1" :max="9999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预算上限（元）" required>
            <el-input-number v-model="pubForm.budgetMax" :min="1" :max="9999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="期望最晚完成时间">
            <el-date-picker
              v-model="pubForm.deadlineTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选填"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item label="交付方式" required>
          <el-radio-group v-model="pubForm.tradeType">
            <el-radio :label="1">校园线下当面（支持真实地图打点与米级半径强核销）</el-radio>
            <el-radio :label="2">线上远程交付（站内交付凭证验收）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="pubForm.tradeType === 1"
          label="在真实校园地图标记会面位置与核销有效半径"
          required
        >
          <CampusMapPicker
            :campus-id="pubForm.campusId || 1"
            v-model="pubMapSpot"
            style="width: 100%"
            @fence-change="ok => (pubFenceValid = ok)"
          />
        </el-form-item>

        <el-form-item label="详细需求描述与验收标准" required>
          <el-input
            v-model="pubForm.description"
            type="textarea"
            :rows="4"
            maxlength="800"
            show-word-limit
            placeholder="请说明当前背景、需要创作者具备什么经验、交付物或当面辅导时长要求..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="publishing"
          :disabled="pubForm.tradeType === 1 && !pubFenceValid"
          @click="submitPublish"
        >
          确认发布悬赏
        </el-button>
      </template>
    </el-dialog>

    <!-- My Demands & Bids Drawer -->
    <el-drawer v-model="myDrawerVisible" title="我的悬赏与竞标记录" size="460px">
      <el-tabs v-model="myActiveTab">
        <el-tab-pane label="我发布的悬赏" name="published">
          <div v-if="myPublished.length === 0" class="drawer-empty">暂无发布的悬赏需求</div>
          <div v-for="d in myPublished" :key="d.id" class="mini-record" @click="goDetailFromDrawer(d.id)">
            <div class="mini-record__head">
              <strong>{{ d.title }}</strong>
              <el-tag size="small">{{ statusText(d.demandStatus) }}</el-tag>
            </div>
            <div class="mini-record__sub xyf-tabular">
              预算 ¥{{ d.budgetMin }}-{{ d.budgetMax }} · {{ d.bidCount }} 人报价
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我参与的竞标" name="bids">
          <div v-if="myBids.length === 0" class="drawer-empty">暂无参与的竞标报价</div>
          <div v-for="b in myBids" :key="b.id" class="mini-record" @click="goDetailFromDrawer(b.demandId)">
            <div class="mini-record__head">
              <strong>{{ b.demandTitle || `悬赏 #${b.demandId}` }}</strong>
              <el-tag size="small" :type="b.bidStatus === 1 ? 'success' : b.bidStatus === 0 ? 'warning' : 'info'">
                {{ b.bidStatus === 1 ? '已中标转单' : b.bidStatus === 0 ? '待需求方选标' : '未中标' }}
              </el-tag>
            </div>
            <div class="mini-record__sub xyf-tabular">
              我的报价 ¥{{ b.bidPrice }} · 承诺 {{ b.estimatedHours }} 小时完成
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const router = useRouter()
const userStore = useUserStore()
const dictStore = useDictStore()

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
  keyword: ''
})

const publishVisible = ref(false)
const publishing = ref(false)
const pubFenceValid = ref(true)
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

const myDrawerVisible = ref(false)
const myActiveTab = ref<'published' | 'bids'>('published')
const myPublished = ref<DemandItem[]>([])
const myBids = ref<DemandBidItem[]>([])

function urgencyLabel(level: number) {
  if (level === 3) return '十万火急'
  if (level === 2) return '加急'
  return '常规'
}

function statusText(st: number) {
  if (st === 0) return '招募竞标中'
  if (st === 1) return '已选标转单'
  if (st === 2) return '已取消'
  return '已关闭'
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

function openPublishDialog() {
  if (!userStore.userInfo) {
    router.push({ path: '/login', query: { redirect: '/demands' } })
    return
  }
  if (userStore.isAdmin) {
    ElMessage.info('管理员账号请前往后台巡检模块管理悬赏')
    return
  }
  pubForm.campusId = userStore.userInfo.campusId || campuses.value[0]?.id || 1
  pubForm.tagId = dictStore.serviceTags?.[0]?.tagId
  publishVisible.value = true
}

async function submitPublish() {
  if (!pubForm.title.trim() || pubForm.title.trim().length < 4) {
    ElMessage.warning('悬赏标题至少填写 4 个字')
    return
  }
  if (!pubForm.description.trim()) {
    ElMessage.warning('请填写详细需求说明')
    return
  }
  if (pubForm.budgetMax < pubForm.budgetMin) {
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

async function openMyDrawer() {
  myDrawerVisible.value = true
  const [pub, bids] = await Promise.all([
    listMyPublishedDemands().catch(() => []),
    listMyDemandBids().catch(() => [])
  ])
  myPublished.value = pub
  myBids.value = bids
}

function goDetailFromDrawer(id: number) {
  myDrawerVisible.value = false
  router.push(`/demands/${id}`)
}

onMounted(async () => {
  await Promise.all([
    listCampuses().then(res => (campuses.value = res)).catch(() => {}),
    dictStore.fetchServiceTags().catch(() => {}),
    fetchList()
  ])
})
</script>

<style scoped lang="scss">
.hall-hero {
  padding: 28px 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, #18181b 0%, #27272a 100%);
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 18px;

  .kicker {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 999px;
    background: rgba(52, 211, 153, 0.16);
    color: #34d399;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  h1 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 800;
  }

  p {
    margin: 0;
    max-width: 680px;
    font-size: 13.5px;
    line-height: 1.65;
    color: rgba(255, 255, 255, 0.78);
  }

  &__actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.filter-card {
  padding: 16px 20px;
  margin-bottom: 18px;
}

.filter-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.demand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.demand-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 28px -12px rgba(24, 24, 27, 0.16);
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }
}

.badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.urgency-pill,
.mode-pill,
.tag-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.urgency-pill {
  &.is-u3 {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }
  &.is-u2 {
    background: #fffbeb;
    color: #d97706;
    border: 1px solid #fde68a;
  }
  &.is-u1 {
    background: #f4f4f5;
    color: #52525b;
  }
}

.mode-pill {
  background: #ecfdf5;
  color: #047857;
}

.tag-pill {
  background: #f4f4f5;
  color: #18181b;
}

.demand-title {
  margin: 2px 0 0;
  font-size: 16.5px;
  font-weight: 700;
  color: var(--xyf-ink);
  line-height: 1.4;
}

.demand-desc {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-2);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spot-strip {
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--xyf-surface-soft);
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

.demand-card__foot {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--xyf-border);
  display: flex;
  justify-content: space-between;
  align-items: baseline;

  .budget-box {
    .label {
      font-size: 11.5px;
      color: var(--xyf-ink-muted);
      margin-right: 6px;
    }
    strong {
      font-size: 18px;
      font-weight: 800;
      color: var(--xyf-ink);
    }
  }

  .bid-stat {
    font-size: 12px;
    color: var(--xyf-ink-muted);
    display: flex;
    gap: 5px;
    strong {
      color: #047857;
    }
  }
}

.row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.pager-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.mini-record {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  margin-bottom: 10px;
  cursor: pointer;

  &:hover {
    border-color: #18181b;
  }

  &__head {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__sub {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.drawer-empty {
  padding: 32px 0;
  text-align: center;
  color: var(--xyf-ink-muted);
  font-size: 13px;
}

@media (max-width: 768px) {
  .row-3 {
    grid-template-columns: 1fr;
  }
}
</style>
