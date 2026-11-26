<template>
  <el-dialog
    :model-value="visible"
    title="发表评价"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="review-form"
    >
      <el-form-item label="服务评分" prop="rating">
        <div class="rating-row">
          <el-rate
            v-model="form.rating"
            :max="5"
            :colors="['#F59E0B', '#F59E0B', '#F59E0B']"
            void-color="#E3DFD5"
            show-text
            :texts="['非常差', '较差', '一般', '较好', '非常好']"
          />
        </div>
      </el-form-item>

      <el-form-item label="评价内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请客观描述服务质量、沟通与交付体验（至少10个字）"
        />
      </el-form-item>

      <el-form-item class="anonymous-row">
        <el-checkbox v-model="form.anonymous">
          匿名发表评价
          <span class="anonymous-tip">（评价列表将隐藏您的昵称与头像）</span>
        </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交评价
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createReview } from '@/api/review'
import { notifyFallbackError } from '@/api/request'
import type { ServiceReviewCreateRequest } from '@/types/review'

interface Props {
  orderId: number
  visible: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  rating: 5,
  content: '',
  anonymous: false
})

const rules: FormRules = {
  rating: [
    { required: true, message: '请为本次服务打分', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || value < 1 || value > 5) {
          callback(new Error('请选择1-5星评分'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const len = (value || '').trim().length
        if (len < 10) {
          callback(new Error('评价内容至少10个字'))
        } else if (len > 500) {
          callback(new Error('评价内容不能超过500字'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const resetForm = () => {
  form.rating = 5
  form.content = ''
  form.anonymous = false
  formRef.value?.clearValidate()
}

const handleClose = () => {
  emit('update:visible', false)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data: ServiceReviewCreateRequest = {
      orderId: props.orderId,
      rating: form.rating,
      content: form.content.trim(),
      anonymous: form.anonymous
    }
    await createReview(data)
    ElMessage.success('评价提交成功')
    emit('success')
    emit('update:visible', false)
  } catch (error: any) {
    notifyFallbackError(error, '提交评价失败')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) resetForm()
  }
)
</script>

<style scoped lang="scss">
.rating-row {
  padding: 6px 0;

  :deep(.el-rate__icon) {
    font-size: 24px;
  }

  :deep(.el-rate__text) {
    color: var(--xyf-primary);
    font-weight: 600;
    margin-left: 10px;
  }
}

.anonymous-row {
  margin-bottom: 0;

  .anonymous-tip {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
