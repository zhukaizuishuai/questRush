<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NInputNumber, NRadioButton, NRadioGroup, NSelect, NSpin } from 'naive-ui'
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
const router = useRouter()

/**
 * 会话来源（来自 URL query）：
 * all    常规刷题，显示配置表单
 * review 复习模式，从 /review?mode=review 进入，进入即自动开局，不显示配置表单
 * wrong  错题重做模式，从 /wrong?mode=wrong 进入，同上
 * 复习 / 错题模式复用本页的作答 → 判分 → 解析 UI，不再单独造一套。
 */
const source = computed<'all' | 'review' | 'wrong'>(() =>
  route.query.mode === 'review' || route.query.mode === 'wrong' ? route.query.mode : 'all'
)
const isQueueMode = computed(() => source.value !== 'all')

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
/** 本轮已作答题数，用于完成页展示 */
const answered = ref(0)
/** 本轮因下架 / 无权限而跳过的题数。同时作为已跳过下标集合的变更信号。 */
const skipped = ref(0)
/**
 * 已经确认不可用的会话下标。goPrev 用它向后跳过，不再退到这些题上再被向前循环弹回。
 * 普通 Set 不触发渲染，变更时同步递增 skipped。
 */
const skippedIndexes = new Set<number>()
/** 加载失败态：阻断作答，避免把上一题再提交一遍 */
const loadFailed = ref(false)
/**
 * network    传输失败，重试同一题
 * expired    会话过期（422）。重试 next 必然再 422，队列模式要重新开局，常规模式回配置页
 * forbidden  会话不属于当前用户（403）。这是整场会话的错误，跳题没有意义
 * skip-limit 连续不可用题目达到上限，停下来让用户决定是否继续，而不是结束整场
 */
const loadFailKind = ref<'network' | 'expired' | 'forbidden' | 'skip-limit'>('network')

const isObjective = computed(() => question.value?.type === 1 || question.value?.type === 2 || question.value?.type === 3)
const canSubmit = computed(() => !result.value && (isObjective.value ? !!answer.value : answer.value.trim().length > 0))
/** 是否处于最后一题且已提交（决定按钮是「完成」还是「下一题」） */
const isLastQuestion = computed(() => !!session.value && index.value >= session.value.total - 1)
/** 前面还有没被跳过的题。「上一题」只在这时可点。 */
const canGoPrev = computed(() => {
  void skipped.value
  return nearestPrev(index.value) != null
})
/** 退出会话后回到哪：复习/错题模式回到对应列表页 */
const exitPath = computed(() => (source.value === 'review' ? '/review' : source.value === 'wrong' ? '/wrong' : '/practice'))

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

/**
 * 代际号。review ↔ wrong 快速对切时，先发出的 createSession 可能后返回。
 * 若用 booting 把后一次开局直接丢掉，旧会话会留在新模式里。
 * 每次开局或复位都加一，过期的请求结果直接丢弃。
 */
let sessionEpoch = 0

/**
 * 复习 / 错题模式：进入页面直接开局。
 * count 不传 → 后端按队列全长返回。
 * 随机模式对复习无意义（队列已按到期时间排序），这里固定用 order。
 */
async function startQueueSession() {
  const epoch = ++sessionEpoch
  creating.value = true
  loadFailed.value = false
  try {
    const created = await createSession({
      source: source.value,
      categoryId: null,
      mode: 'order'
    })
    if (epoch !== sessionEpoch) return
    session.value = created
    index.value = 0
    answered.value = 0
    resetSkipped()
    await loadQuestion(epoch)
  } catch {
    if (epoch !== sessionEpoch) return
    // 后端已通过 message 提示（如「当前没有到期待复习的题目」），退回列表页
    router.replace(exitPath.value)
  } finally {
    if (epoch === sessionEpoch) creating.value = false
  }
}

onMounted(async () => {
  if (isQueueMode.value) {
    loading.value = false
    await startQueueSession()
    return
  }
  try {
    tree.value = (await getCategoryTree()) ?? []
    leafOptions.value = flatten(tree.value)
    const cid = route.query.categoryId
    if (cid) categoryId.value = Number(cid)
  } finally {
    loading.value = false
  }
})

