<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NPagination, NSpin, NTag, useMessage } from 'naive-ui'
import { favoriteList, toggleFavorite } from '@/api/favorite'
import { difficultyLabel, difficultyTagType, formatTime, typeLabel } from '@/utils/format'
import type { FavoriteVO } from '@/types'

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const list = ref<FavoriteVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const page = await favoriteList({ pageNum: pageNum.value, pageSize: pageSize.value })
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

async function removeFav(item: FavoriteVO) {
  await toggleFavorite(item.questionId)
  message.success('已取消收藏')
  await load()
}

function goDetail(id: number) {
  router.push(`/question/${id}`)
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">我的收藏</h2>
    <n-spin :show="loading">
      <div v-if="list.length" class="fav-list">
        <div v-for="item in list" :key="item.questionId" class="fav-card" @click="goDetail(item.questionId)">
          <div class="head">
            <n-tag size="small" :bordered="false" type="info">{{ typeLabel(item.type) }}</n-tag>
            <n-tag size="small" :bordered="false" :type="difficultyTagType(item.difficulty)">
              {{ difficultyLabel(item.difficulty) }}
            </n-tag>
            <span class="category">{{ item.categoryName }}</span>
            <span class="time">{{ formatTime(item.favoritedTime) }}</span>
          </div>
          <div v-if="item.title == null" class="masked-tip">🔒 该题目需会员权限</div>
          <div v-else class="title">{{ item.title }}</div>
          <div class="foot">
            <n-button size="tiny" quaternary type="warning" @click.stop="removeFav(item)">取消收藏</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else-if="!loading" description="还没有收藏任何题目" style="padding: 80px 0" />
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
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fav-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.15s;
}

.fav-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
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

.time {
  margin-left: auto;
  color: #bbb;
  font-size: 12px;
}

.title {
  font-size: 14px;
  line-height: 1.6;
}

.masked-tip {
  color: #8a5a00;
  font-size: 14px;
}

.foot {
  margin-top: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
