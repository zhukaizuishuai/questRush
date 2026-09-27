import { del, download, get, post, put } from './request'
import type {
  AdminStatsVO,
  AdminUserVO,
  CategoryVO,
  ExcelImportResultVO,
  PageResult,
  QuestionAdminVO
} from '@/types'

// ============ 用户管理 ============

export function userList(params: { username?: string; pageNum?: number; pageSize?: number }) {
  return get<PageResult<AdminUserVO>>('/admin/user/list', params)
}

/** 禁用 / 启用（后端会强制登出） */
export function updateUserStatus(id: number, status: number) {
  return put<null>('/admin/user/status', { id, status })
}

/** 手动设置 VIP 过期时间 */
export function setUserVip(id: number, vipExpireTime: string | null) {
  return put<null>('/admin/user/vip', { id, vipExpireTime })
}

// ============ 分类管理 ============

export function adminCategoryList() {
  return get<CategoryVO[]>('/admin/category/list')
}

export function createCategory(data: { name: string; parentId: number; sort: number }) {
  return post<null>('/admin/category', data)
}

export function updateCategory(data: { id: number; name: string; parentId: number; sort: number }) {
  return put<null>('/admin/category', data)
}

/** 删除分类（后端校验子分类与启用题目） */
export function deleteCategory(id: number) {
  return del<null>('/admin/category', { id })
}

// ============ 题目管理 ============

export interface AdminQuestionParams {
  categoryId?: number
  type?: number
  difficulty?: number
  isVip?: number
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export function adminQuestionPage(params: AdminQuestionParams) {
  return get<PageResult<QuestionAdminVO>>('/admin/question/list', params)
}

export function adminQuestionDetail(id: number) {
  return get<QuestionAdminVO>('/admin/question/detail', { id })
}

export interface QuestionSaveDTO {
  id?: number
  categoryId: number
  type: number
  difficulty: number
  title: string
  answer: string
  answerText: string
  analysis: string
  isVip: number
  status: number
  options: { optionCode: string; optionContent: string; sort: number }[]
}

export function createQuestion(data: QuestionSaveDTO) {
  return post<null>('/admin/question', data)
}

export function updateQuestion(data: QuestionSaveDTO) {
  return put<null>('/admin/question', data)
}

export function deleteQuestion(id: number) {
  return del<null>('/admin/question', { id })
}

/** 上下架 */
export function updateQuestionStatus(id: number, status: number) {
  return put<null>('/admin/question/status', { id, status })
}

/** 按点赞表重算 like_count（数据兜底） */
export function recalcLike() {
  return post<null>('/admin/question/recalc-like')
}

// ============ Excel 导入导出 ============

/** 下载导入模板 */
export function downloadTemplate() {
  return download('/admin/excel/template', undefined, '题库导入模板.xlsx')
}

export type DuplicateStrategy = 'SKIP' | 'OVERWRITE' | 'ERROR'

/** 批量导入（返回成功 / 跳过 / 失败明细） */
export function importExcel(file: File, duplicateStrategy: DuplicateStrategy) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('duplicateStrategy', duplicateStrategy)
  return post<ExcelImportResultVO>('/admin/excel/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 按条件批量导出 */
export function exportExcel(params: AdminQuestionParams) {
  return download('/admin/excel/export', params, `题库导出_${Date.now()}.xlsx`)
}

// ============ 统计 ============

export function adminStats() {
  return get<AdminStatsVO>('/admin/stats')
}
