import { get, post } from './request'
import type {
  PageResult,
  PracticeSessionDTO,
  PracticeSessionVO,
  PracticeStatsVO,
  QuestionPracticeVO,
  QuestionSubmitVO,
  WrongBookVO
} from '@/types'

/** 创建刷题会话 */
export function createSession(data: PracticeSessionDTO) {
  return post<PracticeSessionVO>('/practice/session', data)
}

/** 按 index 取题 */
export function nextQuestion(sessionId: string, index: number) {
  return get<QuestionPracticeVO>('/practice/next', { sessionId, index })
}

/** 提交答题，返回判分结果与解析 */
export function submitAnswer(data: { questionId: number; answer: string }) {
  return post<QuestionSubmitVO>('/practice/submit', data)
}

/** 错题本（越权条目脱敏：title 为 null） */
export function wrongList(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<WrongBookVO>>('/practice/wrong/list', params)
}

/** 复习队列（next_review_time <= now） */
export function reviewList(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<WrongBookVO>>('/practice/review/list', params)
}

/** 练习统计 */
export function stats() {
  return get<PracticeStatsVO>('/practice/stats')
}

export default { createSession, nextQuestion, submitAnswer, wrongList, reviewList, stats }
