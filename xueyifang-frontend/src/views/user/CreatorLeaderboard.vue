<template>
  <div class="creator-leaderboard-page xyf-page">
    <div class="xyf-container">
      <!-- Editorial Gallery Header -->
      <header class="fame-header">
        <div class="fame-header__left">
          <h1>行家榜</h1>
        </div>

        <!-- Dimension Segmented Switcher -->
        <div class="xyf-segmented" role="tablist" aria-label="排行维度">
          <button
            v-for="tab in rankDimensions"
            :key="tab.value"
            type="button"
            class="xyf-segmented__btn"
            :class="{ 'is-active': filters.rankBy === tab.value }"
            @click="selectRankBy(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
      </header>

      <!-- Clean Filter Strip -->
      <section class="fame-filter-bar">
        <div class="xyf-tabs">
          <button
            type="button"
            class="xyf-tab"
            :class="{ 'is-active': !filters.tagId }"
            @click="selectTag(undefined)"
          >
            全部技能领域
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
            v-for="c in campusList"
            :key="c.id"
            type="button"
            class="campus-pill"
            :class="{ 'is-active': filters.campusId === c.id }"
            @click="selectCampus(c.id)"
          >
            {{ c.campusName }}
          </button>
        </div>
      </section>

      <div v-if="loading" class="xyf-surface-card" style="padding: 36px">
        <el-skeleton :rows="7" animated />
      </div>

      <div v-else-if="!leaders.length" class="xyf-surface-card" style="padding: 52px">
        <XyfEmptyState
          title="当前筛选条件下暂无上榜创作者"
          description="切换全部校区或全部技能分类查看全校认证行家席位"
        >
          <template #action>
            <el-button plain @click="resetAll">重置筛选</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <!-- Architectural Podium Stage (2 - 1 - 3 Asymmetric Layout on Desktop) -->
        <section v-if="podiumDisplay.length" class="podium-stage">
          <article
            v-for="item in podiumDisplay"
            :key="item.userId"
            class="podium-monument"
            :class="`is-rank-${item.rank}`"
            @click="router.push(`/creators/${item.userId}`)"
          >
            <div class="podium-monument__top">
              <div class="rank-emblem xyf-tabular">
                <svg v-if="item.rank === 1" viewBox="0 0 20 20" fill="none" class="crown-svg">
                  <path d="M3 15L4.8 7L10 11L15.2 7L17 15H3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                </svg>
                <span>NO.0{{ item.rank }}</span>
              </div>

              <span class="level-seal">{{ item.creatorLevelTitle }}</span>
            </div>

            <div class="podium-identity">
              <div class="podium-avatar-ring">
                <XyfAvatar
                  :src="item.avatar"
                  :name="item.displayName"
                  :size="item.rank === 1 ? 74 : 62"
                />
              </div>
              <h3>{{ item.displayName }}</h3>
              <p class="podium-campus ellipsis">
                {{ item.campusName }} · {{ item.professionalName }}
              </p>
            </div>

            <!-- Honor Badges -->
            <div v-if="item.badges?.length" class="podium-badges">
              <span
                v-for="b in item.badges.slice(0, 3)"
                :key="b.badgeCode"
                class="honor-pill"
                :title="b.badgeDesc"
              >
                ✦ {{ b.badgeName }}
              </span>
            </div>

            <!-- 4-Cell Tabular Metric Grid -->
            <div class="podium-stats xyf-tabular">
              <div class="p-stat">
                <strong>{{ Number(item.compositeInfluenceScore || 0).toFixed(1) }}</strong>
                <span>影响力指数</span>
              </div>
              <div class="p-stat">
                <strong>{{ item.completedOrderCount }}</strong>
                <span>担保完单</span>
              </div>
              <div class="p-stat">
                <strong>{{ item.creditScore }}</strong>
                <span>履约信用分</span>
              </div>
              <div class="p-stat">
                <strong>{{ Number(item.totalPracticeHours || 0).toFixed(1) }}h</strong>
                <span>实践学时</span>
              </div>
            </div>

            <div class="podium-foot">
              <span>访问数字作品集主页</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </article>
        </section>

        <!-- Full Honor Directory Matrix -->
        <section class="xyf-surface-card directory-card">
          <div class="directory-head">
            <div>
              <h2>认证创作者荣誉名录（共 {{ leaders.length }} 席）</h2>
              <p>点击任意创作者可调阅其公开数字信用护照、在架技能橱窗与历史真实评价</p>
            </div>
          </div>

          <div class="directory-table">
            <div class="dir-thead">
              <span class="col-rank">席位</span>
              <span class="col-user">创作者与认证资质</span>
              <span class="col-bar">综合影响力刻度</span>
              <span class="col-kpi">完单 / 信用 / 口碑</span>
              <span class="col-act">工作室</span>
            </div>

            <div
              v-for="row in leaders"
              :key="row.userId"
              class="dir-row"
              @click="router.push(`/creators/${row.userId}`)"
            >
              <div class="col-rank xyf-tabular">
                <span class="rank-index" :class="{ 'is-top': row.rank <= 3 }">
                  {{ String(row.rank).padStart(2, '0') }}
                </span>
              </div>

              <div class="col-user">
                <XyfAvatar :src="row.avatar" :name="row.displayName" :size="44" />
                <div class="user-meta">
                  <div class="user-meta__name">
                    <strong>{{ row.displayName }}</strong>
                    <span class="tier-chip">{{ row.creatorLevelTitle }}</span>
                    <span v-if="row.eduVerified === 1" class="edu-seal">edu.cn 已核验</span>
                  </div>
                  <div class="user-meta__sub ellipsis">
                    {{ row.campusName }} · {{ row.professionalName }}
                    <template v-if="row.topSkillTags?.length">
                      · {{ row.topSkillTags.join(' / ') }}
                    </template>
                  </div>
                </div>
              </div>

              <div class="col-bar xyf-tabular">
                <div class="inf-row">
                  <strong>{{ Number(row.compositeInfluenceScore || 0).toFixed(1) }}</strong>
                  <span>{{ Number(row.totalPracticeHours || 0).toFixed(1) }} 学时</span>
                </div>
                <div class="inf-track">
                  <div
                    class="inf-fill"
                    :style="{ width: `${calcInfluencePercent(row.compositeInfluenceScore)}%` }"
                  />
                </div>
              </div>

              <div class="col-kpi xyf-tabular">
                <div class="kpi-mini">
                  <strong>{{ row.completedOrderCount }}</strong>
                  <span>完单</span>
                </div>
                <div class="kpi-mini">
                  <strong>{{ row.creditScore }}</strong>
                  <span>信用</span>
                </div>
                <div class="kpi-mini">
                  <strong>★ {{ Number(row.averageRating || 5).toFixed(1) }}</strong>
                  <span>评分</span>
                </div>
              </div>

              <div class="col-act" @click.stop>
                <el-button size="small" plain @click="router.push(`/creators/${row.userId}`)">
                  访问主页
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import { listCampuses } from '@/api/campus'
import type { CampusInfo } from '@/types/dict'
import { getCreatorLeaderboard, type CreatorLeaderboardItem } from '@/api/leaderboard'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'

