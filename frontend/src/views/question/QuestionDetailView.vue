<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NInput, NModal, NSpin, NTag, useMessage } from 'naive-ui'
import { questionDetail } from '@/api/question'
import { toggleFavorite } from '@/api/favorite'
import { noteDetail, saveNote } from '@/api/note'
import MarkdownRender from '@/components/MarkdownRender.vue'
import OptionGroup from '@/components/OptionGroup.vue'
import LikeButton from '@/components/LikeButton.vue'
import VipBadge from '@/components/VipBadge.vue'
import { difficultyLabel, difficultyTagType, typeLabel } from '@/utils/format'
import type { QuestionPracticeVO } from '@/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(true)
const question = ref<QuestionPracticeVO | null>(null)

// 笔记弹窗
const showNoteModal = ref(false)
const noteContent = ref('')
const noteSaving = ref(false)

// 收藏状态
const favorited = ref(false)
const favPending = ref(false)

async function load() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    question.value = await questionDetail(id)
    favorited.value = question.value?.favorited ?? false
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onToggleFavorite() {
  if (!question.value || favPending.value) return
  favPending.value = true
  try {
    const res = await toggleFavorite(question.value.id)
    favorited.value = res.favorited
    message.success(res.favorited ? '已收藏' : '已取消收藏')
  } finally {
    favPending.value = false
  }
}

async function openNote() {
  if (!question.value) return
  showNoteModal.value = true
  try {
    const note = await noteDetail(question.value.id)
    noteContent.value = note?.content ?? ''
  } catch {
    noteContent.value = ''
  }
}

async function saveNoteAction() {
  if (!question.value) return
  if (!noteContent.value.trim()) {
    message.warning('笔记内容不能为空')
    return
  }
  noteSaving.value = true
  try {
    await saveNote({ questionId: question.value.id, content: noteContent.value })
    message.success('笔记已保存')
    showNoteModal.value = false
  } finally {
    noteSaving.value = false
  }
}

function goPractice() {
  if (!question.value) return
  router.push({ path: '/practice', query: { categoryId: question.value.categoryId } })
}
</script>

<template>
  <div class="page-container">
    <n-spin :show="loading">
      <div v-if="question" class="detail-card">
        <div class="header">
          <n-tag size="small" :bordered="false" type="info">{{ typeLabel(question.type) }}</n-tag>
          <n-tag size="small" :bordered="false" :type="difficultyTagType(question.difficulty)">
            {{ difficultyLabel(question.difficulty) }}
          </n-tag>
          <span class="category">{{ question.categoryName }}</span>
          <VipBadge :is-vip="question.isVip" />
        </div>

        <MarkdownRender class="title" :content="question.title" />

        <!-- 选项只读展示（答题前不含答案） -->
        <OptionGroup
          v-if="question.type !== 4"
          :type="question.type"
          :options="question.options"
          model-value=""
          disabled
        />

        <div class="hint">
          📌 刷题模式下提交作答后才可查看正确答案与解析
        </div>

        <div class="actions">
          <n-button type="primary" @click="goPractice">去刷题</n-button>
          <n-button :type="favorited ? 'warning' : 'default'" secondary :loading="favPending" @click="onToggleFavorite">
            {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
          </n-button>
          <n-button secondary @click="openNote">📝 笔记</n-button>
          <LikeButton :question-id="question.id" :liked="question.liked" :like-count="question.likeCount" />
        </div>
      </div>
      <div v-else-if="!loading" class="empty-tip">题目不存在或已下架</div>
    </n-spin>

    <n-modal v-model:show="showNoteModal" preset="card" title="我的笔记（仅本人可见）" style="width: 640px">
      <n-input
        v-model:value="noteContent"
        type="textarea"
        rows="8"
        placeholder="记录你的思考与总结，支持 Markdown"
      />
      <template #footer>
        <div class="modal-footer">
          <n-button @click="showNoteModal = false">取消</n-button>
          <n-button type="primary" :loading="noteSaving" @click="saveNoteAction">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.detail-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 28px;
}

.header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.category {
  color: #888;
  font-size: 13px;
}

.title {
  font-size: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f1f3;
}

.hint {
  margin-top: 20px;
  color: #999;
  font-size: 13px;
}

.actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 80px 0;
}
</style>