/**
 * watch source（而非 isQueueMode）：review ↔ wrong 两个 query 互切时 isQueueMode 保持 true，
 * 只监听它会导致旧会话留在新模式里（题源、返回路径、文案全不对）。
 * 进入 / 切换队列模式 → 重新开局；从队列模式切回常规刷题 → 复位到配置表单。
 */
watch(source, async (val, oldVal) => {
  if (val === oldVal) return
  if (val !== 'all') {
    // 先丢掉旧会话（含常规刷题进行中的那场），再按新来源开局
    restart()
    await startQueueSession()
  } else {
    restart()
    // 从队列模式切回常规刷题时补加载分类下拉（onMounted 已提前 return 过）
    if (!tree.value.length) {
      tree.value = (await getCategoryTree()) ?? []
      leafOptions.value = flatten(tree.value)
    }
  }
})

async function startSession() {
  const epoch = ++sessionEpoch
  creating.value = true
  try {
    const created = await createSession({
      source: 'all',
      categoryId: categoryId.value,
      mode: mode.value,
      count: count.value,
      difficulty: difficulty.value as 1 | 2 | 3 | null
    })
    if (epoch !== sessionEpoch) return
    session.value = created
    index.value = 0
    answered.value = 0
    resetSkipped()
    await loadQuestion(epoch)
  } finally {
    if (epoch === sessionEpoch) creating.value = false
  }
}

/**
 * 连续自动跳过的上限。用循环而不是递归，到顶后停在失败态等用户点「继续」，
 * 不把后面还能做的题整段结束掉。
 */
const MAX_SKIP_BATCH = 20

interface QuestionSnapshot {
  index: number
  question: QuestionPracticeVO | null
  result: QuestionSubmitVO | null
  answer: string
}

function errorCode(e: unknown): number | undefined {
  return (e as { result?: { code?: number } })?.result?.code
}

function resetSkipped() {
  skippedIndexes.clear()
  skipped.value = 0
}

/** 同一道题只计一次，避免回退时再次 404 把「已跳过 N 道」越加越高 */
function markSkipped(i: number) {
  if (skippedIndexes.has(i)) return
  skippedIndexes.add(i)
  skipped.value += 1
}

/** 从 from 往更小的下标找最近一道还没跳过的题。没有则返回 null。 */
function nearestPrev(from: number): number | null {
  for (let i = from - 1; i >= 0; i--) {
    if (!skippedIndexes.has(i)) return i
  }
  return null
}

function restoreSnapshot(snapshot: QuestionSnapshot) {
  index.value = snapshot.index
  question.value = snapshot.question
  result.value = snapshot.result
  answer.value = snapshot.answer
  loadFailed.value = false
}

async function loadQuestion(
  epoch = sessionEpoch,
  direction: 'forward' | 'backward' = 'forward',
  snapshot?: QuestionSnapshot
) {
  const current = session.value
  if (!current || epoch !== sessionEpoch) return
  questionLoading.value = true
  result.value = null
  answer.value = ''
  loadFailed.value = false
  let skippedThisBatch = 0
  try {
    while (epoch === sessionEpoch && session.value === current && index.value >= 0 && index.value < current.total) {
      try {
        const loaded = await nextQuestion(current.sessionId, index.value)
        if (epoch !== sessionEpoch || session.value !== current) return
        question.value = loaded
        return
      } catch (e) {
        if (epoch !== sessionEpoch || session.value !== current) return
        const code = errorCode(e)
        // 401 由拦截器清登录态并跳转，这里不再铺失败页
        if (code === 401) return
        // 404 下架/删除；40301 才是 VIP 过期。403 是「会话不属于你」，整场无效。
        if (code === 404 || code === 40301) {
          question.value = null
          markSkipped(index.value)
          if (direction === 'backward') {
            // 向后走。不能交给向前的 index += 1，否则会弹回刚离开的那一题。
            const prev = nearestPrev(index.value)
            if (prev == null) {
              if (snapshot) restoreSnapshot(snapshot)
              return
            }
            index.value = prev
            continue
          }
          skippedThisBatch += 1
          if (index.value >= current.total - 1) {
            finishSession()
            return
          }
          index.value += 1
          if (skippedThisBatch >= MAX_SKIP_BATCH) {
            question.value = null
            loadFailKind.value = 'skip-limit'
            loadFailed.value = true
            return
          }
          continue
        }
        question.value = null
        // 回退途中的网络错误留在原题，避免把已提交的判分清掉后再交一次
        if (direction === 'backward' && snapshot && code !== 422 && code !== 403) {
          restoreSnapshot(snapshot)
          return
        }
        loadFailed.value = true
        if (code === 422) loadFailKind.value = 'expired'
        else if (code === 403) loadFailKind.value = 'forbidden'
        else loadFailKind.value = 'network'
        return
      }
    }
    if (direction === 'backward' && snapshot && epoch === sessionEpoch && session.value === current) {
      restoreSnapshot(snapshot)
      return
    }
    if (epoch === sessionEpoch && session.value === current && !question.value && !loadFailed.value) {
      finishSession()
    }
  } finally {
    if (epoch === sessionEpoch) questionLoading.value = false
  }
}

