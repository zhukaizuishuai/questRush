import { get, post, put } from './request'
import type { PageResult, QuestionListVO, QuestionPracticeVO } from '@/types'

export interface QuestionListParams {
  categoryId?: number
  type?: number
  difficulty?: number
  isVip?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

/** 题目列表（按权限过滤） */
export function listQuestion(params: QuestionListParams) {
  return get<PageResult<QuestionListVO>>('/question/list', params)
}

/** 题目搜索 */
export function searchQuestion(params: QuestionListParams) {
  return get<PageResult<QuestionListVO>>('/question/search', params)
}

/** 答题前详情（不含答案） */
export function questionDetail(id: number) {
  return get<QuestionPracticeVO>('/question/detail', { id })
}

export default { listQuestion, searchQuestion, questionDetail }
