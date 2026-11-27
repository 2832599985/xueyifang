<template>
  <div class="service-form-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        title="编辑服务"
        :show-back="true"
        back-text="返回我的服务"
        back-to="/my-services"
      />

      <div v-if="loading" class="xyf-surface-card gate-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="!detail" class="xyf-surface-card gate-card">
        <XyfEmptyState
          title="无法加载该服务信息"
          description="请确认服务是否存在或您是否有权操作此服务。"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/my-services')">返回我的服务</el-button>
          </template>
        </XyfEmptyState>
      </div>

      <template v-else>
        <el-alert
          v-if="!canEditService(detail.status)"
          title="当前服务处于不可直接编辑状态"
          type="warning"
          show-icon
          :closable="false"
          description="仅「已下架」或「已驳回」的服务支持编辑修改，请先前往「我的服务」下架该服务。"
          style="margin-bottom: 18px"
        />

        <el-alert
          v-if="detail.reviewReason"
          :title="`上次审核意见：${detail.reviewReason}`"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: 18px"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="xyf-surface-card form-main-card"
        >
          <el-form-item label="服务标题" prop="serviceTitle">
            <el-input
              v-model="form.serviceTitle"
              maxlength="60"
              show-word-limit
              placeholder="服务标题"
            />
          </el-form-item>

          <div class="form-row-2">
            <el-form-item label="所属技能标签" prop="tagId">
              <el-select v-model="form.tagId" placeholder="请选择技能分类" style="width: 100%">
                <el-option
                  v-for="tag in dictStore.serviceTags || []"
                  :key="tag.tagId"
                  :label="tag.tagName"
                  :value="tag.tagId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="服务定价（元）" prop="price">
              <el-input-number
                v-model="form.price"
                :min="1"
                :max="99999"
                :precision="2"
                :step="5"
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <SmartPricingCompassCard
            :tag-id="form.tagId"
            :trade-type="form.tradeType"
            :proposed-price="form.price"
            :interactive="true"
            @select-price="(val: number) => (form.price = Number(val))"
          />

          <el-form-item label="服务详细描述" prop="serviceDescription">
            <el-input
              v-model="form.serviceDescription"
              type="textarea"
              :rows="7"
              maxlength="1500"
              show-word-limit
            />
          </el-form-item>

          <div class="form-row-2">
            <el-form-item label="交易方式" prop="tradeType">
              <el-radio-group v-model="form.tradeType">
                <el-radio-button :label="TradeType.ONLINE_ESCROW">线上担保交付</el-radio-button>
                <el-radio-button :label="TradeType.OFFLINE">校园线下交易</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="服务形态" prop="isPhysical">
              <el-radio-group v-model="form.isPhysical">
                <el-radio-button :label="PhysicalType.VIRTUAL">虚拟技能 / 数字交付</el-radio-button>
                <el-radio-button :label="PhysicalType.PHYSICAL">实物作品 / 耗材资料</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>

          <el-form-item
            v-if="form.tradeType === TradeType.OFFLINE"
            label="推荐校内交易地点（选填）"
            prop="tradeLocationId"
          >
            <el-select
              v-model="form.tradeLocationId"
              placeholder="选择校内便捷交接地点"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="loc in dictStore.tradeLocations"
                :key="loc.id"
                :label="`${loc.locationName}（${loc.locationAddress || '校内地点'}）`"
                :value="loc.id"
              />
            </el-select>
          </el-form-item>

          <div class="form-row-2">
            <el-form-item label="名额限制">
              <el-radio-group v-model="limitMode">
                <el-radio-button value="unlimited">不限名额</el-radio-button>
                <el-radio-button value="limited">限量接单</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="limitMode === 'limited'" label="最多可购买份数">
              <el-input-number v-model="limitCount" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </div>

          <el-form-item label="展示图片（首张作为列表封面）">
            <div class="image-uploader-panel" style="width: 100%">
              <div class="uploaded-list" v-if="imageList.length">
                <div v-for="(img, idx) in imageList" :key="img.raw + idx" class="uploaded-item">
                  <XyfImage :src="img.preview" width="136px" height="98px" :radius="8" fit="cover" />
                  <span v-if="idx === 0" class="cover-tag">封面主图</span>
                  <div class="uploaded-actions">
                    <button
                      v-if="idx > 0"
                      type="button"
                      class="action-link"
                      @click="setAsCover(idx)"
                    >
                      设为封面
                    </button>
                    <button type="button" class="action-link is-danger" @click="removeImage(idx)">
                      移除
                    </button>
                  </div>
                </div>
              </div>

              <el-upload
                v-if="imageList.length < 5"
                action="#"
                :show-file-list="false"
                :http-request="handleUploadImage"
                accept="image/jpeg,image/png,image/webp"
              >
                <el-button :loading="uploading" plain>
                  <el-icon><Plus /></el-icon>
                  新增展示图片 ({{ imageList.length }}/5)
                </el-button>
              </el-upload>
            </div>
          </el-form-item>

          <div class="submit-footer">
            <el-button @click="router.push('/my-services')">返回</el-button>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="!canEditService(detail.status)"
              @click="handleSubmit"
            >
              保存修改并提交审核
            </el-button>
          </div>
        </el-form>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useDictStore } from '@/stores/dict'
