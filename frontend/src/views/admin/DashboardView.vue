<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NSpin } from 'naive-ui'
import { adminStats } from '@/api/admin'
import type { AdminStatsVO } from '@/types'

const loading = ref(true)
const data = ref<AdminStatsVO | null>(null)

onMounted(async () => {
  try {
    data.value = await adminStats()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <h2 class="page-title">数据统计</h2>
    <n-spin :show="loading">
      <div v-if="data" class="stat-grid">
        <div class="stat-card">
          <div class="num">{{ data.userCount }}</div>
          <div class="label">注册用户数</div>
        </div>
        <div class="stat-card">
          <div class="num">{{ data.vipUserCount }}</div>
          <div class="label">VIP 用户数</div>
        </div>
        <div class="stat-card">
          <div class="num">{{ data.questionCount }}</div>
          <div class="label">题目总数</div>
        </div>
        <div class="stat-card">
          <div class="num">{{ data.dailyAvgAnswers }}</div>
          <div class="label">日均答题量</div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.num {
  font-size: 30px;
  font-weight: 700;
  color: #18a058;
}

.label {
  color: #888;
  font-size: 13px;
  margin-top: 6px;
}
</style>
