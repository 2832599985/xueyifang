<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- Left Kinetic Visual Stage -->
      <AuthVisualStage
        headline="连接校园里的专业行家"
        subtitle="摄影、代码、设计与学业辅导，在可信担保中自由流转。"
      >
        <template #links>
          <router-link to="/services">先逛逛服务大厅</router-link>
          <span>·</span>
          <router-link to="/admin/login">管理员通道</router-link>
        </template>
      </AuthVisualStage>

      <!-- Right Clean Commercial Login Form -->
      <main class="auth-form-panel">
        <div class="auth-form-head">
          <h1>欢迎回来</h1>
          <p>使用校内学号登录你的学艺坊账号</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="学号" prop="studentId">
            <el-input
              v-model="form.studentId"
              placeholder="请输入学号（例如 20240003）"
              clearable
              autocomplete="username"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入登录密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-helpers">
            <el-checkbox v-model="rememberStudentId">记住学号</el-checkbox>
            <el-button text type="primary" @click="forgotDialogVisible = true">
              忘记密码？
            </el-button>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            立即登录
          </el-button>
        </el-form>

        <div class="auth-switch">
          <template v-if="registerConfigStore.registerEnabled">
            <span>还没有校园账号？</span>
            <router-link to="/register" class="switch-link">创建账号</router-link>
          </template>
          <template v-else>
            <span class="closed-tip">当前未开放自助注册，请联系管理员导入学籍账号</span>
          </template>
        </div>
      </main>
    </div>

    <!-- Password Help Modal -->
    <el-dialog v-model="forgotDialogVisible" title="找回密码说明" width="420px">
      <div class="forgot-dialog-body">
        <p>为保障校内实名学号安全，平台暂不支持邮件自助重置密码。</p>
        <p>若遗忘密码或首次由管理员批量导入学号，请联系平台管理员协助重置，或在「个人中心」登录后随时修改登录密码。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="forgotDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRegisterConfigStore } from '@/stores/registerConfig'
import {
  getRememberedStudentId,
  setRememberedStudentId,
  sanitizeRedirect
} from '@/utils/auth'
import { notifyFallbackError } from '@/api/request'
import AuthVisualStage from '@/components/auth/AuthVisualStage.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const registerConfigStore = useRegisterConfigStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const forgotDialogVisible = ref(false)

const savedStudentId = getRememberedStudentId()
const rememberStudentId = ref(Boolean(savedStudentId))

const form = reactive({
  studentId: savedStudentId,
  password: ''
})

const rules: FormRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value || loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login({
      studentId: form.studentId.trim(),
      password: form.password
    })

    if (rememberStudentId.value) {
      setRememberedStudentId(form.studentId.trim())
    } else {
      setRememberedStudentId('')
    }

    ElMessage.success('登录成功')
    const defaultHome = userStore.isAdmin ? '/admin/dashboard' : '/'
    const target = sanitizeRedirect(route.query.redirect as string, defaultHome)
    router.replace(target)
  } catch (err) {
    notifyFallbackError(err, '登录失败，请核对学号与密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  registerConfigStore.fetchRegisterStatus().catch(() => {})
})
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 14% 16%, rgba(217, 175, 118, 0.1), transparent 44%),
    radial-gradient(circle at 88% 84%, rgba(24, 24, 27, 0.05), transparent 42%),
    #f7f7f8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 20px;
}

.auth-shell {
  width: 100%;
  max-width: 980px;
  background: #ffffff;
  border: 1px solid var(--xyf-border);
  border-radius: 24px;
  box-shadow: 0 28px 64px -18px rgba(15, 23, 42, 0.14), 0 6px 18px -6px rgba(15, 23, 42, 0.06);
  display: grid;
  grid-template-columns: 1.02fr 1fr;
  overflow: hidden;
}

.auth-form-panel {
  padding: 52px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-form-head {
  margin-bottom: 30px;

  h1 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 750;
    letter-spacing: -0.025em;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 14px;
    color: var(--xyf-ink-2);
  }
}

.form-helpers {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 22px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
}

.auth-switch {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid var(--xyf-border-subtle);
  text-align: center;
  font-size: 13.5px;
  color: var(--xyf-ink-2);

  .switch-link {
    margin-left: 6px;
    font-weight: 600;
    color: var(--xyf-primary);

    &:hover {
      text-decoration: underline;
    }
  }

  .closed-tip {
    font-size: 12.5px;
    color: var(--xyf-ink-3);
  }
}

.forgot-dialog-body {
  p {
    margin: 0 0 10px;
    font-size: 13.5px;
    line-height: 1.7;
    color: var(--xyf-ink-2);

    &:last-child {
      margin-bottom: 0;
    }
  }
}

@media (max-width: 820px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .auth-shell :deep(.auth-stage) {
    display: none;
  }

  .auth-form-panel {
    padding: 36px 28px;
  }
}
</style>