import { getServiceDetail, updateService, buildImageUrl } from '@/api/service'
import { uploadFile } from '@/api/file'
import { notifyFallbackError } from '@/api/request'
import {
  TradeType,
  PhysicalType,
  canEditService,
  type ServiceDetail
} from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import SmartPricingCompassCard from '@/components/service/SmartPricingCompassCard.vue'

interface UploadedImageItem {
  raw: string
  preview: string
}

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const serviceId = computed(() => Number(route.params.id))

const formRef = ref<FormInstance>()
const loading = ref(true)
const uploading = ref(false)
const submitting = ref(false)
const detail = ref<ServiceDetail | null>(null)
const limitMode = ref<'unlimited' | 'limited'>('unlimited')
const limitCount = ref(20)
const imageList = ref<UploadedImageItem[]>([])

const form = reactive({
  serviceTitle: '',
  serviceDescription: '',
  tagId: undefined as number | undefined,
  price: 30,
  tradeType: TradeType.ONLINE_ESCROW,
  tradeLocationId: undefined as number | undefined,
  isPhysical: PhysicalType.VIRTUAL
})

const rules: FormRules = {
  serviceTitle: [
    { required: true, message: '请输入服务标题', trigger: 'blur' },
    { min: 4, max: 60, message: '标题长度为 4-60 个字符', trigger: 'blur' }
  ],
  tagId: [{ required: true, message: '请选择技能分类', trigger: 'change' }],
  price: [{ required: true, message: '请设置服务价格', trigger: 'blur' }],
  serviceDescription: [
    { required: true, message: '请填写服务详细描述', trigger: 'blur' },
    { min: 10, max: 1500, message: '描述至少需要 10 个字', trigger: 'blur' }
  ]
}

async function handleUploadImage(options: UploadRequestOptions) {
  const file = options.file as File
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const url = await uploadFile(file, 'service_image')
    imageList.value.push({
      raw: url,
      preview: buildImageUrl(url)
    })
    ElMessage.success('图片上传成功')
  } catch (err) {
    notifyFallbackError(err, '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function setAsCover(index: number) {
  const [target] = imageList.value.splice(index, 1)
  if (target) {
    imageList.value.unshift(target)
    ElMessage.success('已设为封面主图')
  }
}

function removeImage(index: number) {
  imageList.value.splice(index, 1)
}

async function handleSubmit() {
  if (!formRef.value || submitting.value || !detail.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await updateService(detail.value.id, {
      serviceTitle: form.serviceTitle.trim(),
      serviceDescription: form.serviceDescription.trim(),
      tagId: form.tagId!,
      price: Number(form.price),
      tradeType: form.tradeType,
      tradeLocationId:
        form.tradeType === TradeType.OFFLINE ? form.tradeLocationId : undefined,
      maxPurchases: limitMode.value === 'unlimited' ? -1 : limitCount.value,
      isPhysical: form.isPhysical,
      images: imageList.value.map((i) => i.raw)
    })
    ElMessage.success('服务已更新，请等待管理员重新审核')
    router.replace('/my-services')
  } catch (err) {
    notifyFallbackError(err, '保存修改失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [svc] = await Promise.all([
      getServiceDetail(serviceId.value),
      dictStore.fetchServiceTags().catch(() => {}),
      dictStore.fetchTradeLocations().catch(() => {})
    ])
    detail.value = svc
    form.serviceTitle = svc.serviceTitle
    form.serviceDescription = svc.serviceDescription
    form.tagId = svc.tagId
    form.price = svc.price
    form.tradeType = svc.tradeType
    form.tradeLocationId = svc.tradeLocationId
    form.isPhysical = svc.isPhysical

    if (svc.maxPurchases === -1) {
      limitMode.value = 'unlimited'
    } else {
      limitMode.value = 'limited'
      limitCount.value = svc.maxPurchases || 1
    }

    imageList.value = (svc.images || []).map((img: string) => ({
      raw: img,
      preview: buildImageUrl(img)
    }))
  } catch (err) {
    notifyFallbackError(err, '获取服务详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.gate-card {
  padding: 28px;
}

.form-main-card {
  max-width: 860px;
  padding: 26px 28px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.image-uploader-panel {
  padding: 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-bg);
  border: 1px dashed var(--xyf-border);
}

.uploaded-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 14px;
}

.uploaded-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .cover-tag {
    position: absolute;
    top: 6px;
    left: 6px;
    background: var(--xyf-primary);
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: var(--xyf-radius-pill);
  }
}

.uploaded-actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;

  .action-link {
    border: none;
    background: transparent;
    padding: 0;
    font-size: 12px;
    color: var(--xyf-primary);
    cursor: pointer;

    &.is-danger {
      color: var(--xyf-danger);
    }
  }
}

.submit-footer {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--xyf-border-light);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 720px) {
  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .form-main-card {
    padding: 20px 16px;
  }
}
</style>
