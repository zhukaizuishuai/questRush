<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NPagination, NSpin, NTag } from 'naive-ui'
import { reviewList } from '@/api/practice'
import MarkdownRender from '@/components/MarkdownRender.vue'
import { typeLabel } from '@/utils/format'
import type { WrongBookVO } from '@/types'

const router = useRouter()
const loading = ref(true)
const list = ref<WrongBookVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const page = await reviewList({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onPageChange(p: number) {
  pageNum.value = p
  load()
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">复习队列</h2>
    <p class="sub-title">
      基于遗忘曲线调度：答错次日复习，连续答对间隔顺延（1 / 2 / 4 / 7 / 15 天），连续答对 3 次即判定掌握。
    </p>
    <n-spin :show="loading">
      <div v-if="list.length" class="review-list">
        <div v-for="item in list" :key="item.questionId" class="review-card">
          <div class="head">
            <n-tag size="small" :bordered="false" type="info">{{ typeLabel(item.type) }}</n-tag>
            <span class="category">{{ item.categoryName }}</span>
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
.sub-title {
  color: #888;
  font-size: 13px;
  margin: -8px 0 16px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 18px 20px;
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.category {
  color: #888;
  font-size: 13px;
}

.title {
  font-size: 14px;
}

.masked-tip {
  color: #8a5a00;
  background: #fffaf0;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
}

.empty-tip {
  text-align: center;
  color: #999;
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