/** 会话已过期：队列模式按当前来源重新开局；常规模式回到配置页（随机模式重建会换题） */
async function recoverExpired() {
  const queue = isQueueMode.value
  restart()
  if (queue) await startQueueSession()
}

/** 包一层，避免点击事件被当成 loadQuestion 的代际参数 */
function retryLoad() {
  void loadQuestion()
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
    answered.value += 1
  } finally {
    submitting.value = false
  }
}

async function goNext() {
  if (session.value && index.value < session.value.total - 1) {
    index.value += 1
    await loadQuestion()
  }
}

async function goPrev() {
  const current = session.value
  if (!current) return
  const target = nearestPrev(index.value)
  if (target == null) return
  const snapshot: QuestionSnapshot = {
    index: index.value,
    question: question.value,
    result: result.value,
    answer: answer.value
  }
  index.value = target
  await loadQuestion(sessionEpoch, 'backward', snapshot)
}

/** 结束本轮：队列模式回列表页并刷新（队列内容已变化），常规模式回配置页 */
function finishSession() {
  const queue = isQueueMode.value
  const path = exitPath.value
  restart()
  if (queue) {
    router.push({ path, query: { refreshed: Date.now() } })
  }
}

function restart() {
  sessionEpoch += 1
  session.value = null
  question.value = null
  result.value = null
  answer.value = ''
  loadFailed.value = false
  resetSkipped()
  questionLoading.value = false
  creating.value = false
}

function resultClass() {
  if (result.value?.isCorrect === 1) return 'correct'
  if (result.value?.isCorrect === 0) return 'wrong'
  return 'neutral'
}

const modeTitle = computed(() =>
  source.value === 'review' ? '复习模式' : source.value === 'wrong' ? '错题重做' : '开始刷题'
)

/**
 * 最后一题的收尾按钮文案。常规刷题（source=all）finishSession 只是回到配置页，
 * 不能显示「返回复习队列」这类队列模式文案。
 */
const finishText = computed(() => {
  if (source.value === 'review') return '完成复习，返回复习队列'
  if (source.value === 'wrong') return '完成重做，返回错题本'
  return '完成练习，重新开始'
})
</script>

