<template>
  <div class="group-hall-page xyf-page">
    <div class="xyf-container">
      <!-- Hero Banner -->
      <section class="group-hero">
        <div class="group-hero__main">
          <span class="kicker">多人共享档期 · 满员阶梯自动退差价</span>
          <h1>校园拼班团课大厅</h1>
          <p>
            期末总复习、ACM 算法集训、电赛硬件实操、考研专业课串讲——认证创作者发起小班共享档期，参团人数达阶梯线后系统自动为全体早期学员退回差价，每人生成独立米级围栏担保订单。
          </p>
        </div>
        <div class="group-hero__actions">
          <el-button v-if="userStore.userInfo && !userStore.isAdmin" plain @click="openMyDrawer">
            我的开班 / 参团
          </el-button>
          <el-button
            v-if="userStore.hasPublishPermission"
            type="primary"
            size="large"
            @click="openCreateDialog"
          >
            + 发起拼班团课
          </el-button>
        </div>
      </section>

      <!-- Filter Bar -->
      <section class="xyf-surface-card filter-card">
        <div class="filter-row">
          <el-select v-model="filters.campusId" placeholder="全部校区" clearable style="width: 180px" @change="reload">
            <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
          </el-select>

          <el-select v-model="filters.tagId" placeholder="全部分类" clearable style="width: 160px" @change="reload">
            <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
          </el-select>

          <el-select v-model="filters.tradeType" placeholder="授课形式" clearable style="width: 150px" @change="reload">
            <el-option label="校内线下小班" :value="1" />
            <el-option label="线上共享课堂" :value="2" />
          </el-select>

          <el-input
            v-model="filters.keyword"
            placeholder="搜索团课关键词（如 线代冲刺 / 算法 / 建模）..."
            clearable
            style="flex: 1; min-width: 200px"
            @keyup.enter="reload"
            @clear="reload"
          />

          <el-button type="primary" plain @click="reload">筛选</el-button>
        </div>
      </section>

      <!-- Sessions Grid -->
      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="sessions.length === 0" class="xyf-surface-card" style="padding: 48px 24px">
        <XyfEmptyState
          title="当前暂无招募中的拼班团课"
          description="认证创作者可发起期末串讲或专题小班课，多人平摊课时费、讲师总收益翻倍。"
        />
      </div>

      <div v-else class="session-grid">
        <article
          v-for="item in sessions"
          :key="item.id"
          class="xyf-surface-card session-card"
          @click="router.push(`/groups/${item.id}`)"
        >
          <div class="session-card__top">
            <div class="pill-row">
              <span class="mode-pill">{{ item.tradeType === 1 ? '线下小班' : '线上课堂' }}</span>
              <span v-if="item.tagName" class="tag-pill">{{ item.tagName }}</span>
              <span class="tier-pill">
                满 {{ item.tierTargetCount }} 人降至 ¥{{ Number(item.tierDiscountPrice).toFixed(0) }}/人
              </span>
            </div>
            <el-tag
              size="small"
              :type="item.sessionStatus === 1 ? 'success' : item.sessionStatus === 2 ? 'warning' : 'primary'"
            >
              {{ statusText(item.sessionStatus) }}
            </el-tag>
          </div>

          <h3 class="session-title">{{ item.title }}</h3>
          <p class="session-desc">{{ item.description }}</p>

          <div class="time-loc-box xyf-mono">
            <div>开课时间：{{ formatDateTime(item.sessionStart) }}</div>
            <div v-if="item.tradeType === 1 && item.meetupPoiName">
              授课地标：{{ item.meetupPoiName }}（{{ item.meetupRadiusMeters || 80 }}m 围栏签到）
            </div>
          </div>

          <div class="progress-bar-wrap">
            <div class="prog-meta xyf-tabular">
              <span>已参团 {{ item.joinedCount }} / {{ item.maxParticipants }} 人（{{ item.minParticipants }} 人成班）</span>
              <span v-if="item.tierRebateApplied === 1" class="rebate-hit">已触发全员阶梯优惠价</span>
            </div>
            <el-progress
              :percentage="Math.min(100, Math.round((item.joinedCount / item.maxParticipants) * 100))"
              :stroke-width="8"
              :show-text="false"
              color="#10b981"
            />
          </div>

          <div class="session-card__foot">
            <div class="price-col xyf-tabular">
              <span class="cur-label">当前参团价</span>
              <strong>¥{{ Number(item.currentUnitPrice || item.basePrice).toFixed(2) }}</strong>
              <span v-if="item.basePrice > item.tierDiscountPrice" class="orig-price">
                原拼团价 ¥{{ Number(item.basePrice).toFixed(0) }}
              </span>
            </div>
            <div class="creator-mini">
              <span>讲师：{{ item.creatorName || '认证创作者' }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>

    <!-- Create Group Session Dialog -->
    <el-dialog v-model="createVisible" title="发起校内多人拼班团课" width="680px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="拼班团课标题（4~120字）" required>
          <el-input
            v-model="form.title"
            maxlength="120"
            placeholder="例如：高数下册期末核心题型 4 人冲刺小班（含自编真题讲义）"
          />
        </el-form-item>

        <div class="row-3">
          <el-form-item label="所属校区" required>
            <el-select v-model="form.campusId" style="width: 100%">
              <el-option v-for="c in campuses" :key="c.id" :label="c.campusName" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程分类">
            <el-select v-model="form.tagId" style="width: 100%">
              <el-option v-for="t in dictStore.serviceTags || []" :key="t.tagId" :label="t.tagName" :value="t.tagId" />
            </el-select>
          </el-form-item>
          <el-form-item label="授课方式">
            <el-select v-model="form.tradeType" style="width: 100%">
              <el-option label="校园线下当面小班" :value="1" />
              <el-option label="线上共享课堂" :value="2" />
            </el-select>
          </el-form-item>
        </div>

        <div class="row-3">
          <el-form-item label="最低成班人数" required>
            <el-input-number v-model="form.minParticipants" :min="2" :max="20" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高容纳人数" required>
            <el-input-number v-model="form.maxParticipants" :min="2" :max="50" style="width: 100%" />
          </el-form-item>
          <el-form-item label="触发阶梯优惠人数" required>
            <el-input-number v-model="form.tierTargetCount" :min="2" :max="50" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="基础拼班单价（元/人）" required>
            <el-input-number v-model="form.basePrice" :min="1" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="达标阶梯优惠单价（元/人，达标后早期学员自动退差价）" required>
            <el-input-number v-model="form.tierDiscountPrice" :min="1" :precision="2" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="row-2">
          <el-form-item label="开课开始时间" required>
            <el-date-picker
              v-model="form.sessionStart"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开课时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开课结束时间" required>
            <el-date-picker
              v-model="form.sessionEnd"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item
          v-if="form.tradeType === 1"
          label="在真实校园地图标记线下小班授课地点与签到核销半径"
          required
        >
          <CampusMapPicker
            :campus-id="form.campusId || 1"
            v-model="mapSpot"
            style="width: 100%"
            @fence-change="ok => (fenceValid = ok)"
          />
        </el-form-item>

        <el-form-item label="课程大纲与配套资料说明" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="800"
            show-word-limit
            placeholder="详细介绍本节团课的知识点模块、配套资料及适合哪些专业的同学参加..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="creating"
          :disabled="form.tradeType === 1 && !fenceValid"
          @click="submitCreate"
        >
          确认发布拼班团课
        </el-button>
      </template>
    </el-dialog>

    <!-- My Group Sessions Drawer -->
    <el-drawer v-model="drawerVisible" title="我的拼班团课记录" size="460px">
      <el-tabs v-model="drawerTab">
        <el-tab-pane label="我报名参团的" name="joined">
          <div v-if="myJoined.length === 0" class="drawer-empty">暂无报名参团记录</div>
          <div v-for="s in myJoined" :key="s.id" class="mini-item" @click="goDetail(s.id)">
            <strong>{{ s.title }}</strong>
            <div class="sub xyf-tabular">
              当前单价 ¥{{ s.currentUnitPrice }} · 已参团 {{ s.joinedCount }}/{{ s.maxParticipants }} 人
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="我开办的团课" name="created">
          <div v-if="myCreated.length === 0" class="drawer-empty">暂无开办团课记录</div>
          <div v-for="s in myCreated" :key="s.id" class="mini-item" @click="goDetail(s.id)">
            <strong>{{ s.title }}</strong>
            <div class="sub xyf-tabular">
              已报名 {{ s.joinedCount }}/{{ s.maxParticipants }} 人 · {{ statusText(s.sessionStatus) }}
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
  listGroupSessions,
  createGroupSession,
  listMyCreatedGroupSessions,
  listMyJoinedGroupSessions,
  type GroupSessionItem
} from '@/api/groupSession'
import { notifyFallbackError } from '@/api/request'
import { formatDateTime } from '@/utils/format'
import type { CampusInfo } from '@/types/dict'
import CampusMapPicker, { type MapLocationValue } from '@/components/business/CampusMapPicker.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const dictStore = useDictStore()

