<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NInputNumber, NRadioButton, NRadioGroup, NSelect, NSpin, useMessage } from 'naive-ui'
import { createSession, nextQuestion, submitAnswer } from '@/api/practice'
import { getCategoryTree } from '@/api/category'
import MarkdownRender from '@/components/MarkdownRender.vue'
import OptionGroup from '@/components/OptionGroup.vue'
import { DIFFICULTY_OPTIONS } from '@/utils/format'
import type {
  CategoryVO,
  PracticeSessionVO,
  QuestionPracticeVO,
  QuestionSubmitVO
} from '@/types'

const route = useRoute()
const message = useMessage()

// ============ 会话创建表单 ============
const loading = ref(true)
const tree = ref<CategoryVO[]>([])
const leafOptions = ref<{ label: string; value: number }[]>([])
const mode = ref<'order' | 'random'>('order')
const categoryId = ref<number | null>(null)
const count = ref(10)
const difficulty = ref<number | null>(null)
const creating = ref(false)

// ============ 会话进行中 ============
const session = ref<PracticeSessionVO | null>(null)
const index = ref(0)
const question = ref<QuestionPracticeVO | null>(null)
const questionLoading = ref(false)
const answer = ref('')
const submitting = ref(false)
const result = ref<QuestionSubmitVO | null>(null)

const isObjective = computed(() => question.value?.type === 1 || question.value?.type === 2 || question.value?.type === 3)
const canSubmit = computed(() => !result.value && (isObjective.value ? !!answer.value : answer.value.trim().length > 0))

function flatten(treeNodes: CategoryVO[], parentName?: string) {
  const out: { label: string; value: number }[] = []
  for (const node of treeNodes) {
    const label = parentName ? `${parentName} / ${node.name}` : node.name
    if (node.children?.length) {
      out.push(...flatten(node.children, label))
    } else {
      out.push({ label, value: node.id })
    }
  }
  return out
}

onMounted(async () => {
  try {
    tree.value = (await getCategoryTree()) ?? []
    leafOptions.value = flatten(tree.value)
    const cid = route.query.categoryId
    if (cid) categoryId.value = Number(cid)
  } finally {
    loading.value = false
  }
})

async function startSession() {
  creating.value = true
  try {
    session.value = await createSession({
      categoryId: categoryId.value,
      mode: mode.value,
      count: count.value,
      difficulty: difficulty.value as 1 | 2 | 3 | null
    })
    index.value = 0
    await loadQuestion()
  } finally {
    creating.value = false
  }
}

async function loadQuestion() {
  if (!session.value) return
  questionLoading.value = true
  result.value = null
  answer.value = ''
  try {
    question.value = await nextQuestion(session.value.sessionId, index.value)
  } finally {
    questionLoading.value = false
  }
}

/** 多选题 answer 已在 OptionGroup 内归一化为排序字母串，直接提交 */
async function onSubmit() {
  if (!session.value || !question.value || !canSubmit.value) return
  submitting.value = true
  try {
    result.value = await submitAnswer({
      questionId: question.value.id,
      answer: answer.value
    })
  } finally {
    submitting.value = false
  }
}

async function goNext() {
  if (index.value < session.value!.total - 1) {
    index.value += 1
    await loadQuestion()
  }
}

async function goPrev() {
  if (index.value > 0) {
    index.value -= 1
    await loadQuestion()
  }
}

function restart() {
  session.value = null
  question.value = null
  result.value = null
  answer.value = ''
}

function resultClass() {
  if (result.value?.isCorrect === 1) return 'correct'
  if (result.value?.isCorrect === 0) return 'wrong'
  return 'neutral'
}
</script>

