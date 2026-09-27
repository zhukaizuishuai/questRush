/** 统一返回体 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页统一返回 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 登录用户信息（GET /api/user/info） */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string | null
  role: 'admin' | 'user'
  vipExpireTime: string | null
  status: number
  createTime: string
}

/** 登录响应 */
export interface LoginVO {
  token: string
  user: UserInfo
}

/** 图形验证码 */
export interface CaptchaVO {
  captchaId: string
  /** base64 图片（无 data: 前缀） */
  image: string
}

/** 分类树节点 */
export interface CategoryVO {
  id: number
  name: string
  parentId: number
  sort: number
  questionCount: number
  createTime: string
  children: CategoryVO[]
}

/** 题型：1 单选 2 多选 3 判断 4 简答 */
export type QuestionType = 1 | 2 | 3 | 4
/** 难度：1 简单 2 中等 3 困难 */
export type QuestionDifficulty = 1 | 2 | 3

/** 题目列表项（列表 / 搜索，不含答案） */
export interface QuestionListVO {
  id: number
  categoryId: number
  categoryName: string
  type: QuestionType
  difficulty: QuestionDifficulty
  isVip: number
  likeCount: number
  status: number
  /** 题干（Markdown 原文） */
  title: string
  createTime: string
}

/** 题目选项（仅 code 与 content，不含正确性标记） */
export interface QuestionOptionVO {
  optionCode: string
  optionContent: string
}

/** 答题前详情 / 刷题页题目（不含答案与解析） */
export interface QuestionPracticeVO {
  id: number
  categoryId: number
  categoryName: string
  type: QuestionType
  difficulty: QuestionDifficulty
  isVip: number
  title: string
  options: QuestionOptionVO[] | null
  favorited: boolean
  liked?: boolean
  likeCount?: number
}

/** 主动查看的答案与解析（GET /api/question/answer） */
export interface QuestionAnswerVO {
  answer: string
  answerText: string
  analysis: string
}

/** 提交答题后返回（含正确答案与解析） */
export interface QuestionSubmitVO {
  questionId: number
  /** 1 正确 0 错误 null 简答未判分 */
  isCorrect: number | null
  /** 正确答案（客观题） */
  answer: string
  /** 简答题参考答案（Markdown） */
  answerText: string
  /** 解析（Markdown） */
  analysis: string
  /** 本次提交后是否已掌握（1 已掌握） */
  mastered: number | null
  /** 下次复习时间（null = 不在复习队列） */
  nextReviewTime: string | null
}

/** 刷题会话创建参数 */
export interface PracticeSessionDTO {
  categoryId: number | null
  mode: 'order' | 'random'
  count: number
  difficulty: QuestionDifficulty | null
}

/** 刷题会话 */
export interface PracticeSessionVO {
  sessionId: string
  total: number
}

/** 错题本 / 复习队列条目（越权时脱敏：title 为 null） */
export interface WrongBookVO {
  questionId: number
  type: QuestionType
  categoryId: number
  categoryName: string
  /** VIP 过期等越权场景为 null，前端显示「该题目需会员权限」 */
  title: string | null
  lastAnswer: string
  totalCount: number
  wrongCount: number
  submitTime: string
  nextReviewTime: string | null
}

/** 统计 */
export interface PracticeStatsVO {
  totalCount: number
  correctCount: number
  /** 正确率（0-100 百分比数） */
  accuracy: number
  categoryStats: CategoryStatVO[]
  continuousCheckInDays: number
  wrongBookCount: number
  reviewCount: number
}

export interface CategoryStatVO {
  categoryId: number
  categoryName: string
  total: number
  correct: number
}

/** 收藏列表条目（越权时脱敏：title 为 null） */
export interface FavoriteVO {
  questionId: number
  categoryId: number
  categoryName: string
  type: QuestionType
  difficulty: QuestionDifficulty
  isVip: number
  title: string | null
  favoritedTime: string
}

/** 笔记条目（detail 场景 id / questionTitle / createTime 可能为 null） */
export interface NoteVO {
  id: number | null
  questionId: number
  questionTitle: string | null
  content: string
  createTime: string | null
  updateTime: string
}

/** VIP 套餐 */
export interface VipPlanVO {
  planType: 'month' | 'quarter' | 'year'
  name: string
  months: number
  price: number
  description?: string
}

/** VIP 订单 */
export interface VipOrderVO {
  id: number
  orderNo: string
  planType: string
  months: number
  amount: number
  payChannel: string
  status: 0 | 1 | 2
  payTime: string | null
  createTime: string
}

/** 管理员 - 用户 */
export interface AdminUserVO {
  id: number
  username: string
  nickname: string
  email: string | null
  role: string
  status: number
  vipExpireTime: string | null
  createTime: string
}

/** 管理员 - 题目（含答案，仅后台） */
export interface QuestionAdminVO {
  id: number
  categoryId: number
  categoryName: string
  type: QuestionType
  difficulty: QuestionDifficulty
  title: string
  answer: string
  answerText: string
  analysis: string
  isVip: number
  status: number
  likeCount: number
  options: QuestionOptionVO[] | null
  createTime: string
}

/** Excel 导入结果 */
export interface ExcelImportResultVO {
  successCount: number
  skippedCount: number
  failCount: number
  failures: ExcelImportFailure[]
}

export interface ExcelImportFailure {
  row: number
  reason: string
}

/** 管理员统计 */
export interface AdminStatsVO {
  userCount: number
  questionCount: number
  vipUserCount: number
  /** 日均答题量 */
  dailyAvgAnswers: number
}
