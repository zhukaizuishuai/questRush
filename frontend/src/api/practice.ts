import { get, post } from './request'
import type {
  PracticeSessionDTO,
  PracticeSessionVO,
  PracticeStatsVO,
  QuestionPracticeVO,
  QuestionSubmitVO,
  WrongBookPage
} from '@/types'

/** 创建刷题会话 */
export function createSession(data: PracticeSessionDTO) {
  return post<PracticeSessionVO>('/practice/session', data)
}

/**
 * 按 index 取题。
 * silent：这道接口的 404 / 40301 是「这道题不可用，换下一道」，
 * 全局弹窗会在连续跳题时刷屏，也把 VIP 过期误报成整页会员拦截。
 */
export function nextQuestion(sessionId: string, index: number) {
  return get<QuestionPracticeVO>('/practice/next', { sessionId, index }, { silent: true })
}

/** 提交答题，返回判分结果与解析 */
export function submitAnswer(data: { questionId: number; answer: string }) {
  return post<QuestionSubmitVO>('/practice/submit', data)
}

/** 错题本（越权条目脱敏：title 为 null；answerableCount 为全队列可作答数） */
export function wrongList(params?: { pageNum?: number; pageSize?: number }) {
  return get<WrongBookPage>('/practice/wrong/list', params)
}

/** 复习队列（next_review_time <= now） */
export function reviewList(params?: { pageNum?: number; pageSize?: number }) {
  return get<WrongBookPage>('/practice/review/list', params)
}

/** 练习统计 */
export function stats() {
  return get<PracticeStatsVO>('/practice/stats')
}
