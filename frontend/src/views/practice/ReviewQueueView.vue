<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NPagination, NSpin, NTag } from 'naive-ui'
import { reviewList } from '@/api/practice'
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
/**
 * 全队列可作答条目数，由服务端统计（与复习会话的选题 SQL 同口径）。
 * 不能用「当前页 title != null 的条数」：队列按到期时间排序，过期 VIP 用户的到期题集中在前面，
 * 第一页可能全是脱敏题而后续页仍有可做题 —— 按当前页判断会误禁用按钮，把用户挡在闭环外。
 */
const answerableCount = ref(0)

async function load() {
  loading.value = true
  try {
    const page = await reviewList({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = page.list
    total.value = page.total
    answerableCount.value = page.answerableCount
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 从复习模式返回时带 refreshed 时间戳（作答会改写 next_review_time），强制重拉队列
watch(
  () => route.query.refreshed,
  () => load()
)

function onPageChange(p: number) {
  pageNum.value = p
  load()
}

/** 进入复习模式：PracticeView 复用同一套作答 → 判分 → 解析 UI */
function startReview() {
  router.push({ path: '/practice', query: { mode: 'review' } })
}
</script>

<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">复习队列</h2>
        <p class="sub-title">
          基于遗忘曲线调度：答错次日复习，连续答对间隔顺延（1 / 2 / 4 / 7 / 15 天），连续答对 3 次即判定掌握。
        </p>
      </div>
      <n-button
        type="primary"
        size="large"
        :disabled="answerableCount === 0"
        @click="startReview"
      >
        开始复习{{ answerableCount > 0 ? `（${answerableCount} 题）` : '' }}
      </n-button>
    </div>

    <div v-if="total > answerableCount && answerableCount > 0" class="masked-banner">
      共 {{ total }} 道到期题目，其中 {{ total - answerableCount }} 道因权限或题目状态无法作答，开始复习时会自动跳过。
    </div>
    <div v-else-if="total > 0 && answerableCount === 0" class="masked-banner">
      本轮 {{ total }} 道到期题目全部因权限或题目状态无法作答，开通会员或等待题目上架后可复习。
    </div>

    <n-spin :show="loading">
      <div v-if="list.length" class="review-list">
        <div v-for="item in list" :key="item.questionId" class="review-card">
          <div class="head">
            <n-tag size="small" :bordered="false" type="info">{{ typeLabel(item.type) }}</n-tag>
            <span class="category">{{ item.categoryName }}</span>
            <span v-if="item.nextReviewTime" class="due">到期 {{ formatTime(item.nextReviewTime) }}</span>
            <span class="stat">做错 {{ item.wrongCount }} / 作答 {{ item.totalCount }} 次</span>
          </div>
          <div v-if="item.title == null" class="masked-tip">🔒 该题目需会员权限，开通会员后可查看题目内容</div>
          <MarkdownRender v-else class="title" :content="item.title" />
        </div>
      </div>
      <div v-else-if="!loading" class="empty-tip">
        ✅ 当前没有需要复习的题目
        <div style="margin-top: 16px">
          <n-button type="primary" @click="router.push('/practice')">继续刷题</n-button>
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

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  background: #fff;
  border: 1px solid var(--border-1);
  border-radius: 12px;
  padding: 18px 20px;
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.category {
  color: var(--ink-400);
  font-size: 13px;
}

.due {
  font-size: 12px;
  color: var(--gold-text);
  background: var(--gold-bg);
  border-radius: 6px;
  padding: 2px 8px;
}

.stat {
  margin-left: auto;
  color: var(--ink-400);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.title {
  font-size: 14px;
}

.masked-tip {
  color: var(--gold-text);
  background: var(--gold-bg);
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
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
