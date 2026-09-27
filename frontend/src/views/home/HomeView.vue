<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NSpin } from 'naive-ui'
import { getCategoryTree } from '@/api/category'
import { reviewList, stats } from '@/api/practice'
import { useUserStore } from '@/stores/user'
import type { CategoryVO, PracticeStatsVO, WrongBookVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const categories = ref<CategoryVO[]>([])
const statsData = ref<PracticeStatsVO | null>(null)
const reviewItems = ref<WrongBookVO[]>([])

/** 分类图标：浅色底 + 品牌化彩色图标底 */
const colorPalette = [
  { bg: '#E7F8F1', fg: '#0B8A62' },
  { bg: '#E0EBFF', fg: '#4E6BE8' },
  { bg: '#FFF0E3', fg: '#E8792B' },
  { bg: '#FDE9EE', fg: '#E14D6E' },
  { bg: '#E0F5F4', fg: '#0E9494' },
  { bg: '#FFF7DC', fg: '#B98A00' }
]

onMounted(async () => {
  try {
    const tree = await getCategoryTree()
    categories.value = tree ?? []
    if (userStore.isLoggedIn) {
      const [s, r] = await Promise.all([
        stats().catch(() => null),
        reviewList().catch(() => null)
      ])
      statsData.value = s
      reviewItems.value = r?.list ?? []
    }
  } finally {
    loading.value = false
  }
})

const categoryColor = (id: number) => colorPalette[Math.abs(id) % colorPalette.length]

const correctRateText = computed(() => {
  if (!statsData.value) return '-'
  return `${statsData.value.accuracy.toFixed(1)}%`
})

/** 问候语按时间段变化 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，注意休息'
  if (h < 12) return '上午好，开始今天的冲刺'
  if (h < 18) return '下午好，准备开始今天的冲刺'
  return '晚上好，来一组睡前练习'
})

function goCategory(c: CategoryVO) {
  // 一级分类仅用于分组，点击进入其下叶子分类的题目列表
  if (c.children && c.children.length > 0) {
    router.push({ path: '/question', query: { categoryId: c.children[0].id } })
  } else {
    router.push({ path: '/question', query: { categoryId: c.id } })
  }
}
</script>

<template>
  <div class="page-container">
    <n-spin :show="loading">
      <!-- 欢迎横幅 -->
      <section class="hero">
        <div class="hero-copy">
          <h2>{{ greeting }}</h2>
          <p>选择一个方向开始今天的练习，坚持每日打卡，面试稳操胜券。</p>
          <div class="hero-actions">
            <button class="hero-btn light" @click="router.push('/practice')">开始刷题</button>
            <button v-if="userStore.isLoggedIn" class="hero-btn ghost" @click="router.push('/review')">
              今日复习计划
            </button>
            <button v-else class="hero-btn ghost" @click="router.push('/login')">登录 / 注册</button>
          </div>
        </div>
        <!-- 连续打卡玻璃卡 -->
        <div v-if="userStore.isLoggedIn && statsData" class="streak-card">
          <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
            <path
              d="M14 2C15 7 20 9 20 15C20 20 17.5 24 14 24C10.5 24 8 20 8 15C8 12 9 10 10.5 8C11 10.5 12 11.5 13 12C13 9 12.5 5 14 2Z"
              fill="#FFD37A"
            />
          </svg>
          <div class="streak-row stat-num">
            {{ statsData.continuousCheckInDays }}<span class="streak-unit">天</span>
          </div>
          <div class="streak-label">连续打卡纪录</div>
        </div>
      </section>

      <!-- 统计卡片 -->
      <section v-if="userStore.isLoggedIn && statsData" class="stat-cards">
        <div class="stat-card">
          <div class="stat-num stat-value">{{ statsData.totalCount }}</div>
          <div class="stat-label">累计答题</div>
        </div>
        <div class="stat-card">
          <div class="stat-num stat-value brand">{{ correctRateText }}</div>
          <div class="stat-label">总作答正确率</div>
        </div>
        <div class="stat-card">
          <div class="stat-num stat-value">{{ statsData.continuousCheckInDays }}<span class="stat-unit">天</span></div>
          <div class="stat-label">连续打卡</div>
        </div>
        <div class="stat-card gold" @click="router.push('/review')">
          <div class="stat-num stat-value">{{ reviewItems.length }}</div>
          <div class="stat-label">{{ reviewItems.length > 0 ? '今日待复习 · 点击进入' : '今日无待复习' }}</div>
        </div>
      </section>

      <!-- 分类入口 -->
      <section class="section">
        <div class="section-head">
          <h3 class="section-title">题库分类</h3>
          <a class="section-link" @click="router.push('/category')">查看全部</a>
        </div>
        <div class="category-grid">
          <div
            v-for="c in categories"
            :key="c.id"
            class="category-card"
            @click="goCategory(c)"
          >
            <div
              class="cat-icon"
              :style="{ background: categoryColor(c.id).bg, color: categoryColor(c.id).fg }"
            >{{ c.name.slice(0, 1) }}</div>
            <div class="cat-info">
              <div class="cat-name">{{ c.name }}</div>
              <div class="cat-sub">{{ c.children?.length ? `${c.children.length} 个子分类` : '开始练习' }}</div>
            </div>
          </div>
        </div>
      </section>
    </n-spin>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--grad-brand);
  color: #fff;
  border-radius: var(--radius-xl);
  padding: 40px 44px;
  margin-bottom: 24px;
}

/* 柔光装饰 */
.hero::before,
.hero::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.hero::before {
  width: 340px;
  height: 340px;
  left: 42%;
  top: -160px;
  background: rgba(224, 235, 255, 0.5);
  filter: blur(90px);
}

.hero::after {
  width: 280px;
  height: 280px;
  right: -60px;
  bottom: -140px;
  background: rgba(255, 247, 220, 0.45);
  filter: blur(80px);
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero h2 {
  margin: 0 0 8px;
  font-size: 26px;
  letter-spacing: 0.01em;
}

.hero p {
  margin: 0 0 20px;
  opacity: 0.92;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-btn {
  border: none;
  cursor: pointer;
  height: 44px;
  padding: 0 26px;
  border-radius: var(--radius-pill);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  transition: all 0.15s;
}

.hero-btn.light {
  background: #fff;
  color: var(--brand-700);
  box-shadow: 0 6px 16px rgba(0, 51, 38, 0.18);
}

.hero-btn.light:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 51, 38, 0.24);
}

.hero-btn.ghost {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.45);
  font-weight: 500;
}