<template>
  <div class="page-container practice-page">
    <!-- 会话创建表单（仅常规刷题模式；复习/错题模式进入即开局） -->
    <div v-if="!session && !isQueueMode" class="setup-card">
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

    <!-- 复习/错题模式：开局中（会话创建失败会自动退回列表页，这里只是短暂过渡） -->
    <div v-else-if="!session" class="setup-card">
      <n-spin :show="creating" description="正在准备题目..." />
    </div>

    <!-- 刷题进行中 -->
    <div v-else class="practice-card" :class="{ 'queue-mode': isQueueMode }">
      <div class="progress-bar">
        <span class="progress-text">
          <template v-if="isQueueMode">{{ modeTitle }} · 第 {{ index + 1 }} / {{ session.total }} 题</template>
          <template v-else>第 {{ index + 1 }} / {{ session.total }} 题</template>
        </span>
        <n-button quaternary size="small" @click="finishSession">
          {{ isQueueMode ? '结束并返回' : '退出会话' }}
        </n-button>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: `${((index + 1) / session.total) * 100}%` }"></div>
      </div>
      <div v-if="isQueueMode" class="mode-tip">
        <template v-if="source === 'review'">
          本轮共 {{ session.total }} 道到期题目，按遗忘曲线「越早到期越先复习」排序。答对后下次复习间隔顺延（1 / 2 / 4 / 7 / 15 天），连续答对 3 次即判定掌握并移出队列。
        </template>
        <template v-else>本轮共 {{ session.total }} 道错题，按最近作答时间从新到旧重做。</template>
      </div>

      <n-spin :show="questionLoading">
        <!-- 加载失败态：按错误种类给出口。过期会话再点 next 只会再 422。 -->
        <div v-if="loadFailed" class="load-failed">
          <p v-if="loadFailKind === 'expired'">会话已过期，请重新开始。常规刷题会回到配置页。</p>
          <p v-else-if="loadFailKind === 'forbidden'">无权访问这个刷题会话。</p>
          <p v-else-if="loadFailKind === 'skip-limit'">
            已连续跳过 {{ MAX_SKIP_BATCH }} 道下架或无权限的题目，后面的题还在。
          </p>
          <p v-else>题目加载失败，请检查网络后重试。</p>
          <div class="op-row">
            <n-button v-if="loadFailKind === 'expired' && isQueueMode" type="primary" @click="recoverExpired">
              重新开始
            </n-button>
            <n-button v-else-if="loadFailKind === 'expired'" type="primary" @click="finishSession">返回配置</n-button>
            <n-button v-else-if="loadFailKind === 'skip-limit'" type="primary" @click="retryLoad">继续</n-button>
            <n-button v-else-if="loadFailKind === 'network'" type="primary" @click="retryLoad">重试</n-button>
            <n-button v-if="!(loadFailKind === 'expired' && !isQueueMode)" @click="finishSession">
              {{ isQueueMode ? '结束并返回' : '退出会话' }}
            </n-button>
          </div>
        </div>
        <div v-else-if="question" class="question-area">
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
            <!-- 仅客观题展示掌握状态：简答题不入复习队列（isCorrect 为 null），显示会误导 -->
            <div v-if="result.isCorrect != null" class="result-line mastery">
              <template v-if="result.mastered === 1">🎯 本题已掌握，移出复习队列</template>
              <template v-else-if="result.nextReviewTime">📌 已按遗忘曲线安排，下次复习：{{ result.nextReviewTime }}</template>
              <template v-else>📌 已加入复习队列</template>
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
            <n-button :disabled="!canGoPrev || questionLoading" @click="goPrev">上一题</n-button>
            <template v-if="!result">
              <n-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="onSubmit">提交答案</n-button>
            </template>
            <template v-else>
              <n-button v-if="!isLastQuestion" type="primary" @click="goNext">下一题</n-button>
              <n-button v-else type="primary" @click="finishSession">{{ finishText }}</n-button>
            </template>
          </div>
          <div v-if="isQueueMode && answered > 0" class="answered-tip">
            本轮已作答 {{ answered }} / {{ session.total }} 题
          </div>
          <div v-if="skipped > 0" class="answered-tip">已跳过 {{ skipped }} 道下架或无权限的题目</div>
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

/* 复习 / 错题模式：顶部说明当前轮次来源与排序规则 */
.mode-tip {
  background: var(--brand-50);
  border: 1px solid var(--border-2);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--brand-700);
  margin-bottom: 22px;
}

.answered-tip {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: var(--ink-400);
  font-variant-numeric: tabular-nums;
}

.load-failed {
  text-align: center;
  color: var(--ink-600);
  padding: 48px 0;
}

.load-failed p {
  margin: 0;
  font-size: 14px;
}
</style>
