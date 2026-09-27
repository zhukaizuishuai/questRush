<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { NSpin } from 'naive-ui'
import { stats } from '@/api/practice'
import type { PracticeStatsVO } from '@/types'

const loading = ref(true)
const data = ref<PracticeStatsVO | null>(null)

onMounted(async () => {
  try {
    data.value = await stats()
  } finally {
    loading.value = false
  }
})

const rateText = computed(() => (data.value ? `${data.value.accuracy.toFixed(1)}%` : '-'))

function rateOf(item: { total: number; correct: number }) {
  if (!item.total) return '0.0%'
  return `${((item.correct / item.total) * 100).toFixed(1)}%`
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">做题统计</h2>
    <n-spin :show="loading">
      <template v-if="data">
        <div class="stat-cards">
          <div class="stat-card">
            <div class="num">{{ data.totalCount }}</div>
            <div class="label">累计作答次数</div>
          </div>
          <div class="stat-card">
            <div class="num">{{ data.correctCount }}</div>
            <div class="label">答对次数</div>
          </div>
          <div class="stat-card">
            <div class="num green">{{ rateText }}</div>
            <div class="label">总作答正确率</div>
          </div>
          <div class="stat-card">
            <div class="num orange">{{ data.continuousCheckInDays }}</div>
            <div class="label">连续打卡（天）</div>
          </div>
          <div class="stat-card">
            <div class="num red">{{ data.wrongBookCount }}</div>
            <div class="label">错题本题数</div>
          </div>
          <div class="stat-card">
            <div class="num blue">{{ data.reviewCount }}</div>
            <div class="label">待复习题数</div>
          </div>
        </div>

        <div class="table-card">
          <div class="table-title">按分类正确率（口径：总作答正确率 = 答对流水 / 总作答流水）</div>
          <table class="stat-table">
            <thead>
              <tr>
                <th>分类</th>
                <th>作答次数</th>
                <th>答对次数</th>
                <th>正确率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in data.categoryStats" :key="c.categoryId">
                <td>{{ c.categoryName }}</td>
                <td>{{ c.total }}</td>
                <td>{{ c.correct }}</td>
                <td>
                  <span class="rate-bar">
                    <span class="rate-fill" :style="{ width: `${Math.min(100, c.total ? (c.correct / c.total) * 100 : 0)}%` }"></span>
                  </span>
                  {{ rateOf(c) }}
                </td>
              </tr>
              <tr v-if="!data.categoryStats?.length">
                <td colspan="4" class="empty">暂无数据，快去刷题吧</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </n-spin>
  </div>
</template>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 18px 12px;
  text-align: center;
}

.num {
  font-size: 24px;
  font-weight: 700;
}

.num.green { color: #18a058; }
.num.orange { color: #f0a020; }
.num.red { color: #e05a5a; }
.num.blue { color: #2080f0; }

.label {
  color: #888;
  font-size: 12px;
  margin-top: 4px;
}

.table-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 20px;
}

.table-title {
  font-weight: 600;
  margin-bottom: 14px;
}

.stat-table {
  width: 100%;
  border-collapse: collapse;
}

.stat-table th,
.stat-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f1f3;
  text-align: left;
  font-size: 14px;
}

.stat-table th {
  color: #888;
  font-weight: 500;
  background: #fafbfc;
}

.rate-bar {
  display: inline-block;
  width: 120px;
  height: 8px;
  background: #f0f1f3;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 8px;
  vertical-align: middle;
}

.rate-fill {
  display: block;
  height: 100%;
  background: #18a058;
  border-radius: 4px;
}

.empty {
  text-align: center;
  color: #999;
}
</style>
