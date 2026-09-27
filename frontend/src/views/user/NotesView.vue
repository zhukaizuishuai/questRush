<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NInput, NModal, NPagination, NSpin, useMessage } from 'naive-ui'
import { noteList, saveNote } from '@/api/note'
import { formatTime } from '@/utils/format'
import type { NoteVO } from '@/types'

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const list = ref<NoteVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showEdit = ref(false)
const editing = ref<NoteVO | null>(null)
const editContent = ref('')
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await noteList({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = page.list
    total.value = page.total
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onPageChange(p: number) {
  pageNum.value = p
  load()
}

function openEdit(note: NoteVO) {
  editing.value = note
  editContent.value = note.content
  showEdit.value = true
}

async function saveEdit() {
  if (!editing.value) return
  if (!editContent.value.trim()) {
    message.warning('笔记内容不能为空')
    return
  }
  saving.value = true
  try {
    await saveNote({ questionId: editing.value.questionId, content: editContent.value })
    message.success('笔记已保存')
    showEdit.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function goQuestion(id: number) {
  router.push(`/question/${id}`)
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">我的笔记</h2>
    <p class="sub-title">笔记仅本人可见，支持在题目详情页编辑保存。</p>
    <n-spin :show="loading">
      <div v-if="list.length" class="note-list">
        <div v-for="note in list" :key="note.id ?? note.questionId" class="note-card">
          <div class="head">
            <a class="q-title" @click="goQuestion(note.questionId)">{{ note.questionTitle || `题目 #${note.questionId}` }}</a>
            <span class="time">{{ formatTime(note.updateTime) }}</span>
          </div>
          <div class="content">{{ note.content }}</div>
          <div class="foot">
            <n-button size="tiny" quaternary type="primary" @click="openEdit(note)">编辑</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else-if="!loading" description="还没有笔记，去题目详情页写一条吧" style="padding: 80px 0" />
      <div v-if="total > pageSize" class="pager">
        <n-pagination
          :page="pageNum"
          :page-size="pageSize"
          :item-count="total"
          @update:page="onPageChange"
        />
      </div>
    </n-spin>

    <n-modal v-model:show="showEdit" preset="card" title="编辑笔记" style="width: 640px">
      <n-input v-model:value="editContent" type="textarea" rows="8" placeholder="支持 Markdown" />
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px">
          <n-button @click="showEdit = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="saveEdit">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.sub-title {
  color: #888;
  font-size: 13px;
  margin: -8px 0 16px;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 16px 20px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.q-title {
  font-weight: 600;
  color: #18a058;
  cursor: pointer;
  font-size: 14px;
}

.time {
  color: #bbb;
  font-size: 12px;
}

.content {
  font-size: 13px;
  color: #555;
  white-space: pre-wrap;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