const router = useRouter()
const dictStore = useDictStore()

const loading = ref(false)
const leaders = ref<CreatorLeaderboardItem[]>([])
const campusList = ref<CampusInfo[]>([])

const rankDimensions = [
  { label: '综合影响力', value: 'COMPREHENSIVE' },
  { label: '实战完单量', value: 'COMPLETED_ORDERS' },
  { label: '履约信用分', value: 'CREDIT_SCORE' },
  { label: '五星口碑榜', value: 'RATING' }
]

const filters = reactive({
  rankBy: 'COMPREHENSIVE',
  campusId: undefined as number | undefined,
  tagId: undefined as number | undefined
})

const podiumDisplay = computed(() => {
  const top = leaders.value.slice(0, 3)
  if (top.length === 3) {
    // 2 - 1 - 3 Olympic visual order on desktop
    return [top[1], top[0], top[2]]
  }
  return top
})

const maxInfluence = computed(() => {
  if (!leaders.value.length) return 100
  return Math.max(100, ...leaders.value.map(i => Number(i.compositeInfluenceScore || 0)))
})

function calcInfluencePercent(score?: number) {
  const val = Number(score || 0)
  return Math.min(100, Math.max(12, Math.round((val / maxInfluence.value) * 100)))
}

function selectRankBy(val: string) {
  filters.rankBy = val
  fetchLeaderboard()
}

function selectTag(id?: number) {
  filters.tagId = id
  fetchLeaderboard()
}

function selectCampus(id?: number) {
  filters.campusId = id
  fetchLeaderboard()
}

function resetAll() {
  filters.campusId = undefined
  filters.tagId = undefined
  fetchLeaderboard()
}

const fetchLeaderboard = async () => {
  loading.value = true
  try {
    leaders.value = await getCreatorLeaderboard({
      rankBy: filters.rankBy,
      campusId: filters.campusId,
      tagId: filters.tagId,
      limit: 30
    })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  dictStore.fetchServiceTags()
  try {
    campusList.value = await listCampuses()
  } catch {
    // ignore
  }
  await fetchLeaderboard()
})
</script>

<style scoped lang="scss">
.fame-header {
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
      font-weight: 780;
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
}

.fame-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--xyf-border);
  margin-bottom: 32px;

  .xyf-tabs {
    border-bottom: none;
    flex: 1;
    min-width: 240px;
  }
}

.campus-pills {
  display: flex;
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
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &.is-active {
    background: #18181b;
    border-color: #18181b;
    color: #ffffff;
  }
}

