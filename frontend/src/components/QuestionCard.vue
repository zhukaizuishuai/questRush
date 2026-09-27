<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { QuestionListVO } from '@/types'
import VipBadge from './VipBadge.vue'
import LikeButton from './LikeButton.vue'
import { typeLabel, difficultyLabel, difficultyTagType } from '@/utils/format'

defineProps<{
  question: QuestionListVO
}>()

const router = useRouter()

function goDetail(id: number) {
  router.push(`/question/${id}`)
}
</script>

<template>
  <div class="question-card" @click="goDetail(question.id)">
    <div class="card-header">
      <n-tag size="small" :bordered="false" type="info">{{ typeLabel(question.type) }}</n-tag>
      <n-tag size="small" :bordered="false" :type="difficultyTagType(question.difficulty)">
        {{ difficultyLabel(question.difficulty) }}
      </n-tag>
      <span class="category">{{ question.categoryName }}</span>
      <VipBadge :is-vip="question.isVip" />
    </div>
    <!-- 列表项题干（Markdown 原文），不含答案 -->
    <div class="card-title">{{ question.title }}</div>
    <div class="card-footer">
      <LikeButton :question-id="question.id" :like-count="question.likeCount" />
      <span class="arrow">查看详情 →</span>
    </div>
  </div>
</template>

<style scoped>
.question-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 10px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.15s;
}

.question-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.category {
  color: #888;
  font-size: 13px;
}

.card-title {
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.arrow {
  color: #18a058;
  font-size: 13px;
}
</style>
