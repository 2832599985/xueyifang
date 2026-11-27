<template>
  <div class="editor-layout">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="xyf-surface-card editor-main-card"
    >
      <div class="xyf-section-heading">
        <h2>基础信息与定价</h2>
      </div>

      <el-form-item label="服务标题" prop="serviceTitle">
        <el-input
          v-model="form.serviceTitle"
          placeholder="清晰概括你的交付内容，例如：Vue3 / SpringBoot 毕设功能定制与调试"
          maxlength="60"
          show-word-limit
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

      <div class="xyf-section-heading section-gap">
        <h2>服务详情与交付说明</h2>
      </div>

      <div v-if="showTemplates" class="template-bar">
        <span class="template-bar__label">快速插入模版：</span>
        <button
          v-for="tpl in descriptionTemplates"
          :key="tpl.name"
          type="button"
          class="xyf-chip template-chip"
          @click="applyTemplate(tpl.content)"
        >
          {{ tpl.name }}
        </button>
      </div>

      <el-form-item label="详细说明" prop="serviceDescription">
        <el-input
          v-model="form.serviceDescription"
          type="textarea"
          :rows="7"
          maxlength="1500"
          show-word-limit
          placeholder="请写明包含内容、交付周期、需买家准备的材料与修改边界..."
        />
      </el-form-item>

      <div class="xyf-section-heading section-gap">
        <h2>交易方式与库存配置</h2>
      </div>

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

      <!-- Visual Campus Landmark Picker (filtered by available & current campus when matched) -->
      <el-form-item
        v-if="form.tradeType === TradeType.OFFLINE"
        label="推荐校内面交地标（选填，方便买家下单定位）"
        prop="tradeLocationId"
      >
        <div class="landmark-picker">
          <div class="landmark-grid">
            <button
              v-for="loc in availableLocations"
              :key="loc.id"
              type="button"
              class="landmark-card"
              :class="{ 'is-selected': form.tradeLocationId === loc.id }"
              @click="toggleLocation(loc.id)"
            >
              <div class="landmark-card__top">
                <strong class="ellipsis">{{ loc.locationName }}</strong>
                <span v-if="loc.hasCctv === 1" class="cctv-tag">监控覆盖</span>
              </div>
              <p class="landmark-card__addr ellipsis">
                {{ loc.locationAddress || loc.locationDescription || '校内指定核销点' }}
              </p>
              <span v-if="loc.validRadiusMeters" class="landmark-card__meta xyf-tabular">
                核销半径 {{ loc.validRadiusMeters }}m
              </span>
            </button>
          </div>

          <div
            v-if="selectedLocation?.latitude && selectedLocation?.longitude"
            class="landmark-map-preview"
          >
            <RealCampusMap
              :latitude="selectedLocation.latitude"
              :longitude="selectedLocation.longitude"
              :center-lat="selectedLocation.latitude"
              :center-lng="selectedLocation.longitude"
              :radius-meters="selectedLocation.validRadiusMeters || 80"
              :interactive="false"
              :height="160"
            />
          </div>
        </div>
      </el-form-item>

      <div class="form-row-2">
        <el-form-item label="名额限制">
          <el-radio-group v-model="limitMode">
            <el-radio-button value="unlimited">不限名额</el-radio-button>
            <el-radio-button value="limited">限量接单</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="limitMode === 'limited'" label="最多可购买份数" prop="maxPurchases">
          <el-input-number v-model="limitCount" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
      </div>

      <div class="xyf-section-heading section-gap">
        <h2>展示图片（最多 5 张，首张为封面）</h2>
      </div>

      <div class="image-uploader-panel">
        <div class="uploaded-list" v-if="imageList.length">
          <div v-for="(img, idx) in imageList" :key="img.raw + idx" class="uploaded-item">
            <XyfImage :src="img.preview" width="136px" height="98px" :radius="10" fit="cover" />
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
            上传展示图片 ({{ imageList.length }}/5)
          </el-button>
        </el-upload>
        <p class="upload-tip">建议上传 4:3 横图或作品实拍截图，单张不超过 5MB，支持 JPG / PNG / WebP。</p>
      </div>

      <div class="submit-footer">
        <el-button @click="emit('cancel')">取消</el-button>
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="submitDisabled"
          @click="handleSubmit"
        >
          {{ submitText }}
        </el-button>
      </div>
    </el-form>

    <!-- Sticky Right Live Card Preview & Honest Settlement Note -->
    <aside class="editor-preview-aside">
      <div class="xyf-surface-card preview-shell">
        <div class="preview-shell__head">
          <span>列表卡片实时预览</span>
          <span v-if="missingCount > 0" class="missing-hint xyf-tabular">
            还差 {{ missingCount }} 项必填
          </span>
          <span v-else class="ready-hint">已可提交</span>
        </div>

        <div class="preview-card-stage">
          <ServiceCard :service="previewService" :lazy="false" />
        </div>

        <dl class="settlement-summary xyf-tabular">
          <div class="settlement-row">
            <dt>标价</dt>
            <dd>¥{{ Number(form.price || 0).toFixed(2) }}</dd>
          </div>
          <div class="settlement-row">
            <dt>平台服务费</dt>
            <dd class="is-muted">按结算时规则扣除</dd>
          </div>
          <div class="settlement-row">
            <dt>收款方式</dt>
            <dd class="is-muted">买家验收后自动入账钱包</dd>
          </div>
        </dl>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { buildImageUrl } from '@/api/service'