const loading = ref(false)
const sessions = ref<GroupSessionItem[]>([])
const campuses = ref<CampusInfo[]>([])

const filters = reactive({
  campusId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  tradeType: undefined as number | undefined,
  keyword: ''
})

const createVisible = ref(false)
const creating = ref(false)
const fenceValid = ref(true)
const form = reactive({
  campusId: 1,
  tagId: undefined as number | undefined,
  title: '',
  description: '',
  basePrice: 60,
  tierTargetCount: 4,
  tierDiscountPrice: 45,
  minParticipants: 2,
  maxParticipants: 6,
  tradeType: 1,
  sessionStart: '2026-12-01T19:00:00',
  sessionEnd: '2026-12-01T21:00:00'
})

const mapSpot = ref<MapLocationValue>({
  latitude: undefined,
  longitude: undefined,
  poiName: '',
  detailAddress: '',
  radiusMeters: 80
})

const drawerVisible = ref(false)
const drawerTab = ref<'joined' | 'created'>('joined')
const myJoined = ref<GroupSessionItem[]>([])
const myCreated = ref<GroupSessionItem[]>([])

function statusText(st: number) {
  if (st === 0) return '招募拼班中'
  if (st === 1) return '已成班待开课'
  if (st === 2) return '已满员锁定'
  if (st === 3) return '已完结'
  return '已取消'
}

