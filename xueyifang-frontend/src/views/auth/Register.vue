<template>
  <div class="auth-page">
    <div class="auth-shell">
      <AuthVisualStage
        headline="加入校园技能协作网络"
        subtitle="一次实名注册，即可自由预约服务或发布你的个人专长。"
      >
        <template #links>
          <router-link to="/login">已有账号？去登录</router-link>
          <span>·</span>
          <router-link to="/">返回首页</router-link>
        </template>
      </AuthVisualStage>

      <main class="auth-form-panel">
        <div class="auth-form-head">
          <h1>创建学艺坊账号</h1>
          <p>请填写您的校内资料完成实名注册</p>
        </div>

        <div v-if="statusChecked && !registerConfigStore.registerEnabled" class="closed-notice">
          <el-alert
            title="管理员暂未开放自助注册"
            type="warning"
            :closable="false"
            show-icon
            description="当前系统关闭了公开注册通道，如需开户请联系管理员通过后台学籍导入开通，或直接返回登录。"
          />
          <el-button type="primary" class="closed-back-btn" @click="router.push('/login')">
            返回登录页
          </el-button>
        </div>

        <el-form
          v-else
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
        >
          <div class="form-row-2">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="form.studentId" placeholder="例如 20241008" clearable />
            </el-form-item>

            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入校内姓名" clearable />
            </el-form-item>
          </div>

          <div class="form-row-2">
            <el-form-item label="所属专业" prop="professionalId">
              <el-select
                v-model="form.professionalId"
                placeholder="请选择所属专业"
                :loading="dictLoading"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in dictStore.professionals || []"
                  :key="item.id"
                  :label="item.professionalName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入 11 位手机号" clearable />
            </el-form-item>
          </div>

          <div class="form-row-2">
            <el-form-item label="联系邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@campus.edu.cn" clearable />
            </el-form-item>

            <el-form-item label="宿舍楼栋" prop="dormitory">
              <el-input v-model="form.dormitory" placeholder="例如 桃园5舍" clearable />
            </el-form-item>
          </div>

          <div class="form-row-2">
            <el-form-item label="设置密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="8-16位，需包含字母和数字"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
              />
            </el-form-item>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="handleRegister"
          >
            提交注册
          </el-button>
        </el-form>

        <div class="auth-switch">
          <span>已经拥有学艺坊账号？</span>
          <router-link to="/login" class="switch-link">直接登录</router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useDictStore } from '@/stores/dict'
import { useRegisterConfigStore } from '@/stores/registerConfig'
import { setRememberedStudentId } from '@/utils/auth'
import { notifyFallbackError } from '@/api/request'
import AuthVisualStage from '@/components/auth/AuthVisualStage.vue'

const router = useRouter()
const dictStore = useDictStore()
const registerConfigStore = useRegisterConfigStore()

const formRef = ref<FormInstance>()
const dictLoading = ref(false)
const submitting = ref(false)
const statusChecked = ref(false)

const form = reactive({
  studentId: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: '',
  professionalId: undefined as number | undefined,
  email: '',
  dormitory: ''
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { min: 6, max: 20, message: '学号长度应在 6-20 位之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '真实姓名长度应在 2-20 位之间', trigger: 'blur' }
  ],
  professionalId: [{ required: true, message: '请选择所属专业', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[0-9])(?=.*[a-zA-Z]).{8,16}$/,
      message: '密码必须为 8-16 位，且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

async function handleRegister() {
  if (!formRef.value || submitting.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await authApi.register({
      studentId: form.studentId.trim(),
      realName: form.realName.trim(),
      phone: form.phone.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      professionalId: form.professionalId!,
      email: form.email.trim(),
      dormitory: form.dormitory.trim()
    })

    setRememberedStudentId(form.studentId.trim())
    ElMessage.success('注册成功，请登录您的学艺坊账号')
    router.replace('/login')
  } catch (err) {
    notifyFallbackError(err, '注册失败，请检查填写内容')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  dictLoading.value = true
  try {
    await Promise.all([
      registerConfigStore.fetchRegisterStatus().catch(() => {}),
      dictStore.fetchProfessionals().catch(() => {})
    ])
  } finally {
    statusChecked.value = true
    dictLoading.value = false
  }
})
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 15%, rgba(217, 175, 118, 0.1), transparent 45%),
    radial-gradient(circle at 90% 85%, rgba(24, 24, 27, 0.05), transparent 40%),
    #f7f7f8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 20px;
}

.auth-shell {
  width: 100%;
  max-width: 1040px;
  background: var(--xyf-surface);
  border: 1px solid var(--xyf-border);
  border-radius: var(--xyf-radius-xl);
  box-shadow: var(--xyf-shadow-lg);
  display: grid;
  grid-template-columns: 0.9fr 1.25fr;
  overflow: hidden;
}

.auth-brand-panel {
  background: var(--xyf-sidebar-bg);
  color: #f8fafc;
  padding: 40px 32px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 24px;
}

.brand-copy {
  h2 {
    margin: 0 0 10px;
    font-size: 22px;
    line-height: 1.35;
    font-weight: 700;
    color: #ffffff;
  }

  p {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.68;
    color: rgba(248, 250, 252, 0.72);
  }
}

.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;

  li {
    display: flex;
    gap: 12px;
    align-items: flex-start;

    .point-dot {
      width: 8px;
      height: 8px;
      border-radius: 999px;
      background: #2dd4bf;
      margin-top: 7px;
      flex-shrink: 0;
    }

    strong {
      display: block;
      font-size: 14px;
      color: #ffffff;
      margin-bottom: 2px;
    }

    p {
      margin: 0;
      font-size: 12.5px;
      color: rgba(248, 250, 252, 0.65);
    }
  }
}

.brand-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
  color: rgba(248, 250, 252, 0.55);

  a {
    color: #99f6e4;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.auth-form-panel {
  padding: 36px 38px;
}

.auth-form-head {
  margin-bottom: 22px;

  h1 {
    margin: 6px 0 4px;
    font-size: 25px;
    font-weight: 700;
    color: var(--xyf-ink);
  }

  p {
    margin: 0;
    font-size: 13.5px;
    color: var(--xyf-ink-muted);
  }
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 6px;
}

.closed-notice {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 0;

  .closed-back-btn {
    align-self: flex-start;
  }
}

.auth-switch {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--xyf-border-light);
  text-align: center;
  font-size: 13.5px;
  color: var(--xyf-ink-muted);

  .switch-link {
    margin-left: 6px;
    color: var(--xyf-primary);
    font-weight: 600;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-brand-panel {
    padding: 24px;
    .brand-points {
      display: none;
    }
  }

  .auth-form-panel {
    padding: 28px 22px;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