import { uploadFile } from '@/api/file'
import { notifyFallbackError } from '@/api/request'
import {
  TradeType,
  PhysicalType,
  ServiceStatus,
  type ServiceFormData,
  type ServiceListItem
} from '@/types/service'
import XyfImage from '@/components/ui/XyfImage.vue'
import ServiceCard from '@/components/business/ServiceCard.vue'
import RealCampusMap from '@/components/business/RealCampusMap.vue'
import SmartPricingCompassCard from '@/components/service/SmartPricingCompassCard.vue'

export interface ServiceFormDraftSnapshot {
  form: {
    serviceTitle: string
    serviceDescription: string
    tagId: number | undefined
    price: number
    tradeType: number
    tradeLocationId: number | undefined
    isPhysical: number
  }
  limitMode: 'unlimited' | 'limited'
  limitCount: number
  images: string[]
}

interface UploadedImageItem {
  raw: string
  preview: string
}

interface Props {
  initialValue?: Partial<ServiceFormDraftSnapshot> | null
  submitText?: string
  submitting?: boolean
  submitDisabled?: boolean
  showTemplates?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  initialValue: null,
  submitText: '提交发布审核',
  submitting: false,
  submitDisabled: false,
  showTemplates: true
})

const emit = defineEmits<{
  (e: 'submit', payload: ServiceFormData): void
  (e: 'cancel'): void
}>()

const userStore = useUserStore()
const dictStore = useDictStore()

const formRef = ref<FormInstance>()
const uploading = ref(false)
const limitMode = ref<'unlimited' | 'limited'>('unlimited')
const limitCount = ref(20)
const imageList = ref<UploadedImageItem[]>([])

const form = reactive({
  serviceTitle: '',
  serviceDescription: '',
  tagId: undefined as number | undefined,
  price: 30,
  tradeType: TradeType.ONLINE_ESCROW as number,
  tradeLocationId: undefined as number | undefined,
  isPhysical: PhysicalType.VIRTUAL as number
})

const descriptionTemplates = [
  {
    name: '设计 / 排版模版',
    content:
      '【服务包含】\n1. 主视觉海报/PPT排版初稿 1 套\n2. 含 2 次细节调整与配色优化\n3. 交付可编辑源文件 + 高清导出图\n\n【需要您提供】\n文案大纲、参考风格截图、期望交付时间。'
  },
  {
    name: '代码 / 调试模版',
    content:
      '【服务范围】\n1. 环境搭建报错排查与接口联调\n2. 核心模块代码重构与注释补全\n3. 附带清晰运行说明与问题总结文档\n\n【交付方式】\n线上远程演示或打包源码注释交付。'
  },
  {
    name: '学业 / 润色模版',
    content:
      '【辅导内容】\n1. 课程重难点 1 对 1 思路梳理（约 60 分钟）\n2. 英文/中文报告结构梳理与表达润色\n3. 配套整理关键知识点思维导图'
  }
]

