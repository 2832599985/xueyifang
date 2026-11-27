<template>
  <div class="service-form-page xyf-page">
    <div class="xyf-container">
      <XyfPageHeader
        kicker="CREATOR STUDIO"
        title="发布校园技能服务"
        subtitle="填写清晰的服务说明、定价与交付方式，提交后经管理员审核即可上架"
        :show-back="true"
        back-text="返回我的服务"
        back-to="/my-services"
      >
        <template #actions v-if="userStore.hasPublishPermission">
          <el-button @click="saveDraft">保存本地草稿</el-button>
          <el-button v-if="hasDraft" text type="danger" @click="clearDraft">清除草稿</el-button>
        </template>
      </XyfPageHeader>

      <!-- Permission Gate -->
      <div v-if="checkingPermission" class="xyf-surface-card gate-card">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else-if="!userStore.hasPublishPermission" class="xyf-surface-card gate-card">
        <XyfEmptyState
          title="尚未开通服务发布权限"
          description="为保障校内服务质量，首次发布前需要在「个人中心」提交一次技能特长与资质说明，管理员审核通过后即可随时发布服务。"
        >
          <template #action>
            <el-button
              type="primary"
              @click="router.push({ path: '/user/center', query: { tab: 'permission' } })"
            >
              前往个人中心申请发布权限
            </el-button>
          </template>
        </XyfEmptyState>
      </div>

      <!-- Publish Form -->
      <div v-else class="form-layout">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="xyf-surface-card form-main-card"
        >
          <div class="xyf-section-heading">
            <h2>1. 基础信息与分类</h2>
          </div>

          <el-form-item label="服务标题" prop="serviceTitle">
            <el-input
              v-model="form.serviceTitle"
              placeholder="清晰概括你的交付内容，例如：Vue3/SpringBoot 毕业设计系统功能定制与调试"
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

          <div class="xyf-section-heading" style="margin-top: 10px">
            <h2>2. 详细描述与交付标准</h2>
          </div>

          <div class="template-bar">
            <span class="template-bar__label">快速插入模版：</span>
            <button
              v-for="tpl in descriptionTemplates"
              :key="tpl.name"
              type="button"
              class="template-pill"
              @click="applyTemplate(tpl.content)"
            >
              {{ tpl.name }}
            </button>
          </div>

          <el-form-item label="服务详细描述" prop="serviceDescription">
            <el-input
              v-model="form.serviceDescription"
              type="textarea"
              :rows="7"
              maxlength="1500"
              show-word-limit
              placeholder="请详细说明服务包含内容、预计交付周期、所需材料及修改次数等..."
            />
          </el-form-item>

          <div class="xyf-section-heading" style="margin-top: 10px">
            <h2>3. 交易与库存配置</h2>
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

          <el-form-item
            v-if="form.tradeType === TradeType.OFFLINE"
            label="推荐校内交易地点（选填，方便买家快速选择）"
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

            <el-form-item v-if="limitMode === 'limited'" label="最多可购买份数" prop="maxPurchases">
              <el-input-number v-model="limitCount" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </div>

          <div class="xyf-section-heading" style="margin-top: 10px">
            <h2>4. 服务展示图片（最多 5 张，首张作为列表封面）</h2>
          </div>

          <div class="image-uploader-panel">
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
                上传展示图片 ({{ imageList.length }}/5)
              </el-button>
            </el-upload>
            <p class="upload-tip">建议上传 4:3 横图或作品实拍截图，单张不超过 5MB，支持 JPG/PNG/WebP。</p>
          </div>

          <div class="submit-footer">
            <el-button @click="router.back()">取消</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              提交发布审核
            </el-button>
          </div>
        </el-form>

        <!-- Helpful Studio Checklist -->
        <aside class="xyf-surface-card studio-guide-card">
          <h3>发布前自检建议</h3>
          <ul class="guide-list">
            <li>标题突出「能解决什么问题 + 适用场景」，避免笼统词。</li>
            <li>说明中列出需要买家提前准备的材料，减少反复沟通。</li>
            <li>若支持加急或多次修改，请在详情中注明相应边界。</li>
            <li>提交后会由管理员进行合规审核，结果将通过站内通知实时提醒。</li>
          </ul>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'
