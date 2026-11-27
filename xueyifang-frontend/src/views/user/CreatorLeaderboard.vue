<template>
  <div class="creator-leaderboard-page xyf-page">
    <div class="xyf-container">
      <!-- Hero Header -->
      <section class="leaderboard-hero xyf-surface-card">
        <div class="hero-main">
          <span class="hero-kicker">CAMPUS SKILL HALL OF FAME · 校友创作者技能风云榜</span>
          <h1>学艺坊校友技能荣誉殿堂与能力勋章榜</h1>
          <p>
            综合考量「教育邮箱双核认证、校园履约信用分、历史真实完单量、SHA-256 实践学时、五星好评率与在池履约保证金」六大维度，实时遴选各校区最具实力的标杆技能创作者。
          </p>
        </div>

        <div class="hero-filters">
          <el-radio-group v-model="filters.rankBy" size="large" @change="fetchLeaderboard">
            <el-radio-button value="COMPREHENSIVE">综合影响力总榜</el-radio-button>
            <el-radio-button value="COMPLETED_ORDERS">实战完单榜</el-radio-button>
            <el-radio-button value="CREDIT_SCORE">履约信用标杆榜</el-radio-button>
            <el-radio-button value="RATING">五星口碑好评榜</el-radio-button>
          </el-radio-group>

          <div class="sub-filter-row">
            <el-select
              v-model="filters.campusId"
              placeholder="全部校区"
              clearable
              style="width: 180px"
              @change="fetchLeaderboard"
            >
              <el-option
                v-for="c in campusList"
                :key="c.id"
                :label="c.campusName"
                :value="c.id"
              />
            </el-select>

            <el-select
              v-model="filters.tagId"
              placeholder="全部技能领域"
              clearable
              style="width: 200px"
              @change="fetchLeaderboard"
            >
              <el-option
                v-for="t in dictStore.serviceTags || []"
                :key="t.tagId"
                :label="t.tagName"
                :value="t.tagId"
              />
            </el-select>
          </div>
        </div>
      </section>

      <div v-if="loading" class="xyf-surface-card" style="padding: 28px">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!leaders.length" class="xyf-surface-card" style="padding: 36px">
        <XyfEmptyState
          title="当前筛选条件下暂无上榜创作者"
          description="尝试切换全部校区或全部技能分类查看全校技能风云榜"
        />
      </div>

      <template v-else>
        <!-- Top 3 Podium -->
        <section class="podium-grid" v-if="topThree.length">
          <article
            v-for="item in topThree"
            :key="item.userId"
            class="podium-card xyf-surface-card"
            :class="`is-rank-${item.rank}`"
            @click="router.push(`/creators/${item.userId}`)"
          >
            <div class="podium-medal">
              <span class="medal-rank">TOP {{ item.rank }}</span>
              <span class="level-pill">{{ item.creatorLevelTitle }}</span>
            </div>

            <div class="podium-profile">
              <XyfAvatar :src="item.avatar" :name="item.displayName" :size="60" />
              <div class="podium-profile__info">
                <h3>{{ item.displayName }}</h3>
                <p>{{ item.campusName }} · {{ item.professionalName }}</p>
              </div>
            </div>

            <div class="badge-wall" v-if="item.badges?.length">
              <span
                v-for="b in item.badges"
                :key="b.badgeCode"
                class="honor-badge"
                :class="`tone-${b.badgeTone}`"
                :title="b.badgeDesc"
              >
                ✦ {{ b.badgeName }}
              </span>
            </div>

            <div class="podium-metrics xyf-tabular">
              <div class="m-cell">
                <strong>{{ Number(item.compositeInfluenceScore || 0).toFixed(1) }}</strong>
                <span>影响力指数</span>
              </div>
              <div class="m-cell">
                <strong>{{ item.completedOrderCount }} 笔</strong>
                <span>担保完单</span>
              </div>
              <div class="m-cell">
                <strong>{{ item.creditScore }}</strong>
                <span>履约信用分</span>
              </div>
              <div class="m-cell">
                <strong>{{ Number(item.totalPracticeHours || 0).toFixed(1) }}h</strong>
                <span>实践学时</span>
              </div>
            </div>
          </article>
        </section>

        <!-- Full Ranked Table / List -->
        <section class="rank-list-card xyf-surface-card">
          <div class="rank-list-head">
            <h2>校友创作者荣誉席位（共 {{ leaders.length }} 位认证导师）</h2>
            <span>点击创作者可调阅其公开数字信用档案、在架服务与历史成交评价</span>
          </div>

          <div class="rank-rows">
            <div
              v-for="row in leaders"
              :key="row.userId"
              class="rank-row"
              @click="router.push(`/creators/${row.userId}`)"
            >
              <div class="rank-num" :class="`is-top-${row.rank}`">
                {{ row.rank }}
              </div>

              <div class="rank-user">
                <XyfAvatar :src="row.avatar" :name="row.displayName" :size="46" />
                <div class="rank-user__meta">
                  <div class="name-line">
                    <strong>{{ row.displayName }}</strong>
                    <span class="level-tag">{{ row.creatorLevelTitle }}</span>
                    <span v-if="row.eduVerified === 1" class="edu-tag">教育邮箱已核验</span>
                  </div>
                  <div class="sub-line">
                    {{ row.campusName }} · {{ row.professionalName }}
                    <span v-if="row.topSkillTags?.length">
                      · 擅长：{{ row.topSkillTags.join(' / ') }}
                    </span>
                  </div>
                  <div class="badge-inline" v-if="row.badges?.length">
                    <span
                      v-for="b in row.badges"
                      :key="b.badgeCode"
                      class="mini-badge"
                      :class="`tone-${b.badgeTone}`"
                      :title="b.badgeDesc"
                    >
                      {{ b.badgeName }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="rank-stats xyf-tabular">
                <div class="stat-col">
                  <span class="s-val is-primary">{{ Number(row.compositeInfluenceScore || 0).toFixed(1) }}</span>
                  <span class="s-lbl">影响力指数</span>
                </div>
                <div class="stat-col">
                  <span class="s-val">{{ row.completedOrderCount }}</span>
                  <span class="s-lbl">完结担保单</span>
                </div>
                <div class="stat-col">
                  <span class="s-val">{{ row.creditScore }}</span>
                  <span class="s-lbl">履约信用分</span>
                </div>
                <div class="stat-col">
                  <span class="s-val">★ {{ Number(row.averageRating || 5).toFixed(1) }}</span>
                  <span class="s-lbl">综合口碑</span>
                </div>
              </div>

              <div class="rank-action" @click.stop>
                <el-button type="primary" plain size="small" @click="router.push(`/creators/${row.userId}`)">
                  查看数字主页
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

const filters = reactive({
  rankBy: 'COMPREHENSIVE',
  campusId: undefined as number | undefined,
  tagId: undefined as number | undefined
})

const topThree = computed(() => leaders.value.slice(0, 3))

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
.creator-leaderboard-page {
  padding: 28px 0 64px;
}

.leaderboard-hero {
  padding: 28px 32px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: #f8fafc;

  .hero-kicker {
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.1em;
    color: #34d399;
  }

  h1 {
    margin: 6px 0 8px;
    font-size: 26px;
    font-weight: 800;
    color: #ffffff;
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #cbd5e1;
    max-width: 780px;
    line-height: 1.6;
  }
}

.hero-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;

  .sub-filter-row {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.podium-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-bottom: 24px;

  @media (max-width: 920px) {
    grid-template-columns: 1fr;
  }
}

.podium-card {
  padding: 22px 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border-top: 4px solid #94a3b8;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.09);
  }

  &.is-rank-1 {
    border-top-color: #f59e0b;
    background: linear-gradient(180deg, #fffbeb 0%, #ffffff 45%);
  }
  &.is-rank-2 {
    border-top-color: #64748b;
  }
  &.is-rank-3 {
    border-top-color: #d97706;
  }
}

.podium-medal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;

  .medal-rank {
    font-size: 13px;
    font-weight: 800;
    padding: 3px 10px;
    border-radius: 999px;
    background: #0f172a;
    color: #ffffff;
  }

  .level-pill {
    font-size: 12px;
    font-weight: 700;
    color: #059669;
    background: #ecfdf5;
    padding: 3px 10px;
    border-radius: 999px;
  }
}

