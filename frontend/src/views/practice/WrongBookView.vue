<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NPagination, NSpin, NTag } from 'naive-ui'
import { wrongList } from '@/api/practice'
import MarkdownRender from '@/components/MarkdownRender.vue'
import { formatTime, typeLabel } from '@/utils/format'
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
    const page = await wrongList({ pageNum: pageNum.value, pageSize: pageSize.value })
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

function goPractice() {
  router.push('/practice')
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">错题本</h2>
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
            <n-button size="small" type="primary" secondary @click="goPractice">去重做</n-button>
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
.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wrong-card {
  background: #fff;
  border: 1px solid #eceef1;
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
  color: #888;
  font-size: 13px;
}

.meta {
  margin-left: auto;
  color: #888;
  font-size: 13px;
}

.danger {
  color: #e05a5a;
}

.title {
  font-size: 14px;
  margin-bottom: 12px;
}

.masked-tip {
  color: #8a5a00;
  background: #fffaf0;
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
  color: #aaa;
  font-size: 12px;
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
