<template>
  <div class="user-center-page xyf-page">
    <div class="xyf-container">
      <header class="center-head">
        <div>
          <h1>个人中心</h1>
          <p>管理你的校内实名资料、创作者认证与账号安全设置</p>
        </div>
        <div class="center-head__actions">
          <button type="button" class="btn-secondary" @click="router.push('/wallet')">
            我的钱包
          </button>
          <button
            v-if="userStore.hasPublishPermission"
            type="button"
            class="btn-primary"
            @click="router.push('/service/publish')"
          >
            发布新服务
          </button>
        </div>
      </header>

      <div class="center-layout">
        <!-- Left Sidebar Profile Card -->
        <aside class="center-sidebar">
          <div class="xyf-surface-card profile-card">
            <div class="avatar-box">
              <XyfAvatar
                :src="profileForm.avatar || userStore.userInfo?.avatar"
                :name="userStore.displayName"
                :size="76"
              />
              <el-upload
                action="#"
                :show-file-list="false"
                :http-request="handleAvatarUpload"
                accept="image/jpeg,image/png,image/webp"
              >
                <el-button size="small" plain :loading="uploadingAvatar" class="avatar-upload-btn">
                  更换头像
                </el-button>
              </el-upload>
            </div>

            <div class="profile-name-row">
              <h2 class="profile-name">{{ userStore.displayName || '同学' }}</h2>
              <el-tooltip
                :content="userStore.hasPublishPermission ? '已开通服务发布权限 · 认证创作者' : '未开通服务发布权限，点击前往申请'"
                placement="top"
              >
                <button
                  type="button"
                  class="creator-seal-btn"
                  :class="{ 'is-verified': userStore.hasPublishPermission }"
                  :aria-label="userStore.hasPublishPermission ? '已开通服务发布权限' : '未开通服务发布权限'"
                  @click="switchTab('permission')"
                >
                  <svg v-if="userStore.hasPublishPermission" viewBox="0 0 20 20" fill="none">
                    <circle cx="10" cy="10" r="9" fill="#18181B" />
                    <path d="M6.4 10.2L8.9 12.6L13.8 7.6" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <svg v-else viewBox="0 0 20 20" fill="none">
                    <circle cx="10" cy="10" r="8.2" stroke="#A1A1AA" stroke-width="1.4" stroke-dasharray="2.5 2.5" />
                    <path d="M10 6.8V13.2M6.8 10H13.2" stroke="#71717A" stroke-width="1.4" stroke-linecap="round" />
                  </svg>
                </button>
              </el-tooltip>
            </div>
            <div class="profile-student-id xyf-mono">
              学号 {{ userStore.userInfo?.studentId || '-' }}
              <span v-if="userStore.userInfo?.realName && userStore.userInfo?.nickname">
                · {{ userStore.userInfo.realName }}
              </span>
            </div>

            <nav class="side-nav" aria-label="个人中心菜单">
              <button
                type="button"
                class="side-nav__item"
                :class="{ 'is-active': activeTab === 'profile' }"
                @click="switchTab('profile')"
              >
                <el-icon><User /></el-icon>
                <span>基本资料</span>
              </button>
              <button
                type="button"
                class="side-nav__item"
                :class="{ 'is-active': activeTab === 'permission' }"
                @click="switchTab('permission')"
              >
                <el-icon><Stamp /></el-icon>
                <span>发布权限认证</span>
              </button>
              <button
                type="button"
                class="side-nav__item"
                :class="{ 'is-active': activeTab === 'security' }"
                @click="switchTab('security')"
              >
                <el-icon><Lock /></el-icon>
                <span>安全与密码</span>
              </button>
            </nav>

            <div class="side-shortcuts">
              <router-link to="/orders">我买到的订单</router-link>
              <router-link to="/orders/sales">收到的销售单</router-link>
              <router-link to="/favorites">我的收藏</router-link>
              <router-link to="/disputes">纠纷记录</router-link>
            </div>
          </div>
        </aside>

        <!-- Main Tab Content -->
        <main class="center-main">
          <!-- Tab 1: Basic Profile -->
          <section v-if="activeTab === 'profile'" class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>个人基本资料</h2>
            </div>

            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
              class="profile-form"
            >
              <div class="form-row-2">
                <el-form-item label="展示昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" maxlength="20" placeholder="设置个性昵称" />
                </el-form-item>

                <el-form-item label="所属学院与专业" prop="professionalId">
                  <el-select
                    v-model="profileForm.professionalId"
                    filterable
                    style="width: 100%"
                    placeholder="请选择所属专业"
                  >
                    <el-option
                      v-for="item in dictStore.professionals || []"
                      :key="item.id"
                      :label="item.professionalName"
                      :value="item.id"
                    />
                  </el-select>
                  <div class="field-hint">
                    注意：专业信息每次修改存在 30 天冷却期，请核实后再保存。
                  </div>
                </el-form-item>
              </div>

              <div class="form-row-2">
                <el-form-item label="联系电话" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="手机号" />
                </el-form-item>

                <el-form-item label="联系邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="name@campus.edu.cn" />
                </el-form-item>
              </div>

              <div class="form-row-2">
                <el-form-item label="年级" prop="grade">
                  <el-select
                    v-model="profileForm.grade"
                    placeholder="请选择所在年级"
                    clearable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="g in gradeOptions"
                      :key="g"
                      :label="g"
                      :value="g"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="宿舍区 / 楼栋" prop="dormitory">
                  <el-input v-model="profileForm.dormitory" placeholder="例如 紫荆公寓 2号楼" clearable />
                </el-form-item>
              </div>

              <el-form-item label="个人技能简介" prop="bio">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="4"
                  maxlength="200"
                  show-word-limit
                  placeholder="介绍你的学习背景、擅长领域或接单时间偏好（最多 200 字）..."
                />
              </el-form-item>

              <div class="form-footer">
                <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">
                  保存资料修改
                </el-button>
              </div>
            </el-form>
          </section>

          <!-- Tab 2: Publish Permission -->
          <section v-else-if="activeTab === 'permission'" class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>技能发布权限认证</h2>
            </div>

            <div v-if="permissionLoading">
              <el-skeleton :rows="4" animated />
            </div>

            <template v-else>
              <div class="perm-status-box">
                <div class="perm-status-left">
                  <div class="perm-seal" :class="{ 'is-verified': userStore.hasPublishPermission }">
                    <svg v-if="userStore.hasPublishPermission" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="10" fill="#18181B" />
                      <path d="M7.8 12.2L10.7 15.1L16.5 9.2" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="9.5" stroke="#A1A1AA" stroke-width="1.6" stroke-dasharray="3 3" />
                      <circle cx="12" cy="12" r="3" fill="#71717A" />
                    </svg>
                  </div>
                  <div>
                    <span class="perm-label">当前认证状态</span>
                    <div class="perm-state-text">{{ permissionStatusLabel }}</div>
                  </div>
                </div>
                <el-button
                  v-if="userStore.hasPublishPermission"
                  type="primary"
                  @click="router.push('/service/publish')"
                >
                  立即发布新服务
                </el-button>
              </div>

              <el-alert
                v-if="permissionInfo?.lastReviewReason && permissionInfo.status === 'rejected'"
                :title="`上次驳回原因：${permissionInfo.lastReviewReason}`"
                type="error"
                show-icon
                :closable="false"
                style="margin-bottom: 18px"
              />

              <el-alert
                v-if="permissionInfo?.status === 'pending'"
                title="您的申请正在等待管理员审核，审核结果将通过站内通知推送给您。"
                type="warning"
                show-icon
                :closable="false"
                style="margin-bottom: 18px"
              />

              <el-form
                v-if="!userStore.hasPublishPermission && permissionInfo?.status !== 'pending'"
                label-position="top"
              >
                <el-form-item label="申请技能方向与优势说明（至少 10 字）">
                  <el-input
                    v-model="applyReason"
                    type="textarea"
                    :rows="5"
                    maxlength="500"
                    show-word-limit
                    placeholder="请描述你计划发布的技能类型、相关课程/竞赛/作品经验及可交付承诺..."
                  />
                </el-form-item>
                <el-button
                  type="primary"
                  :loading="applyingPermission"
                  @click="handleApplyPermission"
                >
                  提交发布权限申请
                </el-button>
              </el-form>
            </template>
          </section>

          <!-- Tab 3: Security & Password -->
          <section v-else class="xyf-surface-card panel-card">
            <div class="xyf-section-heading">
              <h2>账号安全与密码修改</h2>
            </div>

            <el-form
              ref="pwdFormRef"
              :model="pwdForm"
              :rules="pwdRules"
              label-position="top"
              style="max-width: 460px"
            >
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="pwdForm.newPassword"
                  type="password"
                  show-password
                  placeholder="8-16位，需同时包含字母和数字"
                />
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
              </el-form-item>

              <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">
                确认修改密码
              </el-button>
            </el-form>
          </section>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions
} from 'element-plus'
import { User, Stamp, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { authApi } from '@/api/auth'
import { uploadFile } from '@/api/file'
import { getPermissionStatus, applyPermission } from '@/api/permission'
import { notifyFallbackError } from '@/api/request'
import { PERMISSION_REVIEW_STATUS, type PermissionStatus } from '@/types/permission'
import XyfAvatar from '@/components/ui/XyfAvatar.vue'

type CenterTab = 'profile' | 'permission' | 'security'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dictStore = useDictStore()

const activeTab = ref<CenterTab>('profile')
const uploadingAvatar = ref(false)
const savingProfile = ref(false)
const permissionLoading = ref(false)
const applyingPermission = ref(false)
const savingPwd = ref(false)
const permissionInfo = ref<PermissionStatus | null>(null)
const applyReason = ref('')
const originalProfessionalId = ref<number | undefined>(undefined)

const profileFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()

const profileForm = reactive({
  nickname: '',
  avatar: '',
  professionalId: undefined as number | undefined,
  phone: '',
  email: '',
  grade: '',
  dormitory: '',
  bio: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const gradeOptions = ['大一', '大二', '大三', '大四', '研一', '研二', '研三']

const profileRules: FormRules = {
  nickname: [
    { min: 2, max: 20, message: '昵称长度应在 2-20 个字符之间', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[0-9])(?=.*[a-zA-Z]).{8,16}$/,
      message: '新密码必须为 8-16 位，且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (_r: unknown, val: string, cb: (err?: Error) => void) => {
        if (!val) cb(new Error('请再次输入新密码'))
        else if (val !== pwdForm.newPassword) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const permissionStatusLabel = computed(() => {
  if (userStore.hasPublishPermission) return '已通过认证 · 可发布服务'
  const st = permissionInfo.value?.status
  const reviewSt = permissionInfo.value?.permissionReviewStatus
  if (st === 'pending' || reviewSt === PERMISSION_REVIEW_STATUS.PENDING) {
    return '审核中 · 等待管理员复核'
  }
  if (st === 'approved' || reviewSt === PERMISSION_REVIEW_STATUS.APPROVED) {
    return '已通过认证'
  }
  if (st === 'rejected' || reviewSt === PERMISSION_REVIEW_STATUS.REJECTED) {
    return '未通过审核 · 可修正后重提'
  }
  return permissionInfo.value?.statusText || '暂未申请发布权限'
})

function switchTab(tab: CenterTab) {
  activeTab.value = tab
  router.replace({ path: '/user/center', query: { tab } })
}

function hydrateProfileForm() {
  const u = userStore.userInfo
  if (!u) return
  profileForm.nickname = u.nickname || u.realName || ''
  profileForm.avatar = u.avatar || ''
  profileForm.professionalId = u.professionalId
  originalProfessionalId.value = u.professionalId
  profileForm.phone = u.phone || ''
  profileForm.email = u.email || ''
  profileForm.grade = u.grade || ''
  profileForm.dormitory = u.dormitory || ''
  profileForm.bio = u.bio || ''
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  const file = options.file as File
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }
  uploadingAvatar.value = true
  try {
    const url = await uploadFile(file, 'user_avatar')
    profileForm.avatar = url
    await authApi.updateProfile({ avatar: url })
    await userStore.fetchCurrentUser()
    ElMessage.success('头像已更新')
  } catch (err) {
    notifyFallbackError(err, '更换头像失败')
  } finally {
    uploadingAvatar.value = false
  }
}

async function handleSaveProfile() {
  if (!profileFormRef.value || savingProfile.value) return
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }

  const changedMajor =
    profileForm.professionalId !== undefined &&
    profileForm.professionalId !== originalProfessionalId.value

  if (changedMajor) {
    try {
      await ElMessageBox.confirm(
        '专业信息变更后 30 天内将无法再次修改，确认保存本次专业调整吗？',
        '修改专业确认',
        { confirmButtonText: '确认修改', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }

  savingProfile.value = true
  try {
    await authApi.updateProfile({
      nickname: profileForm.nickname.trim() || undefined,
      avatar: profileForm.avatar || undefined,
      professionalId: changedMajor ? profileForm.professionalId : undefined,
      phone: profileForm.phone.trim() || undefined,
      email: profileForm.email.trim() || undefined,
      grade: profileForm.grade.trim() || undefined,
      dormitory: profileForm.dormitory.trim() || undefined,
      bio: profileForm.bio.trim() || undefined
    })
    await userStore.fetchCurrentUser()
    hydrateProfileForm()
    ElMessage.success('个人资料已保存')
  } catch (err) {
    notifyFallbackError(err, '保存个人资料失败')
  } finally {
    savingProfile.value = false
  }
}

async function fetchPermission() {
  permissionLoading.value = true
  try {
    permissionInfo.value = await getPermissionStatus()
  } catch {
    permissionInfo.value = null
  } finally {
    permissionLoading.value = false
  }
}

async function handleApplyPermission() {
  if (applyReason.value.trim().length < 10) {
    ElMessage.warning('请填写至少 10 个字的技能说明与申请理由')
    return
  }
  applyingPermission.value = true
  try {
    await applyPermission({ reason: applyReason.value.trim() })
    ElMessage.success('发布权限申请已提交，请耐心等待管理员审核')
    applyReason.value = ''
    await fetchPermission()
  } catch (err) {
    notifyFallbackError(err, '提交申请失败')
  } finally {
    applyingPermission.value = false
  }
}

async function handleChangePassword() {
  if (!pwdFormRef.value || savingPwd.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }

  savingPwd.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err) {
    notifyFallbackError(err, '修改密码失败')
  } finally {
    savingPwd.value = false
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'permission' || tab === 'security' || tab === 'profile') {
      activeTab.value = tab
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await Promise.all([
    userStore.ensureUserInfo().catch(() => {}),
    dictStore.fetchProfessionals().catch(() => {}),
    fetchPermission()
  ])
  hydrateProfileForm()
})
</script>

<style scoped lang="scss">
.user-center-page {
  padding-top: 36px;
  padding-bottom: 72px;
}

.center-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 28px;

  h1 {
    margin: 0;
    font-size: 30px;
    font-weight: 750;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
    line-height: 1.2;
  }

  p {
    margin: 8px 0 0;
    font-size: 14px;
    color: var(--xyf-ink-3);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.btn-primary {
  height: 40px;
  padding: 0 20px;
  border-radius: 999px;
  border: 1px solid #18181b;
  background: #18181b;
  color: #ffffff;
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    background: #27272a;
    border-color: #27272a;
    transform: translateY(-1px);
  }
}

.btn-secondary {
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid var(--xyf-border);
  background: #ffffff;
  color: var(--xyf-ink);
  font-size: 13.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--xyf-transition);

  &:hover {
    border-color: #a1a1aa;
    background: var(--xyf-surface-soft);
  }
}

.center-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.profile-card {
  padding: 26px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  border-radius: 18px;
}

.avatar-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.profile-name-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 4px;
}

.profile-name {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
  color: var(--xyf-ink);
}

.creator-seal-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: transform var(--xyf-transition);

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    transform: scale(1.1);
  }
}

.profile-student-id {
  font-size: 12.5px;
  color: var(--xyf-ink-3);
  margin-bottom: 18px;
}

.side-nav {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 16px;
  border-top: 1px solid var(--xyf-border-light);

  &__item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 14px;
    border: none;
    border-radius: var(--xyf-radius-sm);
    background: transparent;
    color: var(--xyf-ink-secondary);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    text-align: left;
    transition: all var(--xyf-transition);

    &:hover {
      background: var(--xyf-bg-muted);
      color: var(--xyf-ink);
    }

    &.is-active {
      background: var(--xyf-primary-soft);
      color: var(--xyf-primary-dark);
      font-weight: 650;
    }
  }
}

.side-shortcuts {
  width: 100%;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border-light);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;

  a {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
    text-decoration: none;
    padding: 6px 8px;
    border-radius: var(--xyf-radius-xs);
    background: var(--xyf-bg);

    &:hover {
      color: var(--xyf-primary);
      background: var(--xyf-primary-soft);
    }
  }
}

.panel-card {
  padding: 24px 28px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.perm-status-box {
  padding: 20px 22px;
  border-radius: 14px;
  background: var(--xyf-surface-soft);
  border: 1px solid var(--xyf-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;

  .perm-status-left {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .perm-seal {
    width: 36px;
    height: 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    svg {
      width: 28px;
      height: 28px;
    }
  }

  .perm-label {
    display: block;
    font-size: 12px;
    color: var(--xyf-ink-3);
    margin-bottom: 3px;
  }

  .perm-state-text {
    font-size: 15px;
    font-weight: 650;
    color: var(--xyf-ink);
  }
}

@media (max-width: 900px) {
  .center-layout {
    grid-template-columns: 1fr;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
