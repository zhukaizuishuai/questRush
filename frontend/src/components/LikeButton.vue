<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toggleLike } from '@/api/like'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  questionId: number
  liked?: boolean
  likeCount?: number
}>()

const emit = defineEmits<{
  (e: 'update', liked: boolean, likeCount: number): void
}>()

const router = useRouter()
const userStore = useUserStore()

const localLiked = ref(props.liked ?? false)
const localCount = ref(props.likeCount ?? 0)
/** 请求进行中置灰，防连点 */
const pending = ref(false)

// props 是异步到达的（详情页先挂载组件再拉到数据），必须同步，
// 否则按钮初始态恒为「未点赞 / 0」，与真实数据不符。
watch(
  () => [props.liked, props.likeCount] as const,
  ([liked, count]) => {
    localLiked.value = liked ?? false
    localCount.value = count ?? 0
  }
)

async function onClick() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (pending.value) return
  pending.value = true
  // 乐观更新
  localLiked.value = !localLiked.value
  localCount.value += localLiked.value ? 1 : -1
  try {
    const res = await toggleLike(props.questionId)
    localLiked.value = res.liked
    localCount.value = res.likeCount
    emit('update', res.liked, res.likeCount)
  } catch {
    // 失败回滚
    localLiked.value = props.liked ?? false
    localCount.value = props.likeCount ?? 0
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <span
    class="like-button"
    :class="{ liked: localLiked, pending, disabled: pending }"
    :title="localLiked ? '取消点赞' : '点赞'"
    @click.stop="onClick"
  >
    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
      <path
        d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
      />
    </svg>
    <span class="count">{{ localCount }}</span>
  </span>
</template>

<style scoped>
.like-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #999;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  transition: color 0.15s, transform 0.1s;
}

.like-button:hover {
  color: #e05a5a;
}

.like-button.liked {
  color: #e05a5a;
}

.like-button.pending {
  opacity: 0.5;
  pointer-events: none;
}
</style>
