<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, NInputNumber, NSelect, NSwitch, useMessage } from 'naive-ui'
import { adminQuestionDetail, createQuestion, updateQuestion, type QuestionSaveDTO } from '@/api/admin'
import { getCategoryTree } from '@/api/category'
import MarkdownRender from '@/components/MarkdownRender.vue'
import { DIFFICULTY_OPTIONS, TYPE_OPTIONS } from '@/utils/format'
import type { CategoryVO } from '@/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const editId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const isEdit = computed(() => !!editId.value)

const loading = ref(true)
const saving = ref(false)
const categoryOptions = ref<{ label: string; value: number }[]>([])

const form = reactive({
  categoryId: null as number | null,
  type: 1,
  difficulty: 2,
  title: '',
  answer: '',
  answerText: '',
  analysis: '',
  isVip: false,
  status: true,
  options: [
    { optionCode: 'A', optionContent: '' },
    { optionCode: 'B', optionContent: '' },
    { optionCode: 'C', optionContent: '' },
    { optionCode: 'D', optionContent: '' }
  ] as { optionCode: string; optionContent: string }[]
})

const isObjective = computed(() => form.type === 1 || form.type === 2 || form.type === 3)

onMounted(async () => {
  try {
    const tree = (await getCategoryTree()) ?? []
    categoryOptions.value = flatten(tree)
    if (editId.value) {
      const q = await adminQuestionDetail(editId.value)
      form.categoryId = q.categoryId
      form.type = q.type
      form.difficulty = q.difficulty
      form.title = q.title
      form.answer = q.answer ?? ''
      form.answerText = q.answerText ?? ''
      form.analysis = q.analysis ?? ''
      form.isVip = q.isVip === 1
      form.status = q.status === 1
      if (q.options?.length) {
        form.options = q.options.map((o) => ({ optionCode: o.optionCode, optionContent: o.optionContent }))
      }
    }
  } finally {
    loading.value = false
  }
})

function flatten(nodes: CategoryVO[], parentName?: string) {
  const out: { label: string; value: number }[] = []
  for (const n of nodes) {
    const label = parentName ? `${parentName} / ${n.name}` : n.name
    if (n.children?.length) out.push(...flatten(n.children, label))
    else out.push({ label, value: n.id })
  }
  return out
}

function addOption() {
  const codes = 'ABCDEFGH'
  if (form.options.length >= 8) return
  form.options.push({ optionCode: codes[form.options.length] ?? '', optionContent: '' })
}

function removeOption(i: number) {
  form.options.splice(i, 1)
}

async function save() {
  if (!form.categoryId) {
    message.warning('请选择分类（题目只能挂载在叶子分类）')
    return
  }
  if (!form.title.trim()) {
    message.warning('请输入题干')
    return
  }
  if (isObjective.value) {
    if (form.options.some((o) => !o.optionContent.trim())) {
      message.warning('客观题选项内容不能为空')
      return
    }
    if (!form.answer.trim()) {
      message.warning('请填写正确答案（如 A 或 ABD，判断题填 A/B）')
      return
    }
    const validCodes = form.options.map((o) => o.optionCode)
    if (form.answer.split('').some((c) => !validCodes.includes(c))) {
      message.warning('答案字符必须落在选项范围内')
      return
    }
  }
  saving.value = true
  try {
    const dto: QuestionSaveDTO = {
      id: editId.value,
      categoryId: form.categoryId,
      type: form.type,
      difficulty: form.difficulty,
      title: form.title,
      answer: isObjective.value ? form.answer : '',
      answerText: form.answerText,
      analysis: form.analysis,
      isVip: form.isVip ? 1 : 0,
      status: form.status ? 1 : 0,
      options: isObjective.value
        ? form.options.map((o, i) => ({ ...o, sort: i }))
        : []
    }
    if (isEdit.value) {
      await updateQuestion(dto)
    } else {
      await createQuestion(dto)
    }
    message.success('已保存')
    router.push('/admin/questions')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="edit-page">
    <div class="head-row">
      <h2 class="page-title">{{ isEdit ? '编辑题目' : '新增题目' }}</h2>
      <n-button @click="router.back()">返回</n-button>
    </div>

    <n-spin :show="loading">
      <div class="form-grid">
        <div class="form-card">
          <n-form label-placement="left" label-width="90">
            <n-form-item label="所属分类">
              <n-select v-model:value="form.categoryId" :options="categoryOptions" filterable placeholder="选择叶子分类" />
            </n-form-item>
            <n-form-item label="题型">
              <n-select v-model:value="form.type" :options="TYPE_OPTIONS" @update:value="() => { if (form.type === 3) { form.options = [{ optionCode: 'A', optionContent: '正确' }, { optionCode: 'B', optionContent: '错误' }] } }" />
            </n-form-item>
            <n-form-item label="难度">
              <n-select v-model:value="form.difficulty" :options="DIFFICULTY_OPTIONS" />
            </n-form-item>
            <n-form-item label="VIP 专属">
              <n-switch v-model:value="form.isVip" />
            </n-form-item>
            <n-form-item label="上架状态">
              <n-switch v-model:value="form.status">
                <template #checked>启用</template>
                <template #unchecked>下架</template>
              </n-switch>
            </n-form-item>
          </n-form>
        </div>

        <div class="content-card">
          <div class="field">
            <div class="field-label">题干（支持 Markdown）</div>
            <n-input v-model:value="form.title" type="textarea" rows="6" placeholder="支持 Markdown 与代码块，代码用 ```java 包裹" />
            <MarkdownRender class="preview" :content="form.title" />
          </div>

          <!-- 客观题选项 -->
          <div v-if="isObjective" class="field">
            <div class="field-label">
              选项内容
              <n-button v-if="form.type !== 3" size="tiny" quaternary type="primary" @click="addOption">+ 添加选项</n-button>
            </div>
            <div v-for="(opt, i) in form.options" :key="i" class="option-row">
              <n-input v-model:value="opt.optionCode" style="width: 60px" :disabled="form.type === 3" />
              <n-input v-model:value="opt.optionContent" placeholder="选项内容（支持 Markdown）" style="flex: 1" />
              <n-button v-if="form.type !== 3 && form.options.length > 2" size="tiny" quaternary type="error" @click="removeOption(i)">删除</n-button>
            </div>
          </div>

          <div v-if="isObjective" class="field">
            <div class="field-label">正确答案</div>
            <n-input v-model:value="form.answer" :placeholder="form.type === 2 ? '多选填字母，如 ABD（判分时与顺序无关）' : '单选填 A；判断题填 A（正确）或 B（错误）'" style="width: 300px" />
          </div>

          <div v-if="form.type === 4" class="field">
            <div class="field-label">简答题参考答案（Markdown）</div>
            <n-input v-model:value="form.answerText" type="textarea" rows="6" />
          </div>

          <div class="field">
            <div class="field-label">题目解析（Markdown，选填）</div>
            <n-input v-model:value="form.analysis" type="textarea" rows="5" placeholder="提交作答后展示给用户" />
            <MarkdownRender v-if="form.analysis" class="preview" :content="form.analysis" />
          </div>

          <n-button type="primary" size="large" :loading="saving" @click="save">保存</n-button>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 16px;
  align-items: start;
}

.form-card,
.content-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 20px;
}

.field {
  margin-bottom: 20px;
}

.field-label {
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.preview {
  margin-top: 10px;
  border: 1px dashed #e0e0e6;
  border-radius: 8px;
  padding: 10px 14px;
  background: #fafbfc;
}
</style>