import { publishService, buildImageUrl } from '@/api/service'
import { uploadFile } from '@/api/file'
import { notifyFallbackError } from '@/api/request'
import { TradeType, PhysicalType } from '@/types/service'
import XyfPageHeader from '@/components/ui/XyfPageHeader.vue'
import XyfEmptyState from '@/components/ui/XyfEmptyState.vue'
import XyfImage from '@/components/ui/XyfImage.vue'
import SmartPricingCompassCard from '@/components/service/SmartPricingCompassCard.vue'

interface UploadedImageItem {
  raw: string
  preview: string
}

const router = useRouter()
const userStore = useUserStore()
const dictStore = useDictStore()

const formRef = ref<FormInstance>()
const checkingPermission = ref(true)
const uploading = ref(false)
const submitting = ref(false)
const limitMode = ref<'unlimited' | 'limited'>('unlimited')
const limitCount = ref(20)
const imageList = ref<UploadedImageItem[]>([])
const hasDraft = ref(false)

const form = reactive({
  serviceTitle: '',
  serviceDescription: '',
  tagId: undefined as number | undefined,
  price: 30,
  tradeType: TradeType.ONLINE_ESCROW,
  tradeLocationId: undefined as number | undefined,
  isPhysical: PhysicalType.VIRTUAL
})

const draftStorageKey = computed(() => `xyf_service_draft_${userStore.userInfo?.id || 'guest'}`)

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

function saveDraft() {
  try {
    localStorage.setItem(
      draftStorageKey.value,
      JSON.stringify({
        form: { ...form },
        limitMode: limitMode.value,
        limitCount: limitCount.value,
        images: imageList.value.map((i) => i.raw)
      })
    )
    hasDraft.value = true
    ElMessage.success('草稿已保存至当前浏览器')
  } catch {
    ElMessage.warning('草稿保存失败')
  }
}

function restoreDraftIfAny() {
  try {
    const raw = localStorage.getItem(draftStorageKey.value)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed?.form) {
      Object.assign(form, parsed.form)
    }
    if (parsed?.limitMode) limitMode.value = parsed.limitMode
    if (parsed?.limitCount) limitCount.value = parsed.limitCount
    if (Array.isArray(parsed?.images)) {
      imageList.value = parsed.images.map((item: string) => ({
        raw: item,
        preview: buildImageUrl(item)
      }))
    }
    hasDraft.value = true
  } catch {
    // 忽略损坏的草稿
  }
}

function clearDraft(silent = false) {
  localStorage.removeItem(draftStorageKey.value)
  hasDraft.value = false
  if (!silent) ElMessage.info('草稿已清除')
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await publishService({
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

    clearDraft(true)
    ElMessage.success('服务已提交，等待管理员审核上架')
    router.replace('/my-services')
  } catch (err) {
    notifyFallbackError(err, '服务发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    await userStore.ensureUserInfo()
    if (userStore.hasPublishPermission) {
      await Promise.all([
        dictStore.fetchServiceTags().catch(() => {}),
        dictStore.fetchTradeLocations().catch(() => {})
      ])
      restoreDraftIfAny()
    }
  } finally {
    checkingPermission.value = false
  }
})
</script>

<style scoped lang="scss">
.gate-card {
  padding: 28px;
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(260px, 0.7fr);
  gap: 22px;
  align-items: start;
}

.form-main-card {
  padding: 26px 28px;
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
  margin-bottom: 10px;

  &__label {
    font-size: 12.5px;
    color: var(--xyf-ink-muted);
  }
}

.template-pill {
  padding: 4px 12px;
  border-radius: var(--xyf-radius-pill);
  border: 1px solid var(--xyf-primary-border);
  background: var(--xyf-primary-soft);
  color: var(--xyf-primary-dark);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;

  &:hover {
    background: var(--xyf-primary);
    color: #fff;
  }
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

    &:hover {
      text-decoration: underline;
    }
  }
}

.upload-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--xyf-ink-muted);
}

.submit-footer {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid var(--xyf-border-light);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.studio-guide-card {
  padding: 22px;
  position: sticky;
  top: 84px;

  h3 {
    margin: 0 0 12px;
    font-size: 15.5px;
    font-weight: 700;
    color: var(--xyf-ink);
  }
}

.guide-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--xyf-ink-secondary);
}

@media (max-width: 900px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .studio-guide-card {
    position: static;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .form-main-card {
    padding: 20px 16px;
  }
}
</style>
