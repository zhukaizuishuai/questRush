<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NDataTable,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NUpload,
  NUploadDragger,
  NSwitch,
  useDialog,
  useMessage,
  type DataTableColumns,
  type UploadFileInfo
} from 'naive-ui'
import {
  adminQuestionPage,
  deleteQuestion,
  downloadTemplate,
  exportExcel,
  importExcel,
  recalcLike,
  updateQuestionStatus
} from '@/api/admin'
import { getCategoryTree } from '@/api/category'
import { DIFFICULTY_OPTIONS, TYPE_OPTIONS, formatTime } from '@/utils/format'
import type { CategoryVO, ExcelImportResultVO, QuestionAdminVO } from '@/types'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const list = ref<QuestionAdminVO[]>([])
const total = ref(0)

const query = reactive({
  categoryId: undefined as number | undefined,
  type: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  isVip: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const categoryOptions = ref<{ label: string; value: number }[]>([])
const isVipOptions = [
  { label: '免费', value: 0 },
  { label: 'VIP', value: 1 }
]
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '下架', value: 0 }
]

function flatten(nodes: CategoryVO[], parentName?: string) {
  const out: { label: string; value: number }[] = []
  for (const n of nodes) {
    const label = parentName ? `${parentName} / ${n.name}` : n.name
    if (n.children?.length) out.push(...flatten(n.children, label))
    else out.push({ label, value: n.id })
  }
  return out
}

async function load() {
  loading.value = true
  try {
    const page = await adminQuestionPage({ ...query })
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const tree = (await getCategoryTree()) ?? []
  categoryOptions.value = flatten(tree)
  await load()
})

function onSearch() {
  query.pageNum = 1
  load()
}

// ============ Excel 导入对话框 ============
const showImport = ref(false)
const importing = ref(false)
const duplicateStrategy = ref<'SKIP' | 'OVERWRITE' | 'ERROR'>('SKIP')
const importFile = ref<File | null>(null)
const importResult = ref<ExcelImportResultVO | null>(null)

const strategyOptions = [
  { label: 'SKIP - 已存在则跳过（推荐，追加新题）', value: 'SKIP' },
  { label: 'OVERWRITE - 已存在则覆盖更新', value: 'OVERWRITE' },
  { label: 'ERROR - 已存在则记为失败行', value: 'ERROR' }
]

function onFileChange(options: { fileList: UploadFileInfo[] }) {
  const f = options.fileList[0]?.file ?? null
  if (f && !f.name.endsWith('.xlsx')) {
    message.error('仅支持 .xlsx 文件')
    importFile.value = null
    return
  }
  if (f && f.size > 5 * 1024 * 1024) {
    message.error('文件不能超过 5MB')
    importFile.value = null
    return
  }
  importFile.value = f
  importResult.value = null
}

async function doImport() {
  if (!importFile.value) {
    message.warning('请先选择 .xlsx 文件')
    return
  }
  importing.value = true
  try {
    importResult.value = await importExcel(importFile.value, duplicateStrategy.value)
    message.success(`导入完成：新增 ${importResult.value.successCount} 条 / 跳过 ${importResult.value.skippedCount} 条 / 失败 ${importResult.value.failCount} 条`)
    await load()
  } finally {
    importing.value = false
  }
}

function openImport() {
  importFile.value = null
  importResult.value = null
  duplicateStrategy.value = 'SKIP'
  showImport.value = true
}

const failureColumns: DataTableColumns<{ row: number; reason: string }> = [
  { title: 'Excel 行号', key: 'row', width: 100 },
  { title: '失败原因', key: 'reason' }
]

// ============ 行操作 ============
function remove(row: QuestionAdminVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除该题目吗？（逻辑删除，可在后台追溯）`,
    positiveText: '确定删除',
    negativeText: '取消',
    async onPositiveClick() {
      await deleteQuestion(row.id)
      message.success('已删除')
      await load()
    }
  })
}

async function toggleShelf(row: QuestionAdminVO) {
  const next = row.status === 1 ? 0 : 1
  await updateQuestionStatus(row.id, next)
  message.success(next === 1 ? '已上架' : '已下架')
  await load()
}

async function doRecalcLike() {
  await recalcLike()
  message.success('点赞数已按点赞表重算')
  await load()
}

const columns: DataTableColumns<QuestionAdminVO> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '分类', key: 'categoryName', width: 130, ellipsis: { tooltip: true } },
  {
    title: '题干',
    key: 'title',
    ellipsis: { tooltip: true },
    render: (row) => row.title.replace(/[#*`>\-\n]/g, '').slice(0, 60)
  },
  { title: '题型', key: 'type', width: 80, render: (row) => TYPE_OPTIONS.find((t) => t.value === row.type)?.label ?? '-' },
  { title: '难度', key: 'difficulty', width: 70, render: (row) => DIFFICULTY_OPTIONS.find((d) => d.value === row.difficulty)?.label ?? '-' },
  { title: 'VIP', key: 'isVip', width: 60, render: (row) => (row.isVip === 1 ? '是' : '否') },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        size: 'small',
        onUpdateValue: (v: boolean) => toggleShelf({ ...row, status: v ? 1 : 0 })
      })
  },
  { title: '点赞', key: 'likeCount', width: 60 },
  { title: '创建时间', key: 'createTime', width: 150, render: (row) => formatTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render(row) {
      return h(NSpace, { size: 'small' }, () => [
        h(NButton, { size: 'tiny', onClick: () => router.push(`/admin/question-edit/${row.id}`) }, () => '编辑'),
        h(NButton, { size: 'tiny', type: 'error', secondary: true, onClick: () => remove(row) }, () => '删除')
      ])
    }
  }
]
</script>