/* Architectural 2 - 1 - 3 Podium Stage */
.podium-stage {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  align-items: end;
  margin-bottom: 32px;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

.podium-monument {
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 20px;
  padding: 24px 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  cursor: pointer;
  transition: transform var(--xyf-transition), box-shadow var(--xyf-transition), border-color var(--xyf-transition);

  &:hover {
    transform: translateY(-4px);
    border-color: #18181b;
    box-shadow: var(--xyf-shadow-md);
  }

  &.is-rank-1 {
    background: #141417;
    color: #ffffff;
    border-color: #27272a;
    padding: 30px 26px 20px;
    box-shadow: 0 24px 52px -18px rgba(9, 9, 11, 0.34);

    @media (max-width: 960px) {
      order: -1;
    }

    .rank-emblem {
      background: rgba(200, 169, 126, 0.18);
      border-color: rgba(200, 169, 126, 0.45);
      color: #e5c79e;
    }

    .level-seal {
      background: rgba(255, 255, 255, 0.1);
      color: #f4f4f5;
    }

    .podium-identity h3 {
      color: #ffffff;
      font-size: 20px;
    }

    .podium-campus {
      color: rgba(255, 255, 255, 0.65);
    }

    .podium-avatar-ring {
      padding: 3px;
      border-radius: 50%;
      background: linear-gradient(135deg, #f3e2c7 0%, #a88554 100%);
    }

    .honor-pill {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.14);
      color: #e5c79e;
    }

    .podium-stats {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);

      strong {
        color: #ffffff;
      }
      span {
        color: rgba(255, 255, 255, 0.58);
      }
    }

    .podium-foot {
      border-top-color: rgba(255, 255, 255, 0.1);
      color: #e5c79e;
    }
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.rank-emblem {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  font-size: 12px;
  font-weight: 800;
  color: var(--xyf-ink);

  .crown-svg {
    width: 14px;
    height: 14px;
  }
}

.level-seal {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--xyf-surface-muted);
  color: var(--xyf-ink-2);
}

.podium-identity {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;

  h3 {
    margin: 4px 0 0;
    font-size: 18px;
    font-weight: 750;
    color: var(--xyf-ink);
  }

  .podium-campus {
    margin: 0;
    font-size: 12.5px;
    color: var(--xyf-ink-3);
    max-width: 100%;
  }
}

.podium-badges {
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}

.honor-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--xyf-champagne-soft);
  border: 1px solid var(--xyf-champagne-border);
  color: #9a5b13;
}

.podium-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  padding: 12px 10px;
  border-radius: 12px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border-subtle);
  text-align: center;

  .p-stat {
    display: flex;
    flex-direction: column;
    gap: 2px;

    strong {
      font-size: 15px;
      font-weight: 750;
      color: var(--xyf-ink);
    }

    span {
      font-size: 10.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.podium-foot {
  padding-top: 12px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--xyf-ink-2);
}

/* Directory Matrix */
.directory-card {
  padding: 26px 28px;
}

.directory-head {
  margin-bottom: 18px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 750;
    color: var(--xyf-ink);
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--xyf-ink-3);
  }
}

.dir-thead {
  display: grid;
  grid-template-columns: 64px minmax(0, 1.5fr) minmax(160px, 0.9fr) 210px 96px;
  gap: 16px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--xyf-border);
  font-size: 12px;
  font-weight: 600;
  color: var(--xyf-ink-3);

  @media (max-width: 920px) {
    display: none;
  }
}

.dir-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1.5fr) minmax(160px, 0.9fr) 210px 96px;
  gap: 16px;
  align-items: center;
  padding: 15px 14px;
  border-bottom: 1px solid var(--xyf-border-subtle);
  cursor: pointer;
  transition: background var(--xyf-transition);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--xyf-surface-soft);
  }

  @media (max-width: 920px) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

.rank-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--xyf-surface-soft);
  color: var(--xyf-ink-2);
  font-size: 13px;
  font-weight: 700;

  &.is-top {
    background: #18181b;
    color: #e5c79e;
  }
}

.col-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;

  .user-meta {
    min-width: 0;

    &__name {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;

      strong {
        font-size: 14.5px;
        color: var(--xyf-ink);
      }
    }

    .tier-chip,
    .edu-seal {
      font-size: 11px;
      font-weight: 600;
      padding: 1px 7px;
      border-radius: 999px;
      background: var(--xyf-surface-muted);
      color: var(--xyf-ink-2);
    }

    .edu-seal {
      background: var(--xyf-champagne-soft);
      border: 1px solid var(--xyf-champagne-border);
      color: #9a5b13;
    }

    &__sub {
      margin-top: 3px;
      font-size: 12.5px;
      color: var(--xyf-ink-3);
    }
  }
}

.col-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .inf-row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--xyf-ink-3);

    strong {
      color: var(--xyf-ink);
      font-size: 13.5px;
    }
  }

  .inf-track {
    height: 6px;
    border-radius: 999px;
    background: #ececef;
    overflow: hidden;

    .inf-fill {
      height: 100%;
      background: #18181b;
      border-radius: 999px;
    }
  }
}

.col-kpi {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  text-align: center;

  .kpi-mini {
    display: flex;
    flex-direction: column;

    strong {
      font-size: 14px;
      font-weight: 700;
      color: var(--xyf-ink);
    }

    span {
      font-size: 11px;
      color: var(--xyf-ink-3);
    }
  }
}

.col-act {
  text-align: right;
}
</style>