const rules: FormRules = {
  serviceTitle: [
    { required: true, message: '请输入服务标题', trigger: 'blur' },
    { min: 4, max: 60, message: '标题长度建议 4-60 个字符', trigger: 'blur' }
  ],
  tagId: [{ required: true, message: '请选择技能分类', trigger: 'change' }],
  price: [{ required: true, message: '请设置服务价格', trigger: 'blur' }],
  serviceDescription: [
    { required: true, message: '请填写服务详细描述', trigger: 'blur' },
    { min: 10, max: 1500, message: '描述至少需要 10 个字', trigger: 'blur' }
  ]
}

const availableLocations = computed(() => {
  const all = (dictStore.tradeLocations || []).filter((loc) => loc.isAvailable !== 0)
  const userCampusId = (userStore.userInfo as Record<string, any> | null)?.campusId
  if (userCampusId) {
    const matched = all.filter((loc) => loc.campusId === userCampusId)
    if (matched.length > 0) return matched
  }
  return all
})

const selectedLocation = computed(() =>
  availableLocations.value.find((loc) => loc.id === form.tradeLocationId) || null
)

const missingCount = computed(() => {
  let count = 0
  if (form.serviceTitle.trim().length < 4) count += 1
  if (!form.tagId) count += 1
  if (!form.price || form.price < 1) count += 1
  if (form.serviceDescription.trim().length < 10) count += 1
  return count
})

const previewService = computed<ServiceListItem>(() => ({
  id: 0,
  serviceTitle: form.serviceTitle.trim() || '这里将显示你的服务标题',
  serviceDescription: form.serviceDescription,
  price: Number(form.price || 0),
  tradeType: form.tradeType,
  tagId: form.tagId || 0,
  tagName: form.tagId ? dictStore.getTagName(form.tagId) : '技能分类',
  sellerId: userStore.userInfo?.id || 0,
  sellerName: userStore.userInfo?.nickname || userStore.userInfo?.realName || '我',
  sellerAvatar: userStore.userInfo?.avatar || '',
  coverImage: imageList.value[0]?.raw || '',
  status: ServiceStatus.ONLINE,
  viewCount: 0,
  collectionCount: 0,
  salesCount: 0,
  currentPurchaseCount: 0,
  maxPurchases: limitMode.value === 'unlimited' ? -1 : limitCount.value,
  isPhysical: form.isPhysical,
  createTime: ''
}))

function applySnapshot(snapshot?: Partial<ServiceFormDraftSnapshot> | null) {
  if (!snapshot) return
  if (snapshot.form) {
    Object.assign(form, snapshot.form)
  }
  if (snapshot.limitMode) limitMode.value = snapshot.limitMode
  if (typeof snapshot.limitCount === 'number') limitCount.value = snapshot.limitCount
  if (Array.isArray(snapshot.images)) {
    imageList.value = snapshot.images.map((item) => ({
      raw: item,
      preview: buildImageUrl(item)
    }))
  }
}

watch(
  () => props.initialValue,
  (val) => {
    applySnapshot(val)
  },
  { immediate: true, deep: true }
)

function getSnapshot(): ServiceFormDraftSnapshot {
  return {
    form: { ...form },
    limitMode: limitMode.value,
    limitCount: limitCount.value,
    images: imageList.value.map((i) => i.raw)
  }
}

defineExpose({ getSnapshot })

function toggleLocation(id: number) {
  form.tradeLocationId = form.tradeLocationId === id ? undefined : id
}