<template>
  <div class="page-container practice-page">
    <!-- 会话创建表单 -->
    <div v-if="!session" class="setup-card">
      <h2 class="page-title">开始刷题</h2>
      <n-spin :show="loading">
        <div class="form-item">
          <div class="form-label">刷题分类</div>
          <n-select v-model:value="categoryId" :options="leafOptions" placeholder="全部分类" clearable />
        </div>
        <div class="form-item">
          <div class="form-label">刷题模式</div>
          <n-radio-group v-model:value="mode">
            <n-radio-button value="order">顺序刷题</n-radio-button>
            <n-radio-button value="random">随机刷题</n-radio-button>
          </n-radio-group>
        </div>
        <div class="form-item">
          <div class="form-label">题目数量</div>
          <n-input-number v-model:value="count" :min="1" :max="100" />
        </div>
        <div class="form-item">
          <div class="form-label">难度筛选</div>
          <n-select v-model:value="difficulty" :options="DIFFICULTY_OPTIONS" placeholder="全部难度" clearable style="width: 200px" />
        </div>
        <n-button type="primary" size="large" :loading="creating" @click="startSession">创建刷题会话</n-button>
      </n-spin>
    </div>

    <!-- 刷题进行中 -->
    <div v-else class="practice-card">
      <div class="progress-bar">
        <span class="progress-text">第 {{ index + 1 }} / {{ session.total }} 题</span>
        <n-button quaternary size="small" @click="restart">退出会话</n-button>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: `${((index + 1) / session.total) * 100}%` }"></div>
      </div>

      <n-spin :show="questionLoading">
        <div v-if="question" class="question-area">
          <MarkdownRender class="q-title" :content="question.title" />

          <OptionGroup v-model="answer" :type="question.type" :options="question.options" :disabled="!!result" />

          <!-- 提交前 UI 不出现任何答案；提交后展示判分 + 解析 -->
          <div v-if="result" class="result-panel" :class="resultClass()">
            <div class="result-head">
              <template v-if="result.isCorrect === 1">✅ 回答正确</template>
              <template v-else-if="result.isCorrect === 0">❌ 回答错误</template>
              <template v-else>📝 已提交，简答题不自动判分</template>
            </div>
            <div v-if="result.isCorrect === 0" class="result-line">
              你的作答：<b>{{ answer || '（空）' }}</b>
            </div>
            <div v-if="result.answer" class="result-line">
              正确答案：<b class="answer-text">{{ result.answer }}</b>
            </div>
            <div v-if="result.mastered != null" class="result-line mastery">
              {{ result.mastered === 1 ? '🎯 本题已掌握，移出复习队列' : '📌 已加入复习队列，按遗忘曲线安排复习' }}
              <template v-if="result.nextReviewTime">（下次复习：{{ result.nextReviewTime }}）</template>
            </div>
            <div v-if="result.analysis" class="analysis">
              <div class="analysis-title">题目解析</div>
              <MarkdownRender :content="result.analysis" />
            </div>
            <div v-if="question.type === 4 && result.answerText" class="analysis">
              <div class="analysis-title">参考答案</div>
              <MarkdownRender :content="result.answerText" />
            </div>
          </div>

          <div class="op-row">
            <n-button :disabled="index === 0" @click="goPrev">上一题</n-button>
            <template v-if="!result">
              <n-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="onSubmit">提交答案</n-button>
            </template>
            <template v-else>
              <n-button v-if="index < session.total - 1" type="primary" @click="goNext">下一题</n-button>
              <n-button v-else type="primary" @click="restart">完成练习，重新开始</n-button>
            </template>
          </div>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<style scoped>
.setup-card,
.practice-card {
  background: #fff;
  border: 1px solid var(--border-1);
  border-radius: var(--radius-lg);
  padding: 28px;
  max-width: 860px;
  margin: 0 auto;
  box-shadow: var(--shadow-card);
}

.form-item {
  margin-bottom: 18px;
  max-width: 420px;
}

.form-label {
  margin-bottom: 8px;
  color: var(--ink-600);
  font-size: 14px;
}

.progress-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.progress-text {
  color: var(--ink-600);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}

.progress-track {
  height: 8px;
  background: #e7eaee;
  border-radius: 4px;
  margin-bottom: 26px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--grad-brand);
  border-radius: 4px;
  transition: width 0.3s;
}

.q-title {
  font-size: 16px;
  margin-bottom: 20px;
}

.result-panel {
  margin-top: 22px;
  border-radius: var(--radius-md);
  padding: 18px 20px;
}

.result-panel.correct {
  background: #f2fbf7;
  border: 1px solid #bbe8d4;
}

.result-panel.wrong {
  background: var(--rose-bg);
  border: 1px solid var(--rose-border);
}

.result-panel.neutral {
  background: var(--bg-soft);
  border: 1px solid var(--border-1);
}

.result-head {
  font-weight: 700;
  margin-bottom: 10px;
}

.result-panel.correct .result-head {
  color: var(--brand-700);
}

.result-panel.wrong .result-head {
  color: var(--rose-500);
}

.result-line {
  font-size: 14px;
  margin-bottom: 6px;
}

.answer-text {
  color: var(--brand-700);
}

.mastery {
  color: var(--gold-text);
  font-size: 13px;
}

.analysis {
  margin-top: 12px;
  padding: 14px 16px;
  border-top: 1px dashed var(--border-2);
  background: #fff;
  border-radius: var(--radius-sm);
}

.result-panel.correct .analysis {
  border-top: none;
}

.analysis-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--ink-900);
}

.op-row {
  margin-top: 26px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
