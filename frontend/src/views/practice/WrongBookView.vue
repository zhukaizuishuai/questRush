<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NPagination, NSpin, NTag } from 'naive-ui'
import { wrongList } from '@/api/practice'
import MarkdownRender from '@/components/MarkdownRender.vue'
import { formatTime, typeLabel } from '@/utils/format'
import type { WrongBookVO } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const list = ref<WrongBookVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
/** 全队列可作答条目数，服务端统计（与错题重做会话的选题 SQL 同口径） */
const answerableCount = ref(0)

async function load() {
  loading.value = true
  try {
    const page = await wrongList({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = page.list
    total.value = page.total
    answerableCount.value = page.answerableCount
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 从错题重做模式返回时带 refreshed 时间戳（答对后条目会移出错题本），强制重拉
watch(
  () => route.query.refreshed,
  () => load()
)

function onPageChange(p: number) {
  pageNum.value = p
  load()
}

/** 错题重做模式：PracticeView 从错题本选题，复用同一套作答 UI */
function startRedo() {
  router.push({ path: '/practice', query: { mode: 'wrong' } })
}

function goPractice() {
  router.push('/practice')
}
</script>

<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">错题本</h2>
        <p class="sub-title">答错自动归集，重新答对一次即移出错题本；未掌握的题目会按遗忘曲线进入复习队列。</p>
      </div>
      <n-button
        type="primary"
        size="large"
        :disabled="answerableCount === 0"
        @click="startRedo"
      >
        重做错题{{ answerableCount > 0 ? `（${answerableCount} 题）` : '' }}
      </n-button>
    </div>

    <div v-if="total > answerableCount && answerableCount > 0" class="masked-banner">
      共 {{ total }} 道错题，其中 {{ total - answerableCount }} 道因权限或题目状态无法作答，重做时会自动跳过。
    </div>
    <div v-else-if="total > 0 && answerableCount === 0" class="masked-banner">
      本轮 {{ total }} 道错题全部因权限或题目状态无法作答，开通会员或等待题目上架后可重做。
    </div>

    <n-spin :show="loading">
      <div v-if="list.length" class="wrong-list">
        <div v-for="item in list" :key="item.questionId" class="wrong-card">
          <div class="head">
            <n-tag size="small" :bordered="false" type="info">{{ typeLabel(item.type) }}</n-tag>
            <span class="category">{{ item.categoryName }}</span>
            <span class="meta">
              累计做错 <b class="danger">{{ item.wrongCount }}</b> 次 / 共作答 {{ item.totalCount }} 次
            </span>
          </div>
          <!-- VIP 过期后返回脱敏条目：title 为 null，仅提示 -->
          <div v-if="item.title == null" class="masked-tip">🔒 该题目需会员权限，开通会员后可查看题目内容</div>
          <MarkdownRender v-else class="title" :content="item.title" />
          <div class="foot">
            <span class="time">最近作答：{{ formatTime(item.submitTime) }}</span>
            <n-button
              size="small"
              type="primary"
              secondary
              :disabled="item.title == null"
              @click="startRedo"
            >
              重做错题
            </n-button>
          </div>
        </div>
      </div>
      <div v-else-if="!loading" class="empty-tip">
        🎉 太棒了，错题本是空的！
        <div style="margin-top: 16px">
          <n-button type="primary" @click="goPractice">继续刷题</n-button>
        </div>
      </div>
      <div v-if="total > pageSize" class="pager">
        <n-pagination
          :page="pageNum"
          :page-size="pageSize"
          :item-count="total"
          @update:page="onPageChange"
        />
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.sub-title {
  color: var(--ink-400);
  font-size: 13px;
  margin: 4px 0 0;
  max-width: 640px;
}

.masked-banner {
  background: var(--gold-bg);
  border: 1px solid var(--gold-border);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--gold-text);
  margin-bottom: 14px;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wrong-card {
  background: #fff;
  border: 1px solid var(--border-1);
  border-radius: 12px;
  padding: 18px 20px;
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.category {
  color: var(--ink-400);
  font-size: 13px;
}

.meta {
  margin-left: auto;
  color: var(--ink-400);
  font-size: 13px;
}

.danger {
  color: var(--rose-500);
}

.title {
  font-size: 14px;
  margin-bottom: 12px;
}

.masked-tip {
  color: var(--gold-text);
  background: var(--gold-bg);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  font-size: 14px;
}

.foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time {
  color: var(--ink-400);
  font-size: 12px;
}

.empty-tip {
  text-align: center;
  color: var(--ink-400);
  padding: 80px 0;
  background: #fff;
  border-radius: 12px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
