<template>
  <div class="admin-login-page">
    <div class="admin-login-card">
      <div class="admin-brand-head">
        <XyfBrandLogo
          theme="light"
          title="学艺坊 · 管理控制台"
          subtitle="Platform Governance & Moderation"
        />
      </div>

      <div class="admin-login-body">
        <h1>管理员控制台</h1>
        <p class="subtitle">请使用校内运营与审核管理员账号登录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="管理员学号 / 账号" prop="studentId">
            <el-input
              v-model="form.studentId"
              placeholder="请输入管理员学号"
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
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            进入管理控制台
          </el-button>
        </el-form>

        <div class="back-front">
          <router-link to="/">返回校园前台首页</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { sanitizeRedirect } from '@/utils/auth'
import { notifyFallbackError } from '@/api/request'
import XyfBrandLogo from '@/components/ui/XyfBrandLogo.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  studentId: '',
  password: ''
})

const rules: FormRules = {
  studentId: [{ required: true, message: '请输入管理员学号', trigger: 'blur' }],
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

    if (!userStore.isAdmin) {
      ElMessage.error('当前账号不具备管理员权限')
      userStore.logout()
      return
    }

    ElMessage.success('管理员登录成功')
    const target = sanitizeRedirect(route.query.redirect as string, '/admin/dashboard')
    router.replace(target.startsWith('/admin') ? target : '/admin/dashboard')
  } catch (err) {
    notifyFallbackError(err, '管理员登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  background: #0f0f12;
  background-image:
    radial-gradient(circle at 20% 20%, rgba(229, 199, 158, 0.14), transparent 45%),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.06), transparent 45%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.admin-login-card {
  width: 100%;
  max-width: 460px;
  border-radius: var(--xyf-radius-xl);
  overflow: hidden;
  background: var(--xyf-surface);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: var(--xyf-shadow-lg);
}

.admin-brand-head {
  background: #111113;
  padding: 24px 28px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-login-body {
  padding: 32px 30px;

  h1 {
    margin: 6px 0 6px;
    font-size: 24px;
    font-weight: 800;
    color: var(--xyf-ink);
  }

  .subtitle {
    margin: 0 0 24px;
    font-size: 13px;
    color: var(--xyf-ink-muted);
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 8px;
}

.back-front {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;

  a {
    color: var(--xyf-ink-muted);
    text-decoration: none;

    &:hover {
      color: var(--xyf-primary);
      text-decoration: underline;
    }
  }
}
</style>