function reload() {
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listGroupSessions({
      pageNum: 1,
      pageSize: 24,
      campusId: filters.campusId,
      tagId: filters.tagId,
      tradeType: filters.tradeType,
      keyword: filters.keyword.trim() || undefined
    })
    sessions.value = res.records || []
  } catch (err) {
    notifyFallbackError(err, '加载拼班团课大厅失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.campusId = userStore.userInfo?.campusId || campuses.value[0]?.id || 1
  form.tagId = dictStore.serviceTags?.[0]?.tagId
  createVisible.value = true
}

async function submitCreate() {
  if (!form.title.trim() || form.title.trim().length < 4) {
    ElMessage.warning('团课标题至少填写 4 个字')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请填写课程大纲说明')
    return
  }
  if (form.tierDiscountPrice > form.basePrice) {
    ElMessage.warning('阶梯满员优惠价不能高于基础拼班单价')
    return
  }
  if (form.tradeType === 1 && (!mapSpot.value.latitude || !mapSpot.value.poiName.trim())) {
    ElMessage.warning('请在校园地图标记线下授课地点')
    return
  }

  creating.value = true
  try {
    const id = await createGroupSession({
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      meetupLat: form.tradeType === 1 ? mapSpot.value.latitude : undefined,
      meetupLng: form.tradeType === 1 ? mapSpot.value.longitude : undefined,
      meetupPoiName: form.tradeType === 1 ? mapSpot.value.poiName.trim() : undefined,
      meetupAddress: form.tradeType === 1 ? mapSpot.value.detailAddress?.trim() : undefined,
      meetupRadiusMeters: form.tradeType === 1 ? mapSpot.value.radiusMeters || 80 : undefined
    })
    ElMessage.success('拼班团课已发布，快邀请同学一起拼团享阶梯优惠')
    createVisible.value = false
    router.push(`/groups/${id}`)
  } catch (err) {
    notifyFallbackError(err, '发布团课失败')
  } finally {
    creating.value = false
  }
}

async function openMyDrawer() {
  drawerVisible.value = true
  const [j, c] = await Promise.all([
    listMyJoinedGroupSessions().catch(() => []),
    listMyCreatedGroupSessions().catch(() => [])
  ])
  myJoined.value = j
  myCreated.value = c
}

function goDetail(id: number) {
  drawerVisible.value = false
  router.push(`/groups/${id}`)
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
.group-hero {
  padding: 28px 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, #064e3b 0%, #18181b 100%);
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
    background: rgba(52, 211, 153, 0.2);
    color: #6ee7b7;
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
    color: rgba(255, 255, 255, 0.82);
  }

  &__actions {
    display: flex;
    gap: 10px;
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
}

.session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.session-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.18s ease;

  &:hover {
    transform: translateY(-2px);
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }
}

.pill-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mode-pill,
.tag-pill,
.tier-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.mode-pill {
  background: #f4f4f5;
  color: #18181b;
}

.tag-pill {
  background: #f4f4f5;
  color: #52525b;
}

.tier-pill {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.session-title {
  margin: 2px 0 0;
  font-size: 16.5px;
  font-weight: 700;
}

.session-desc {
  margin: 0;
  font-size: 13px;
  color: var(--xyf-ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.time-loc-box {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--xyf-surface-soft);
  font-size: 12px;
  color: var(--xyf-ink-muted);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.progress-bar-wrap {
  .prog-meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--xyf-ink-2);
    margin-bottom: 5px;

    .rebate-hit {
      color: #059669;
      font-weight: 700;
    }
  }
}

.session-card__foot {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--xyf-border);
  display: flex;
  justify-content: space-between;
  align-items: baseline;

  .price-col {
    .cur-label {
      font-size: 11.5px;
      color: var(--xyf-ink-muted);
      margin-right: 5px;
    }
    strong {
      font-size: 19px;
      font-weight: 800;
      color: #047857;
    }
    .orig-price {
      margin-left: 6px;
      font-size: 12px;
      text-decoration: line-through;
      color: var(--xyf-ink-muted);
    }
  }

  .creator-mini {
    font-size: 12px;
    color: var(--xyf-ink-muted);
  }
}

.row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.mini-item {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--xyf-border);
  margin-bottom: 10px;
  cursor: pointer;

  .sub {
    margin-top: 4px;
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.drawer-empty {
  padding: 28px 0;
  text-align: center;
  color: var(--xyf-ink-muted);
}
</style>