.podium-profile {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;

  h3 {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
  }

  p {
    margin: 0;
    font-size: 12.5px;
    color: #64748b;
  }
}

.badge-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
  min-height: 28px;
}

.honor-badge,
.mini-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;

  &.tone-gold {
    background: #fef3c7;
    color: #b45309;
  }
  &.tone-emerald {
    background: #d1fae5;
    color: #047857;
  }
  &.tone-blue {
    background: #dbeafe;
    color: #1d4ed8;
  }
  &.tone-purple {
    background: #f3e8ff;
    color: #7e22ce;
  }
}

.podium-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding-top: 14px;
  border-top: 1px dashed #e2e8f0;

  .m-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;

    strong {
      font-size: 15px;
      font-weight: 800;
      color: #0f172a;
    }

    span {
      font-size: 11px;
      color: #64748b;
    }
  }
}

.rank-list-card {
  padding: 24px 28px;
}

.rank-list-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
  }

  span {
    font-size: 12.5px;
    color: #64748b;
  }
}

.rank-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    border-color: #0f172a;
    background: #f8fafc;
  }

  @media (max-width: 768px) {
    flex-wrap: wrap;
  }
}

.rank-num {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #475569;
  font-weight: 800;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.is-top-1 {
    background: #f59e0b;
    color: #ffffff;
  }
  &.is-top-2 {
    background: #64748b;
    color: #ffffff;
  }
  &.is-top-3 {
    background: #d97706;
    color: #ffffff;
  }
}

.rank-user {
  flex: 1;
  min-width: 240px;
  display: flex;
  align-items: center;
  gap: 14px;

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .name-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    strong {
      font-size: 15.5px;
      color: #0f172a;
    }

    .level-tag {
      font-size: 11px;
      font-weight: 700;
      padding: 1px 8px;
      border-radius: 999px;
      background: #f1f5f9;
      color: #334155;
    }

    .edu-tag {
      font-size: 11px;
      font-weight: 600;
      padding: 1px 8px;
      border-radius: 999px;
      background: #ecfdf5;
      color: #047857;
    }
  }

  .sub-line {
    font-size: 12.5px;
    color: #64748b;
  }

  .badge-inline {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 2px;
  }
}

.rank-stats {
  display: flex;
  align-items: center;
  gap: 24px;

  .stat-col {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    min-width: 68px;

    .s-val {
      font-size: 16px;
      font-weight: 800;
      color: #0f172a;

      &.is-primary {
        color: #059669;
      }
    }

    .s-lbl {
      font-size: 11px;
      color: #64748b;
    }
  }
}
</style>
