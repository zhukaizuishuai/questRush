<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NInput, NPagination, NSelect, NSpin } from 'naive-ui'
import { listQuestion, searchQuestion } from '@/api/question'
import { getCategoryTree } from '@/api/category'
import QuestionCard from '@/components/QuestionCard.vue'
import { DIFFICULTY_OPTIONS, TYPE_OPTIONS } from '@/utils/format'
import type { CategoryVO, QuestionListVO } from '@/types'

const route = useRoute()

const loading = ref(false)
const list = ref<QuestionListVO[]>([])
const total = ref(0)

const query = reactive({
  categoryId: undefined as number | undefined,
  type: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

interface CategoryOption {
  label: string
  value: number
}

const categoryOptions = ref<CategoryOption[]>([])

function flatten(tree: CategoryVO[], parentName?: string): CategoryOption[] {
  const out: CategoryOption[] = []
  for (const node of tree) {
    const label = parentName ? `${parentName} / ${node.name}` : node.name
    if (node.children?.length) {
      out.push(...flatten(node.children, label))
    } else {
      out.push({ label, value: node.id })
    }
  }
  return out
}

async function loadCategories() {
  const tree = (await getCategoryTree()) ?? []
  categoryOptions.value = flatten(tree)
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (query.keyword.trim()) {
      const page = await searchQuestion(params)
      list.value = page.list
      total.value = page.total
    } else {
      const page = await listQuestion(params)
      list.value = page.list
      total.value = page.total
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  const cid = route.query.categoryId
  if (cid) query.categoryId = Number(cid)
  await load()
})

watch(
  () => route.query.categoryId,
  (cid) => {
    if (cid) {
      query.categoryId = Number(cid)
      query.pageNum = 1
      load()
    }
  }
)

function onFilterChange() {
  query.pageNum = 1
  load()
}

function onSearch() {
  query.pageNum = 1
  load()
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">题库</h2>

    <div class="filter-bar">
      <n-input
        v-model:value="query.keyword"
        placeholder="搜索题干关键词"
        clearable
        style="width: 240px"
        @keyup.enter="onSearch"
        @clear="onFilterChange"
      />
      <n-button type="primary" secondary @click="onSearch">搜索</n-button>
      <n-select
        v-model:value="query.categoryId"
        :options="categoryOptions"
        placeholder="全部分类"
        clearable
        style="width: 220px"
        @update:value="onFilterChange"
      />
      <n-select
        v-model:value="query.type"
        :options="TYPE_OPTIONS"
        placeholder="全部题型"
        clearable
        style="width: 140px"
        @update:value="onFilterChange"
      />
      <n-select
        v-model:value="query.difficulty"
        :options="DIFFICULTY_OPTIONS"
        placeholder="全部难度"
        clearable
        style="width: 140px"
        @update:value="onFilterChange"
      />
    </div>

    <n-spin :show="loading">
      <div class="question-list">
        <QuestionCard v-for="q in list" :key="q.id" :question="q" />
      </div>
      <div v-if="!loading && list.length === 0" class="empty-tip">没有找到符合条件的题目</div>
      <div class="pager">
        <n-pagination
          v-model:page="query.pageNum"
          :page-size="query.pageSize"
          :item-count="total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          @update:page="load"
          @update:page-size="(s: number) => { query.pageSize = s; query.pageNum = 1; load() }"
        />
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 60px 0;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