function applyTemplate(content: string) {
  if (!form.serviceDescription.trim()) {
    form.serviceDescription = content
  } else {
    form.serviceDescription = `${form.serviceDescription.trim()}\n\n${content}`
  }
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
  if (!formRef.value || props.submitting || props.submitDisabled) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  emit('submit', {
    serviceTitle: form.serviceTitle.trim(),
    serviceDescription: form.serviceDescription.trim(),
    tagId: form.tagId!,
    price: Number(form.price),
    tradeType: form.tradeType,
    tradeLocationId: form.tradeType === TradeType.OFFLINE ? form.tradeLocationId : undefined,
    maxPurchases: limitMode.value === 'unlimited' ? -1 : limitCount.value,
    isPhysical: form.isPhysical,
    images: imageList.value.map((i) => i.raw)
  })
}
</script>

<style scoped lang="scss">
.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
  align-items: start;
}

.editor-main-card {
  padding: 26px 28px;
}

.section-gap {
  margin-top: 14px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.template-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;

  &__label {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.template-chip {
  padding: 4px 12px;
  font-size: 12px;
}

.landmark-picker {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.landmark-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.landmark-card {
  text-align: left;
  padding: 12px 14px;
  border-radius: var(--xyf-radius-sm);
  border: 1px solid var(--xyf-border);
  background: var(--xyf-surface);
  font-family: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: border-color var(--xyf-transition), background-color var(--xyf-transition),
    box-shadow var(--xyf-transition);

  &:hover {
    border-color: var(--xyf-border-strong);
    background: var(--xyf-surface-soft);
  }

  &.is-selected {
    border-color: var(--xyf-primary);
    background: var(--xyf-surface-soft);
    box-shadow: var(--xyf-shadow-focus);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    strong {
      font-size: 13px;
      font-weight: 600;
      color: var(--xyf-ink);
    }
  }

  .cctv-tag {
    font-size: 11px;
    font-weight: 500;
    padding: 1px 6px;
    border-radius: var(--xyf-radius-xs);
    background: var(--xyf-success-soft);
    border: 1px solid var(--xyf-success-border);
    color: var(--xyf-success);
    flex-shrink: 0;
  }

  &__addr {
    margin: 0;
    font-size: 12px;
    color: var(--xyf-ink-2);
  }

  &__meta {
    font-size: 12px;
    color: var(--xyf-ink-3);
  }
}

.landmark-map-preview {
  border-radius: var(--xyf-radius-sm);
  overflow: hidden;
  border: 1px solid var(--xyf-border);
}

.image-uploader-panel {
  padding: 16px;
  border-radius: var(--xyf-radius-md);
  background: var(--xyf-surface-soft);
  border: 1px dashed var(--xyf-border-strong);
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
    padding: 2px 7px;
    border-radius: var(--xyf-radius-xs);
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
    color: var(--xyf-ink);
    cursor: pointer;
    font-family: inherit;

    &.is-danger {
      color: var(--xyf-danger);
    }

    &:hover {
      text-decoration: underline;
    }
  }
}

.upload-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--xyf-ink-3);
}

.submit-footer {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Sticky Right Preview */
.editor-preview-aside {
  position: sticky;
  top: 84px;
}

.preview-shell {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 600;
    color: var(--xyf-ink);
  }

  .missing-hint {
    font-size: 12px;
    font-weight: 500;
    color: #9a5b13;
  }

  .ready-hint {
    font-size: 12px;
    font-weight: 500;
    color: var(--xyf-success);
  }
}

.preview-card-stage {
  pointer-events: none;
}

.settlement-summary {
  margin: 0;
  padding-top: 14px;
  border-top: 1px solid var(--xyf-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.settlement-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;

  dt {
    color: var(--xyf-ink-3);
  }

  dd {
    margin: 0;
    font-weight: 600;
    color: var(--xyf-ink);

    &.is-muted {
      font-weight: 500;
      color: var(--xyf-ink-2);
    }
  }
}

@media (max-width: 960px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .editor-preview-aside {
    position: static;
  }

  .form-row-2,
  .landmark-grid {
    grid-template-columns: 1fr;
  }

  .editor-main-card {
    padding: 20px 16px;
  }
}
</style>