.hero-btn.ghost:hover {
  background: rgba(255, 255, 255, 0.26);
}

/* 打卡玻璃卡 */
.streak-card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 200px;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(24px);
  flex-shrink: 0;
}

.streak-row {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}

.streak-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
}

.streak-label {
  font-size: 12px;
  opacity: 0.85;
}

/* 统计卡 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--border-1);
  box-shadow: var(--shadow-card);
  transition: all 0.15s;
}

.stat-card.gold {
  cursor: pointer;
  background: #fffbf0;
  border-color: var(--gold-border);
}

.stat-card.gold .stat-value {
  color: var(--gold-text);
}

.stat-card.gold .stat-label {
  color: var(--gold-text);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  line-height: 1.2;
}

.stat-value.brand {
  color: var(--brand-700);
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
  color: var(--ink-600);
}

.stat-label {
  color: var(--ink-600);
  font-size: 13px;
  margin-top: 4px;
}

/* 分类区 */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 18px;
  margin: 0;
}

.section-link {
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 1px solid var(--border-1);
  border-radius: var(--radius-md);
  padding: 18px;
  cursor: pointer;
  transition: all 0.15s;
}

.category-card:hover {
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
  border-color: var(--border-2);
}

.cat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cat-name {
  font-weight: 600;
  color: var(--ink-900);
}

.cat-sub {
  color: var(--ink-400);
  font-size: 12px;
  margin-top: 2px;
}
</style>