<template>
  <div>
    <div class="head-row">
      <h2 class="page-title">题库管理</h2>
      <n-space>
        <n-button @click="downloadTemplate">下载导入模板</n-button>
        <n-button type="warning" secondary @click="doRecalcLike">重算点赞数</n-button>
        <n-button type="success" secondary @click="exportExcel({ ...query })">批量导出</n-button>
        <n-button type="primary" secondary @click="openImport">Excel 批量导入</n-button>
        <n-button type="primary" @click="router.push('/admin/question-edit')">新增题目</n-button>
      </n-space>
    </div>

    <div class="filter-bar">
      <n-input v-model:value="query.keyword" placeholder="题干关键词" clearable style="width: 180px" @keyup.enter="onSearch" @clear="onSearch" />
      <n-select v-model:value="query.categoryId" :options="categoryOptions" placeholder="分类" clearable filterable style="width: 200px" @update:value="onSearch" />
      <n-select v-model:value="query.type" :options="TYPE_OPTIONS" placeholder="题型" clearable style="width: 110px" @update:value="onSearch" />
      <n-select v-model:value="query.difficulty" :options="DIFFICULTY_OPTIONS" placeholder="难度" clearable style="width: 100px" @update:value="onSearch" />
      <n-select v-model:value="query.isVip" :options="isVipOptions" placeholder="VIP" clearable style="width: 90px" @update:value="onSearch" />
      <n-select v-model:value="query.status" :options="statusOptions" placeholder="状态" clearable style="width: 90px" @update:value="onSearch" />
      <n-button type="primary" secondary @click="onSearch">查询</n-button>
    </div>

    <div class="table-card">
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :bordered="false"
        :scroll-x="1200"
        :pagination="{
          page: query.pageNum,
          pageSize: query.pageSize,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.pageNum = p; load() },
          onUpdatePageSize: (s: number) => { query.pageSize = s; query.pageNum = 1; load() }
        }"
      />
    </div>

    <!-- Excel 导入对话框 -->
    <n-modal v-model:show="showImport" preset="card" title="Excel 批量导入" style="width: 640px">
      <n-space vertical size="large">
        <div>
          <div class="form-label">重复题处理策略</div>
          <n-select v-model:value="duplicateStrategy" :options="strategyOptions" />
        </div>
        <n-upload :max="1" accept=".xlsx" :default-upload="false" :on-change="onFileChange" :file-list="importFile ? [{ id: 'f', name: importFile.name, status: 'finished' } as UploadFileInfo] : []">
          <n-upload-dragger>
            <div style="padding: 24px 0">
              <div style="font-size: 28px">📥</div>
              <div>点击或拖拽 .xlsx 文件到此处（≤ 5MB、≤ 5000 行）</div>
            </div>
          </n-upload-dragger>
        </n-upload>
        <n-button type="primary" block :loading="importing" :disabled="!importFile" @click="doImport">
          开始导入
        </n-button>

        <div v-if="importResult" class="import-summary">
          <n-space>
            <n-tag type="success" :bordered="false">新增 {{ importResult.successCount }} 条</n-tag>
            <n-tag type="info" :bordered="false">跳过 {{ importResult.skippedCount }} 条</n-tag>
            <n-tag type="error" :bordered="false">失败 {{ importResult.failCount }} 条</n-tag>
          </n-space>
          <n-data-table
            v-if="importResult.failures.length"
            :columns="failureColumns"
            :data="importResult.failures"
            size="small"
            :max-height="240"
            style="margin-top: 12px"
          />
        </div>
      </n-space>
    </n-modal>
  </div>
</template>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.table-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 16px;
}

.form-label {
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
}

.import-summary {
  border-top: 1px dashed #e0e0e6;
  padding-top: 14px;
}
</style>
