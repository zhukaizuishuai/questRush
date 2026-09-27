<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NSpin } from 'naive-ui'
import { getCategoryTree } from '@/api/category'
import type { CategoryVO } from '@/types'

const router = useRouter()
const loading = ref(true)
const tree = ref<CategoryVO[]>([])

onMounted(async () => {
  try {
    tree.value = (await getCategoryTree()) ?? []
  } finally {
    loading.value = false
  }
})

function goQuestion(c: CategoryVO) {
  router.push({ path: '/question', query: { categoryId: c.id } })
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">分类浏览</h2>
    <n-spin :show="loading">
      <div v-for="root in tree" :key="root.id" class="group">
        <div class="group-name">{{ root.name }}</div>
        <div class="leaf-list">
          <template v-if="root.children?.length">
            <div
              v-for="leaf in root.children"
              :key="leaf.id"
              class="leaf"
              @click="goQuestion(leaf)"
            >
              {{ leaf.name }}
              <span class="go">→</span>
            </div>
          </template>
          <div v-else class="leaf empty" @click="goQuestion(root)">{{ root.name }} →</div>
        </div>
      </div>
      <div v-if="!loading && tree.length === 0" class="empty-tip">暂无分类数据</div>
    </n-spin>
  </div>
</template>

<style scoped>
.group {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 16px;
}

.group-name {
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 12px;
  color: #18a058;
}

.leaf-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.leaf {
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.leaf:hover {
  background: #f0faf4;
  color: #18a058;
}

.leaf.empty {
  color: #18a058;
}

.go {
  opacity: 0.6;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 60px 0;
}
</style>
